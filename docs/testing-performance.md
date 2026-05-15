---
description: Performance testing conventions — JMeter plan structure, Gatling Scala DSL, k6 JS, profiling workflow, tuning targets, results table
globs: testing/performance/**
alwaysApply: false
---

# Performance Testing Conventions

## Primary Load Test Target

`GET /api/v1/ward/{wardId}/overview` is the **primary target** across all three tools.
It joins 4 tables + window function for latest vitals per EHR + problem count aggregation.
It will expose N+1 risks, missing indexes, and thread pool saturation at lower concurrency
than any other endpoint. This is by design — it mirrors COSMIC ward view query patterns.

## Load Profile (identical across all three tools)

- Ramp: 0 → 50 users over 30 seconds
- Steady state: 50 users for 3 minutes
- Ramp down: 50 → 0 over 30 seconds
- Thresholds: p95 < 500ms, error rate < 1%

Three samplers per run:
1. `GET /api/v1/ward/ICU/overview` (primary — 4-table join)
2. `GET /api/v1/ehr/subject/${patientId}` (EHR lookup)
3. `POST /api/v1/ehr/${ehrId}/compositions/${cid}/vitals` (write path + RabbitMQ publish)

Use seed patient/EHR IDs from `V5__seed_patients.sql` — 20 patients, realistic volume.

## JMeter (`testing/performance/jmeter/cosmolab-baseline.jmx`)

- Thread Group: 50 users, ramp 30s, duration 3 minutes
- CSV Data Set Config: `patient-ids.csv` with columns `patientId,ehrId,compositionId`
- Response assertions: HTTP 200, response time < 500ms
- Listeners: Aggregate Report, Response Time Graph
- Run headless:
  ```bash
  jmeter -n -t cosmolab-baseline.jmx -l results.jtl -e -o report/
  ```

## Gatling (`testing/performance/gatling/`)

Maven plugin — run with `mvn gatling:test`.

```scala
// src/test/scala/cosmolab/CosmoLabSimulation.scala
class CosmoLabSimulation extends Simulation {

  val patientFeeder = csv("patient-ids.csv").circular

  val scn = scenario("CosmoLab Ward Workflow")
    .feed(patientFeeder)
    .exec(http("Ward Overview")
      .get("/api/v1/ward/ICU/overview")
      .check(status.is(200)))
    .pause(1)
    .exec(http("EHR Lookup")
      .get("/api/v1/ehr/subject/#{patientId}")
      .check(status.is(200)))
    .pause(1)
    .exec(http("Record Vitals")
      .post("/api/v1/ehr/#{ehrId}/compositions/#{compositionId}/vitals")
      .body(ElFileBody("vitals-payload.json")).asJson
      .check(status.is(201)))

  setUp(
    scn.inject(
      rampUsers(50).during(30.seconds),
      constantUsersPerSec(20).during(180.seconds)
    )
  ).assertions(
    global.responseTime.percentile(95).lt(500),
    global.failedRequests.percent.lt(1)
  )
}
```

## k6 (`testing/performance/k6/patient-load.js`)

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

const patients = new SharedArray('patients', () => JSON.parse(open('./patient-ids.json')));

export const options = {
  stages: [
    { duration: '30s', target: 50 },
    { duration: '3m',  target: 50 },
    { duration: '30s', target: 0  },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed:   ['rate<0.01'],
  },
};

export default function () {
  const p = patients[Math.floor(Math.random() * patients.length)];
  const headers = { Authorization: 'Bearer mock-token' };

  check(http.get(`/api/v1/ward/ICU/overview`, { headers }), { 'status 200': r => r.status === 200 });
  sleep(1);
  check(http.get(`/api/v1/ehr/subject/${p.patientId}`, { headers }), { 'status 200': r => r.status === 200 });
  sleep(1);
  check(http.post(`/api/v1/ehr/${p.ehrId}/compositions/${p.compositionId}/vitals`,
    JSON.stringify({ heartRate: 72, systolicBP: 120, diastolicBP: 80 }),
    { headers: { ...headers, 'Content-Type': 'application/json' } }
  ), { 'status 201': r => r.status === 201 });
}
```

Run with Prometheus remote write:
```bash
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write k6 run patient-load.js
```

## Tool Comparison

| Dimension | JMeter | Gatling | k6 |
|---|---|---|---|
| Script format | XML (GUI) | Scala DSL | JavaScript |
| Report | HTML (plugins) | Built-in HTML | CLI + Grafana |
| CI integration | Maven plugin | Maven plugin | CLI |
| Resource usage | High (JVM, thread-per-user) | Medium (async Akka) | Low (Go coroutines) |
| Best for | Broad protocol testing | Performance regression in CI | High concurrency; Grafana-native |

## Profiling Workflow (JMX port 9999 required)

**Use async-profiler for fast iteration; JFR for final evidence.**

1. Warm-up: Gatling at 25 users/s for 60s (allows JIT to compile hot paths)
2. Full load: Gatling at 50 users/s for 3 minutes

**async-profiler** (fast, interactive HTML flame graph):
```bash
docker exec cosmolab-backend ./profiler.sh -e cpu -d 120 -f /tmp/flame.html 1
docker cp cosmolab-backend:/tmp/flame.html ./profiling/
# Open flame.html in browser — wide flat bars = hot methods
```

**JFR** (richer: GC, allocations, lock contention, not just CPU):
```bash
docker exec cosmolab-backend jcmd 1 JFR.start duration=120s filename=/tmp/cosmolab.jfr settings=profile
docker cp cosmolab-backend:/tmp/cosmolab.jfr ./profiling/
# Open in JMC → Method Profiling → sort by self-time
```

Expected hotspots (confirm from actual flame graph):
- `WardOverviewService` — N+1 or full-table scan
- `ObjectMapper.writeValueAsBytes` — Jackson serialisation of nested WardOverview payload
- `NioWorker` / `WorkerThread` blocked — Undertow thread pool saturation

## Tuning Targets (apply in order; re-run Gatling between each)

| # | Target | Change | Expected impact |
|---|---|---|---|
| 1 | Undertow thread pool | `task-max-threads` 32 → 200 in `standalone.xml` | Immediate throughput increase |
| 2 | WardOverview N+1 | Rewrite with `JOIN FETCH` or native SQL window query | Dramatic query count reduction |
| 3 | JPA batch loading | `@BatchSize(size=25)` on collection associations | Reduced query count on nested fetches |
| 4 | G1GC tuning | `-XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=16m` | Reduced GC pause spikes |

## Results Table (fill in after Sprint 7)

| Metric | JMeter baseline | Gatling before | Gatling after | k6 |
|---|---|---|---|---|
| Throughput (req/s) | | | | |
| p50 latency (ms) | | | | |
| p95 latency (ms) | | | | |
| p99 latency (ms) | | | | |
| Error rate | | | | |

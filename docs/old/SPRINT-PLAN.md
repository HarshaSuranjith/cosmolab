# CosmoLab — Sprint Plan

> Implementation plan for the full CosmoLab build cycle.
> Each sprint is one focused day of work with a hard exit criterion.
> No sprint is considered done until its exit criterion is met.
>
> Parent document: [CLAUDE.md](../CLAUDE.md)
> Clinical domain context: [FEATURES.md](FEATURES.md)

---

## Overview

| Sprint | Focus | Exit criterion |
|---|---|---|
| 1 | Infrastructure + domain model | `docker-compose up` healthy; Flyway applied; `/actuator/health` UP |
| 2 | Backend API + RabbitMQ + unit tests | All endpoints correct; events in RabbitMQ UI; MockMvc tests pass |
| 3 | Angular frontend | All pages navigable; data from live backend; no console errors |
| 4 | Containerisation + Playwright E2E | Full stack in containers; Playwright suite green |
| 5 | Jenkins pipeline | All stages green; Playwright report published as artefact |
| 6 | Observability + JMeter baseline | Grafana live under load; JMeter HTML report with p95 numbers |
| 7 | Gatling + k6 + performance optimisation | Before/after benchmark with profiling evidence |

---

## Sprint 1 — Infrastructure + Domain Model

**Goal**: working Docker Compose stack; Spring Boot WAR starts; connects to MSSQL; schema migrated.

### Tasks

**Infrastructure**
- `devops/docker-compose.yml`: all services, healthchecks, named `cosmolab-net` network
  - MSSQL healthcheck: TCP probe on port 1433 (15-25s startup; must use `condition: service_healthy`)
  - RabbitMQ healthcheck: `rabbitmq-diagnostics -q ping`
  - Backend `depends_on`: both sqlserver and rabbitmq healthy
- `devops/docker-compose.dev.yml`: host source mounts, JPDA debug port 5005, shorter healthcheck intervals

**Backend bootstrap**
- `pom.xml`: Spring Boot 3.2 WAR packaging
  - deps: `mssql-jdbc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-amqp`, `spring-boot-starter-web`, `spring-boot-starter-actuator`, `flyway-sqlserver`, `micrometer-registry-prometheus`, `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`, `loki-logback-appender`, `lombok`, `spring-boot-starter-validation`
  - `spring-boot-starter-tomcat` scope `provided` (WildFly provides Undertow)
- `Application.java`
- `CosmoLabServletInitializer.java` extends `SpringBootServletInitializer`
- `webapp/WEB-INF/jboss-deployment-structure.xml` — excludes WildFly's bundled Jackson and Hibernate
- `application.yml`: datasource, RabbitMQ connection, Flyway, Actuator endpoints, OTLP exporter

**Domain entities** (aligned with FEATURES.md §3)
- `Patient.java` — demographics aggregate
- `EhrRecord.java` — `@OneToOne` Patient
- `Composition.java` — `@ManyToOne` EhrRecord; `CompositionType` + `CompositionStatus` enums
- `VitalSigns.java` — `@ManyToOne` Composition; all measurement fields nullable
- `ProblemListEntry.java` — `@ManyToOne` Composition and EhrRecord (direct FK for efficient queries)
- Domain repository interfaces (pure ports — no Spring annotations)
- Spring Data JPA implementations in `infrastructure/persistence/`

**Flyway migrations**
- `V1__patients_and_ehr.sql` — `patients`, `ehr_records` + indexes
- `V2__compositions.sql` — `compositions`
- `V3__vital_signs.sql` — `vital_signs`
- `V4__problem_list.sql` — `problem_list_entries`
- `V5__seed_patients.sql` — 20 patients + EHR records across ICU, Cardiology, Neurology
- `V6__seed_clinical_data.sql` — compositions + vitals + problems per patient (realistic load test data)

### Exit criterion
`mvn clean install` succeeds. All containers healthy. Flyway log shows V1-V6 applied. `GET /actuator/health` returns `{"status":"UP"}`.

---

## Sprint 2 — Backend API + RabbitMQ Events

**Goal**: full REST API working; clinical events flowing through RabbitMQ; unit tests passing.

### Tasks

**Services**
- `EhrService.java`: createEhr, getEhr, getEhrBySubject
- `CompositionService.java`: createComposition, amendComposition, listCompositions (Pageable + type filter)
- `VitalSignsService.java`: recordVitals, getLatestVitals, getVitalHistory (date range)
- `ProblemListService.java`: addProblem, updateProblemStatus, listProblems (status filter)
- `WardOverviewService.java`: aggregation — joins Patient + EhrRecord + VitalSigns (latest per EHR via window function) + ProblemListEntry count

**Controllers + error handling**
- `EhrController`, `CompositionController`, `VitalSignsController`, `ProblemListController`, `WardOverviewController`
- `GlobalExceptionHandler.java` (`@RestControllerAdvice`): all exceptions mapped to RFC 7807 `ProblemDetail`
  - Not found → 404, validation → 400 + field error map, duplicate → 409, unhandled → 500

**DTOs**
- Request/response DTOs per resource; `PagedResponse<T>` generic wrapper
- `WardOverviewResponse` — nested patient + latestVitals + `flags[]` + activeProblemCount

**RabbitMQ**
- `RabbitMQConfig.java`: `clinical.events` topic exchange, `audit.log` direct exchange, `audit-queue` quorum queue, binding
- `ClinicalEvent.java` record: `eventType`, `aggregateId`, `occurredAt`, `payload`
- `ClinicalEventPublisher.java`: async send to `clinical.events`; publisher-confirm sync send to `audit.log`
- `AuditEventConsumer.java`: `@RabbitListener` manual ack; idempotent on `(aggregateId, occurredAt)`

**Tests**
- `EhrControllerTest`, `VitalSignsControllerTest`, `ProblemListControllerTest`, `WardOverviewControllerTest`, `CompositionControllerTest`
- Coverage: happy path, not found, validation failure, pagination

### Exit criterion
All MockMvc tests pass. All endpoints return correct paginated JSON. RabbitMQ Management UI (`:15672`) shows messages on `clinical.events` after any write. `WardOverview` returns populated response for ICU ward.

---

## Sprint 3 — Angular Frontend

**Goal**: all pages built against live backend; openEHR domain reflected in the UI.

### Tasks

**Module scaffold**
- `AppModule`, `AppRoutingModule` (lazy load all feature modules)
- `CoreModule` — singleton, imported once
- `SharedModule` — imported by every feature module

**Core**
- `ApiInterceptor`: injects mock JWT header; maps 4xx/5xx to `NotificationService`
- `AuthGuard`: `sessionStorage.getItem('token')` check
- `AuthService`: `login()` sets hardcoded token
- `NotificationService`: wraps `MatSnackBar`

**Shared components**
- `PageHeaderComponent`, `LoadingSpinnerComponent` (`MatProgressBar`)
- `SeverityBadgeComponent` — colour chip (MILD/MODERATE/SEVERE, ROUTINE/URGENT/CRITICAL)
- `VitalSignsChartComponent` — sparkline per measurement; cells amber/red outside normal range

**Feature modules**

`WardModule` (`/ward/:wardId`):
- `WardOverviewComponent` — dense patient table; vital sign cells colour-coded from `flags[]`; row click → patient detail

`PatientsModule` (`/patients`, `/patients/:id`):
- `PatientListComponent` — Material table; 300ms debounce search; ward select; status filter chips; paginator
- `PatientDetailComponent` — header (name + personnummer); `mat-tab-group`:
  - **Overview**: EHR summary + latest vitals snapshot
  - **Vitals**: history table + sparklines using `VitalSignsChartComponent`
  - **Problems**: active problem list with ICD-10 codes + severity badges; add problem inline form
  - **Compositions**: composition list + type filter; expand to detail

`AdminModule` (`/admin`):
- Links to Grafana (:3000), Actuator, Prometheus (:9090), RabbitMQ UI (:15672)
- Spring Boot version from `/actuator/info`

**Angular services**: `EhrService`, `CompositionService`, `VitalSignsService`, `ProblemListService`, `WardOverviewService`

### Exit criterion
Full navigation flow. Ward overview loads with colour-coded vitals. All 4 patient detail tabs populated. Adding a vitals record updates the sparkline. Adding a problem appears in the list. No 404s on direct URL entry. No console errors.

---

## Sprint 4 — Containerisation + Playwright E2E

**Goal**: full stack in containers; Playwright E2E suite covering all critical paths; green in CI mode.

### Containerisation

`cosmolab-backend/Dockerfile` (multi-stage):
- Stage 1: `maven:3.9-eclipse-temurin-17-alpine` — `mvn clean package -DskipTests`
- Stage 2: `quay.io/wildfly/wildfly:30.0.Final` — copies WAR to `standalone/deployments/`; sets `JAVA_OPTS`

`cosmolab-frontend/Dockerfile` (multi-stage):
- Stage 1: `node:20-alpine` — `npm ci && ng build --configuration production`
- Stage 2: `nginx:1.25-alpine` — serves `dist/`; copies `nginx.conf`

`cosmolab-frontend/nginx.conf`:
```nginx
location /api { proxy_pass http://cosmolab-backend:8080; }
location / { try_files $uri $uri/ /index.html; }
```

### Playwright

`testing/e2e/playwright.config.ts`: baseURL `http://localhost:80`; Chromium + Firefox; screenshot + video on failure; HTML reporter.

Page objects (`testing/e2e/pages/`): `WardOverviewPage`, `PatientListPage`, `PatientDetailPage`, `CreateVitalsPage`, `AddProblemPage`

Test files (`testing/e2e/tests/`):

| File | Key scenarios |
|---|---|
| `ward-overview.spec.ts` | Table renders; colour-coded cells; row click navigates |
| `patient-list.spec.ts` | Search; ward filter; pagination; row click |
| `patient-detail.spec.ts` | All 4 tabs load; vitals sparklines; ICD-10 codes visible |
| `create-vitals.spec.ts` | Form submits; new reading appears; abnormal value flagged |
| `add-problem.spec.ts` | Form submits; appears in list; empty ICD-10 shows validation error |
| `navigation.spec.ts` | Direct URL entry; invalid EHR ID shows error; nav active state |

### Exit criterion
`docker-compose up --build` starts cleanly. `npx playwright test` exits 0 on Chromium and Firefox. HTML report generated with screenshots on any failure.

---

## Sprint 5 — Jenkins Pipeline

**Goal**: declarative 7-stage pipeline runs green; Playwright report published as Jenkins artefact.

### Tasks

`devops/jenkins/Dockerfile`: Jenkins LTS JDK 17 + Docker CLI + Maven 3.9 + Node 20 + Playwright system libs.

`devops/jenkins/Jenkinsfile` — 7 stages:

| Stage | Command |
|---|---|
| Checkout | `git checkout` |
| Backend unit tests | `mvn test` |
| Backend build | `mvn clean package -DskipTests` |
| Frontend build | `npm ci && ng build --configuration production` |
| Docker build | `docker build` for backend + frontend images tagged `${BUILD_NUMBER}` |
| E2E tests | `docker-compose up -d` → wait-for-health → `npx playwright test` → `docker-compose down` |
| Deploy | `docker-compose up -d` |

Post (always): `docker-compose down`; archive JUnit XML; publish Playwright HTML report.

Docker socket: `/var/run/docker.sock` bind mount (no DinD).

### Exit criterion
All 7 stages green. JUnit test trend visible in Jenkins. Playwright report accessible from build page. Clean on re-trigger.

---

## Sprint 6 — Observability + JMeter Baseline

**Goal**: LGTM stack provisioned; Grafana dashboards live under load; JMeter baseline numbers recorded.

### Observability

`devops/observability/prometheus.yml`:
- Scrape `cosmolab-backend:8080/actuator/prometheus` every 15s
- Scrape `rabbitmq:15692/metrics` every 15s (RabbitMQ Prometheus plugin — independent of app metrics)

`devops/observability/loki/loki-config.yml`: filesystem storage; 168h retention.

`devops/docker-compose.yml` update: Loki log driver on backend + frontend services.

`devops/observability/grafana/datasources/datasources.yml`: Prometheus + Loki + Tempo auto-provisioned.

**Dashboards**:

`cosmolab-jvm.json` — heap used/committed/max; GC pause p99; GC count rate; live threads; CPU %

`cosmolab-http.json` — request rate by endpoint; error rate; p50/p95/p99 latency; WardOverview highlighted

`cosmolab-rabbitmq.json` — publish rate to `clinical.events`; `audit-queue` depth; consumer ack rate; unacked count

### JMeter

`testing/performance/jmeter/cosmolab-baseline.jmx`:
- 50 users, 30s ramp, 3 minute steady state
- CSV Data Set for patient/EHR IDs from seed data
- Sampler A: `GET /api/v1/ward/ICU/overview` (primary — 4-table join with window function)
- Sampler B: `GET /api/v1/ehr/subject/${patientId}`
- Sampler C: `POST /api/v1/ehr/${ehrId}/compositions/${cid}/vitals`
- Assertions: HTTP 200, response time < 500ms
- Run headless: `jmeter -n -t cosmolab-baseline.jmx -l results.jtl -e -o report/`

### Exit criterion
Grafana shows live HTTP + JVM metrics during JMeter run. Loki shows backend log stream. JMeter HTML report generated with p50/p95/p99 and throughput. RabbitMQ dashboard shows publish rate during write-path test.

---

## Sprint 7 — Gatling + k6 + Performance Optimisation

**Goal**: same scenario in all three tools; profiling evidence; measurable improvement documented.

### Gatling

`testing/performance/gatling/pom.xml`: Gatling Maven plugin (`mvn gatling:test`).

`CosmoLabSimulation.scala`:
```scala
val scn = scenario("CosmoLab Ward Workflow")
  .exec(http("Ward Overview").get("/api/v1/ward/ICU/overview").check(status.is(200)))
  .pause(1)
  .exec(http("EHR Lookup").get("/api/v1/ehr/subject/${patientId}").check(status.is(200)))
  .pause(1)
  .exec(http("Record Vitals").post("/api/v1/ehr/${ehrId}/compositions/${cid}/vitals")
    .body(ElFileBody("vitals-payload.json")).asJson.check(status.is(201)))

setUp(scn.inject(
  rampUsers(50).during(30.seconds),
  constantUsersPerSec(20).during(180.seconds)
)).assertions(
  global.responseTime.percentile(95).lt(500),
  global.failedRequests.percent.lt(1)
)
```

### k6

`testing/performance/k6/patient-load.js`:
```javascript
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
// K6_PROMETHEUS_RW_SERVER_URL wires output into Grafana alongside app metrics
```

### Profiling workflow

JMX on port 9999 enables both tools. Use async-profiler for fast iteration; JFR for final evidence.

1. Warm-up: Gatling at 25 users/s for 60s (JIT compilation)
2. Full load: Gatling at 50 users/s for 3 minutes
3. **async-profiler** (fast, HTML flame graph):
   `docker exec cosmolab-backend ./profiler.sh -e cpu -d 120 -f /tmp/flame.html 1`
4. **JFR** (rich — GC, allocations, lock contention):
   `docker exec cosmolab-backend jcmd 1 JFR.start duration=120s filename=/tmp/cosmolab.jfr settings=profile`
5. `docker cp cosmolab-backend:/tmp/flame.html ./profiling/`
6. Open `flame.html` in browser → identify top self-time methods → apply tuning → re-run Gatling

### Tuning targets (apply in order; isolate each change)

| Target | Change | Expected impact |
|---|---|---|
| Undertow thread pool | `task-max-threads` 32 → 200 in `standalone.xml` | Immediate throughput increase under concurrent load |
| WardOverview N+1 | Rewrite with `JOIN FETCH` or native query | Dramatic reduction in SQL round-trips |
| JPA batch loading | `@BatchSize(size=25)` on collection associations | Reduced query count on composition + vitals fetch |
| G1GC tuning | `-XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=16m` | Reduced GC pause spikes visible in JFR |

### Results table (fill in after Sprint 7)

| Metric | JMeter baseline | Gatling before tuning | Gatling after tuning | k6 |
|---|---|---|---|---|
| Throughput (req/s) | | | | |
| p50 latency (ms) | | | | |
| p95 latency (ms) | | | | |
| p99 latency (ms) | | | | |
| Error rate | | | | |

### Exit criterion
Gatling before/after shows measurable p95 improvement. At least one tuning change applied and documented with flame graph evidence. k6 remote write visible in Grafana. Results table populated.

---

## Deferred items

| Item | Blocked on | Target |
|---|---|---|
| Mock JWT → real auth | Decision 2 deferred | Post-sprint 7 |
| WildFly 2-node cluster (`standalone-ha.xml`) | Sprint 4 complete | Optional sprint 8 |
| CareInstruction (INSTRUCTION archetype) | Sprint 2 complete | Optional sprint 8 |
| FHIR R4 API layer | Full openEHR feature set | Phase 2 |

---

*Last updated: Sprint 0 — plan only. Fill results table after Sprint 7. Mark exit criteria as complete.*

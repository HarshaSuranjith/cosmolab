# CosmoLab

A minimal clinical information system that implements a minimal version of OpenEHR as a standard.
Built as a full-cycle portfolio project covering backend engineering, containerisation,
observability, and data-driven performance optimisation.

---

## Quick Start

```bash
git clone git@github.com:HarshaSuranjith/cosmolab.git
cd cosmolab
docker-compose -f devops/docker-compose.yml up --build
```

| Service | URL |
|---|---|
| Angular frontend | http://localhost:80 |
| Backend API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| RabbitMQ management | http://localhost:15672 (guest/guest) |
| Grafana | http://localhost:3000 (admin/admin) |
| Prometheus | http://localhost:9090 |
| Actuator health | http://localhost:8080/actuator/health |

> MSSQL takes 15–25 seconds to become ready. The backend `depends_on: condition: service_healthy`
> gate ensures Flyway does not start until SQL Server is accepting connections.

---

## Technology Stack

| Layer | Technology | Version | Rationale |
|---|---|---|---|
| Frontend | Angular (NgModules) | 17 | Enterprise pattern; modular, maintainable |
| Backend | Spring Boot WAR | 3.2 / JDK 17 | Enterprise Java; WAR exercises the real JBoss deployment path |
| App server | WildFly | 30 | Open-source JBoss; production-grade runtime |
| Database | MS SQL Server | 2022 | `mcr.microsoft.com/mssql/server:2022-latest` (free developer licence) |
| Message broker | RabbitMQ | 3.12-management | Domain event streaming + audit trail |
| Schema migrations | Flyway | 9.x | Version-controlled DDL; no manual SQL |
| CI/CD | Jenkins (pipeline) + GitHub Actions | LTS | Jenkins mirrors what enterprise shops run; GHA for security scanning |
| Observability | LGTM stack | latest | Loki · Grafana · Tempo · Prometheus |
| Load testing | JMeter + Gatling + k6 | latest | All three tools, same scenario — output comparison |
| Profiling | JFR + async-profiler | JDK built-in | Zero-overhead continuous recording |
| E2E testing | Playwright | 1.x | Cross-browser, Page Object Model, CI-integrated |
| Containerisation | Docker + Compose | latest | Full stack with single `docker-compose up` |

### Why WAR on WildFly, not embedded Tomcat?

COSMO deploys as a WAR into JBoss/WildFly. CosmoLab replicates this exactly:
`spring-boot-starter-tomcat` is `provided` scope, `SpringBootServletInitializer` is
extended, and WildFly controls the Undertow thread pool. This is where the most
impactful performance tuning happens.

---

## Architecture

```
── DOMAIN TRAFFIC ──────────────────────────────────────────────────────────
Browser
  └─► cosmolab-frontend (Nginx :80)
          └─► /api proxy ─► cosmolab-backend (WildFly :8080)
                                ├─► sqlserver  (MSSQL :1433)
                                └─► rabbitmq   (AMQP :5672)

rabbitmq exchanges
  clinical.events  (topic)  ──► future consumers
  audit.log        (direct) ──► audit-queue ──► AuditEventConsumer

── OBSERVABILITY (independent of RabbitMQ) ─────────────────────────────────
cosmolab-backend  ──► /actuator/prometheus  ◄── prometheus (:9090)
cosmolab-backend  ──► OTLP :4317           ──► tempo (:3200)
cosmolab-backend  ──► Docker log driver    ──► loki (:3100)
prometheus, loki, tempo ──► grafana (:3000)
```

**Strict separation:** RabbitMQ carries domain events only. Metrics, logs, and traces flow
directly to the observability stack and never route through the broker. A RabbitMQ outage
must not affect dashboards.

---

## Domain Model (openEHR-aligned)

```
EHR                        one per patient, permanent, never deleted
└── Composition            one clinical document / encounter
    └── Entry subtypes
        ├── VitalSigns     OBSERVATION — raw measurements, no interpretation
        └── ProblemListEntry  EVALUATION — clinical interpretation, ICD-10 coded
```

### Entities

| Entity | Table | Key fields |
|---|---|---|
| `Patient` | `patients` | `id`, `first_name`, `last_name`, `personal_number` (UNIQUE), `ward`, `status` |
| `EhrRecord` | `ehr_records` | `ehr_id`, `subject_id → patients`, `system_id`, `status` |
| `Composition` | `compositions` | `id`, `ehr_id`, `composition_type`, `start_time`, `commit_time` |
| `VitalSigns` | `vital_signs` | `id`, `composition_id`, `recorded_at`, `systolic_bp`, `heart_rate`, `temperature`, `oxygen_saturation` |
| `ProblemListEntry` | `problem_list_entries` | `id`, `ehr_id` (direct FK), `icd10_code`, `display_name`, `severity`, `status` |

**`startTime` vs `commitTime`:** a nurse records at 06:00 but saves at 08:30 — both facts
are preserved. These are fundamentally different data points in clinical systems.

**NVARCHAR everywhere:** SQL Server VARCHAR corrupts Swedish characters (å, ä, ö).
All `String` columns use `@Column(columnDefinition = "NVARCHAR(100)")`.

**DATETIMEOFFSET(6) for `Instant`:** Hibernate 6 maps `java.time.Instant` to `TIMESTAMP_UTC`
(datetimeoffset), not `datetime2` as Hibernate 5 did. All `Instant` columns in Flyway
migrations use `DATETIMEOFFSET(6)`.

---

## REST API

Full interactive docs at **`/swagger-ui.html`** · OpenAPI spec at **`/v3/api-docs`**

```
# Demographics
GET  POST         /api/v1/patients
GET  PUT  DELETE  /api/v1/patients/{id}

# EHR
POST              /api/v1/ehr
GET               /api/v1/ehr/{ehrId}
GET               /api/v1/ehr/subject/{patientId}

# Compositions
POST              /api/v1/ehr/{ehrId}/compositions
GET               /api/v1/ehr/{ehrId}/compositions          ?type=&page=&size=
GET  PUT          /api/v1/ehr/{ehrId}/compositions/{id}

# Vital Signs
POST              /api/v1/ehr/{ehrId}/compositions/{cid}/vitals
GET               /api/v1/ehr/{ehrId}/vitals                ?from=&to=
GET               /api/v1/ehr/{ehrId}/vitals/latest         → 200 | 204

# Problem List
POST              /api/v1/ehr/{ehrId}/problems
GET               /api/v1/ehr/{ehrId}/problems              ?status=ACTIVE
GET  PUT          /api/v1/ehr/{ehrId}/problems/{id}

# Ward Overview  ← primary load test target
GET               /api/v1/ward/{wardId}/overview

# Actuator
GET               /actuator/health | /actuator/prometheus | /actuator/info
```

All error responses use RFC 7807 `ProblemDetail` (Spring 6 built-in).
Validation errors (400) include a `fieldErrors` map in the `properties` extension.

---

## Project Structure

```
cosmolab/
├── .github/
│   └── workflows/
│       ├── codeql.yml          # CodeQL SAST on push/PR/weekly
│       └── snyk.yml            # Snyk dependency scan + monitor
├── backend/                    # Spring Boot 3.2 WAR
│   ├── Dockerfile              # Multi-stage: Maven build → WildFly 30
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/arcticsurge/cosmolab/
│       │   ├── config/         # WebConfig, RabbitMQConfig, OpenApiConfig, SecurityConfig
│       │   ├── domain/         # Entities + repository ports (zero framework deps)
│       │   ├── application/    # @Service use cases + typed exceptions
│       │   ├── infrastructure/ # JPA impls + RabbitMQ publisher/consumer
│       │   └── interfaces/rest/ # Controllers + DTOs + GlobalExceptionHandler
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration/   # V1–V6 Flyway SQL migrations
│       └── test/java/          # Testcontainers integration tests
├── frontend/                   # Angular 17 NgModules SPA
├── testing/
│   ├── e2e/                    # Playwright (Chromium + Firefox)
│   └── performance/            # JMeter · Gatling · k6
└── devops/
    ├── docker-compose.yml
    ├── docker-compose.dev.yml  # Debug port 5005, SQL logging
    ├── scripts/
    │   └── wait-for-health.sh
    ├── jenkins/
    │   ├── Dockerfile
    │   └── Jenkinsfile          # 7-stage pipeline
    └── observability/
        ├── prometheus.yml
        ├── loki/
        ├── tempo/
        └── grafana/            # Auto-provisioned datasources + dashboards
```

---

## Backend — DDD-lite Architecture

```
com.arcticsurge.cosmolab
├── domain/         zero Spring/JPA imports on interfaces; repository ports only
├── application/    @Service, @Transactional; orchestrates domain + infrastructure
│                   typed exceptions per domain concept (PatientNotFoundException, etc.)
├── infrastructure/ JPA repository implementations; RabbitMQ publisher + consumer
└── interfaces/rest/ controllers + DTOs; domain entities never exposed directly
```

### Key patterns

**`Optional` — never check-then-act:**
```java
// Wrong: two DB hits, TOCTOU race
if (repo.existsBySubjectId(id)) return repo.findBySubjectId(id).get();

// Correct: single query, atomic
return repo.findBySubjectId(id).orElseGet(() -> {
    EhrRecord ehr = new EhrRecord();
    ehr.setSubjectId(id);
    return repo.save(ehr);
});
```

**Stream `flatMap` over imperative loops:**
```java
// WardOverviewService — Optional.stream() silently skips patients with no EHR
return patients.stream()
    .flatMap(p -> ehrRepo.findBySubjectId(p.getId())
        .map(ehr -> buildSummary(p, ehr)).stream())
    .toList();
```

**RabbitMQ — two strategies:**
- `clinical.events` (topic exchange) — async fire-and-forget; routing key per event type
- `audit.log` (direct exchange) → quorum queue with manual ack + idempotent consumer

### Flyway Migrations

| Version | Content |
|---|---|
| V1 | `patients` + `ehr_records` + indexes |
| V2 | `compositions` |
| V3 | `vital_signs` |
| V4 | `problem_list_entries` |
| V5 | Seed: 20 patients + EHR records (ICU, Cardiology, Neurology wards) |
| V6 | Seed: compositions, vitals, problem list entries per patient |

---

## Testing

### Integration — Testcontainers

Tests run against **real SQL Server 2022 and RabbitMQ 3.12 containers**, not H2 mocks.
H2 `MODE=MSSQLServer` masks dialect-specific issues (e.g. `DATETIMEOFFSET` vs `DATETIME2`).

```
backend/src/test/java/…/AbstractIntegrationTest.java
  @Container static MSSQLServerContainer MSSQL (with init-db.sql)
  @Container static RabbitMQContainer RABBIT
  @DynamicPropertySource → injects container ports before ApplicationContext boots
```

Run with `mvn test` — requires Docker. Containers start once per JVM run (~25 s for MSSQL)
and are shared across all test classes via Spring's context cache.

### E2E — Playwright *(Sprint 4)*

```
testing/e2e/
├── pages/          Page Object Model — all selectors here, never in test files
└── tests/          ward-overview, patient-list, patient-detail, vitals, problems, navigation
```

Selectors use `data-testid` attributes only — never CSS classes or text content.

### Performance — JMeter + Gatling + k6 *(Sprint 6–7)*

All three tools run the same 3-scenario profile (50 users, 30 s ramp, 3 min steady state)
against `GET /api/v1/ward/{wardId}/overview` as the primary target.

---

## Security Scanning (GitHub Actions)

Two workflows run on every push and PR to `master`, plus a weekly schedule:

### CodeQL (`.github/workflows/codeql.yml`)

Static analysis of Java bytecode. Uses the `security-and-quality` query suite covering
OWASP Top 10 (injection, path traversal, XXE) and code quality checks.
Results appear in **Security → Code scanning** on GitHub.

### Snyk (`.github/workflows/snyk.yml`)

Dependency and supply-chain vulnerability scanning.

| Step | When | What |
|---|---|---|
| `snyk test` | every push / PR | checks Maven deps against CVE database; HIGH + CRITICAL reported |
| Upload SARIF | every push / PR | results appear in GitHub Security tab |
| `snyk monitor` | push to `master` only | records dependency snapshot in Snyk dashboard for new-CVE alerting |

**Required secret:** `SNYK_TOKEN` — add at Settings → Secrets and variables → Actions.
Get the token from [app.snyk.io](https://app.snyk.io) → Account Settings → API token.

---

## Observability (LGTM Stack)

| Signal | Pipeline |
|---|---|
| Metrics | Micrometer → `/actuator/prometheus` → Prometheus → Grafana |
| Logs | Logback → Docker log driver → Loki → Grafana |
| Traces | OpenTelemetry → OTLP :4317 → Tempo → Grafana |

Three pre-provisioned Grafana dashboards: JVM (`cosmolab-jvm.json`), HTTP (`cosmolab-http.json`), RabbitMQ (`cosmolab-rabbitmq.json`).

RabbitMQ metrics are scraped from port **15692** (Prometheus plugin), not from the
application — the two pipelines are entirely independent.

---

## Performance Engineering (Sprint 7)

**Primary target:** `GET /api/v1/ward/{wardId}/overview` — 4-table join with window function
for latest vitals per EHR and problem count aggregation. Designed to expose N+1 risks and
thread pool saturation at realistic concurrency.

**Profiling workflow:**
1. Warm-up run at 25 users/s for 60 s (lets JIT compile hot paths)
2. Start JFR: `docker exec cosmolab-backend jcmd 1 JFR.start duration=120s filename=/tmp/cosmolab.jfr settings=profile`
3. Run Gatling at 50 users, 3 min steady state
4. Copy JFR: `docker cp cosmolab-backend:/tmp/cosmolab.jfr ./profiling/`
5. Open in JMC → Method Profiling → sort by Self time

**Tuning order (highest to lowest expected impact):**
1. Undertow worker threads (`task-max-threads` 32 → 200 in `standalone.xml`)
2. WardOverview N+1 query (native SQL + window function vs per-patient EHR lookups)
3. JPA batch fetch size
4. G1GC flags (`MaxGCPauseMillis`, `G1HeapRegionSize`)

**WildFly JVM flags** (set in `JAVA_OPTS`):
```
-Xms512m -Xmx1024m
-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=16m
-XX:+FlightRecorder
-XX:StartFlightRecording=duration=0,filename=/tmp/cosmolab.jfr,settings=profile,maxsize=256m,dumponexit=true
```

JMX enabled on port 9999 for async-profiler attach and JConsole inspection during load tests.

---

## CI/CD — Jenkins Pipeline *(Sprint 5)*

7-stage declarative pipeline in `devops/jenkins/Jenkinsfile`:

| Stage | Action |
|---|---|
| Checkout | Clone from SCM |
| Backend test | `mvn test` (Testcontainers — requires Docker socket) |
| Backend build | `mvn package -DskipTests` → WAR |
| Frontend build | `npm ci && ng build` |
| Docker build | `docker-compose build` |
| E2E tests | `wait-for-health.sh` → Playwright on Chromium + Firefox |
| Deploy | `docker-compose up -d` |

Post: `docker-compose down`, archive JUnit XML, publish Playwright HTML report.

---

## Sprint Status

| Sprint | Focus | Status |
|---|---|---|
| 1 | Infrastructure + domain model | **Done** |
| 2 | Backend API + RabbitMQ + MockMvc tests | In progress |
| 3 | Angular frontend | Pending |
| 4 | Containerisation + Playwright E2E | Pending |
| 5 | Jenkins pipeline | Pending |
| 6 | Observability + JMeter baseline | Pending |
| 7 | Gatling + k6 + performance optimisation | Pending |

**Sprint 1 delivered:**
- Full domain model (Patient, EHR, Composition, VitalSigns, ProblemListEntry)
- All 6 REST controllers + GlobalExceptionHandler (RFC 7807)
- RabbitMQ topology (clinical.events topic + audit.log quorum queue)
- Docker Compose stack with all 8 services + healthchecks
- Flyway V1–V6 (schema + seed data for 20 patients)
- OpenAPI/Swagger UI via springdoc-openapi 2.5.0
- Testcontainers integration (real SQL Server + RabbitMQ in tests)
- CodeQL + Snyk GitHub Actions workflows
- Modern Java patterns throughout (Optional.orElseGet, stream flatMap, typed exceptions)

---

## Known Gotchas

| Gotcha | Detail |
|---|---|
| `DATETIMEOFFSET` not `DATETIME2` | Hibernate 6 maps `java.time.Instant` to `datetimeoffset`; using `DATETIME2` in Flyway DDL causes `SchemaManagementException` at startup |
| MSSQL startup time | Container takes 15–25 s. `depends_on: condition: service_healthy` is mandatory — Flyway cannot retry a failed boot |
| `jboss-deployment-structure.xml` | Must exclude WildFly's bundled Jackson and Hibernate or WildFly's parent-first classloader silently uses wrong versions |
| NVARCHAR not VARCHAR | VARCHAR corrupts å/ä/ö; all `String` columns need `columnDefinition = "NVARCHAR(n)"` |
| WAR context root | Deployed as `ROOT.war` so context root is `/`; API at `http://localhost:8080/api/v1/…` without app prefix |
| Testcontainers requires Docker | `mvn test` fails without Docker running; CI must mount the Docker socket |
| Snyk needs `SNYK_TOKEN` secret | Workflow is no-op until the secret is configured in repository settings |

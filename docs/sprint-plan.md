---
description: Sprint-by-sprint implementation tasks and exit criteria. Use when asked what to build next, what the current sprint covers, or what the exit criteria are.
alwaysApply: false
---

# CosmoLab — Sprint Plan

## Sprint Overview

| Sprint | Focus | Exit criterion |
|---|---|---|
| 1 | Infrastructure + domain model | docker-compose up healthy; Flyway V1-V6 applied; /actuator/health UP |
| 2 | Backend API + RabbitMQ + unit tests | All endpoints correct; events in RabbitMQ UI; MockMvc pass |
| 3 | Angular frontend | All pages navigable; live data; no console errors |
| 4 | Containerisation + Playwright E2E | Full stack in containers; Playwright green |
| 5 | Jenkins pipeline | All 7 stages green; Playwright report published |
| 6 | Observability + JMeter baseline | Grafana live under load; JMeter HTML report with p95 |
| 7 | Gatling + k6 + optimisation | Before/after benchmark with profiling evidence |

---

## Sprint 1 — Infrastructure + Domain Model

**pom.xml deps**: `mssql-jdbc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-amqp`,
`spring-boot-starter-web`, `spring-boot-starter-actuator`, `flyway-sqlserver`,
`micrometer-registry-prometheus`, `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`,
`loki-logback-appender`, `lombok`, `spring-boot-starter-validation`.
`spring-boot-starter-tomcat` scope `provided`.

**Files to create**:
- `devops/docker-compose.yml` — all services + healthchecks (MSSQL TCP probe, RabbitMQ ping)
- `devops/docker-compose.dev.yml` — source mounts, JPDA port 5005
- `Application.java` + `CosmoLabServletInitializer.java`
- `webapp/WEB-INF/jboss-deployment-structure.xml`
- `application.yml` — datasource, rabbitmq, flyway, actuator, OTLP
- `V1__patients_and_ehr.sql` through `V6__seed_clinical_data.sql`
- Domain entities: `Patient`, `EhrRecord`, `Composition`, `VitalSigns`, `ProblemListEntry`
- Domain repository interfaces + Spring Data JPA implementations

**Exit**: `mvn clean install` succeeds. All containers healthy. Flyway V1-V6 applied. `/actuator/health` UP.

---

## Sprint 2 — Backend API + RabbitMQ Events

**Files to create**:
- Services: `EhrService`, `CompositionService`, `VitalSignsService`, `ProblemListService`, `WardOverviewService`
- Controllers: one per resource + `GlobalExceptionHandler`
- `RabbitMQConfig.java` — exchanges, quorum queue, binding
- `ClinicalEvent.java` record + `ClinicalEventPublisher.java` + `AuditEventConsumer.java`
- DTOs: request/response per resource + `PagedResponse<T>` + `WardOverviewResponse`
- Tests: `EhrControllerTest`, `VitalSignsControllerTest`, `ProblemListControllerTest`,
  `WardOverviewControllerTest`, `CompositionControllerTest`

**Exit**: All MockMvc tests pass. RabbitMQ UI (`:15672`) shows messages on `clinical.events` after any write. WardOverview returns populated ICU response.

---

## Sprint 3 — Angular Frontend

**Files to create**:
- `AppModule`, `AppRoutingModule`, `CoreModule`, `SharedModule`
- `ApiInterceptor`, `AuthGuard`, `AuthService`, `NotificationService`
- Shared: `PageHeaderComponent`, `LoadingSpinnerComponent`, `SeverityBadgeComponent`, `VitalSignsChartComponent`
- `WardModule` → `WardOverviewComponent`
- `PatientsModule` → `PatientListComponent` + `PatientDetailComponent` (4 tabs)
- `AdminModule` → `AdminComponent`
- Angular services: `EhrService`, `VitalSignsService`, `ProblemListService`, `WardOverviewService`

**Exit**: Full navigation flow. All 4 patient detail tabs populated. Adding vitals updates sparkline. Adding problem appears in list. No console errors.

---

## Sprint 4 — Containerisation + Playwright E2E

**Files to create**:
- `cosmolab-backend/Dockerfile` (multi-stage: Maven build → WildFly deploy)
- `cosmolab-frontend/Dockerfile` (multi-stage: npm build → Nginx serve)
- `cosmolab-frontend/nginx.conf`
- `testing/e2e/playwright.config.ts`
- Page objects: `WardOverviewPage`, `PatientListPage`, `PatientDetailPage`, `CreateVitalsPage`, `AddProblemPage`
- Tests: `ward-overview.spec.ts`, `patient-list.spec.ts`, `patient-detail.spec.ts`,
  `create-vitals.spec.ts`, `add-problem.spec.ts`, `navigation.spec.ts`

**Exit**: `docker-compose up --build` starts cleanly. Playwright exits 0 on Chromium + Firefox.

---

## Sprint 5 — Jenkins Pipeline

**Files to create**:
- `devops/jenkins/Dockerfile` — Jenkins LTS + Docker CLI + Maven 3.9 + Node 20 + Playwright deps
- `devops/jenkins/Jenkinsfile` — 7 stages: Checkout → Backend test → Backend build →
  Frontend build → Docker build → E2E tests → Deploy

Docker socket: `/var/run/docker.sock` bind mount (no DinD).
Post always: `docker-compose down` + archive JUnit XML + publish Playwright HTML report.

**Exit**: All 7 stages green. Playwright report accessible from Jenkins build page.

---

## Sprint 6 — Observability + JMeter Baseline

**Files to create**:
- `devops/observability/prometheus.yml` — scrape backend + rabbitmq:15692
- `devops/observability/loki/loki-config.yml`
- `devops/observability/grafana/datasources/datasources.yml`
- `devops/observability/grafana/dashboards/cosmolab-jvm.json`
- `devops/observability/grafana/dashboards/cosmolab-http.json`
- `devops/observability/grafana/dashboards/cosmolab-rabbitmq.json`
- `testing/performance/jmeter/cosmolab-baseline.jmx` — 3 samplers, 50 users, 3 min

**Exit**: Grafana shows live metrics under load. JMeter HTML report with p95 numbers.

---

## Sprint 7 — Gatling + k6 + Optimisation

**Files to create**:
- `testing/performance/gatling/pom.xml` + `CosmoLabSimulation.scala`
- `testing/performance/k6/patient-load.js`
- `profiling/` directory for flame graph output

**Workflow**: warm-up → async-profiler → identify hotspot → apply tuning → re-run Gatling → record delta.

**Tuning order**: (1) Undertow threads, (2) WardOverview N+1, (3) JPA BatchSize, (4) G1GC flags.

**Exit**: Measurable p95 improvement. Flame graph saved. Results table in `testing-performance.md` filled.

---

## Deferred Items

| Item | Target |
|---|---|
| Real auth (Spring Security + IDP) | Post-sprint 7 |
| WildFly 2-node cluster | Optional sprint 8 |
| CareInstruction (INSTRUCTION archetype) | Optional sprint 8 |
| FHIR R4 API layer | Phase 2 |

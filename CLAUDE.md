# CosmoLab — Project Context Document

> **Single source of truth** for architecture, sprint plan, and interview preparation.
> This file is the first context document passed to any AI assistant session.
> Update it as each phase completes.
>
> **Related documents** — domain-specific detail lives in `docs/`; this file is the entry point.
> - [`docs/clinical-domain.md`](docs/clinical-domain.md) — openEHR containment hierarchy, EHR/Composition/VitalSigns/ProblemListEntry entities, interview talking points
> - [`docs/cosmolab-architecture.md`](docs/cosmolab-architecture.md) — container diagram, full backend package tree, complete REST API surface, RabbitMQ topology, Flyway sequence
> - [`docs/backend-java.md`](docs/backend-java.md) — Spring Boot WAR on WildFly conventions, JPA/MSSQL patterns, RabbitMQ integration, validation, error handling
> - [`docs/frontend-angular.md`](docs/frontend-angular.md) — NgModules structure, routing, mock JWT auth pattern, component conventions, data-testid inventory
> - [`docs/devops-infra.md`](docs/devops-infra.md) — Docker Compose services, WildFly tuning, JMX, JFR, MSSQL container config, Nginx
> - [`docs/sprint-plan.md`](docs/sprint-plan.md) — sprint-by-sprint tasks and exit criteria
> - [`docs/testing-e2e.md`](docs/testing-e2e.md) — Playwright config, Page Object Model rules, test scenarios, CI integration
> - [`docs/testing-performance.md`](docs/testing-performance.md) — JMeter, Gatling, k6 scripts, profiling workflow, tuning targets, results table

---

## 1. Project Overview

### 1.1 What is CosmoLab?

CosmoLab is a minimal clinical information system that mirrors the Cambio COSMIC technology stack. It covers patient management, clinical notes, and an audit trail — enough domain complexity to generate realistic load test scenarios and meaningful performance data.

It serves two simultaneous goals:

- **Interview preparation**: hands-on muscle memory with the exact stack Cambio runs (Spring Boot WAR, WildFly, MS SQL Server, JMeter, Gatling, k6, JFR profiling)
- **Portfolio evidence**: a full-cycle project demonstrating backend engineering depth, containerisation, CI/CD, observability, and data-driven performance optimisation — all in one codebase

### 1.2 Domain model rationale

The clinical domain was chosen deliberately. It is write-heavy (notes created constantly), read-heavy (ward dashboards polling patient lists), has mixed workloads (OLTP reads + occasional bulk queries), and produces an audit trail that justifies a message broker. This makes it a realistic substrate for performance engineering — unlike toy CRUD apps where all endpoints behave identically under load.

### 1.3 Interview alignment summary

| Cambio JD requirement | CosmoLab evidence |
|---|---|
| JMeter load testing | `testing/performance/jmeter/` + HTML report with latency percentiles |
| Gatling performance testing | `testing/performance/gatling/` + simulation report |
| k6 scripting | `testing/performance/k6/` + Prometheus remote write integration |
| Java profiling and flame graphs | JFR recording + JMC analysis + before/after benchmark |
| Spring Boot / Java enterprise backend | Full DDD-lite backend, JPA, RabbitMQ, validation, error handling |
| Containerisation and Docker | `devops/docker-compose.yml` + multi-stage Dockerfiles |
| CI/CD pipeline experience | `devops/jenkins/Jenkinsfile` — declarative pipeline, 7 stages |
| Observability and monitoring | Grafana dashboards, Prometheus, Loki logs, Tempo traces |
| Angular frontend | 4-page NgModules app with routing, guards, lazy loading |
| MS SQL Server | Flyway-versioned schema, MSSQL-specific JPA config, query tuning |
| End-to-end testing | Playwright suite — Page Object Model, Chromium + Firefox, CI-integrated |

---

## 2. Technology Stack

### 2.1 Chosen technologies

| Layer | Technology | Version | Reason |
|---|---|---|---|
| Frontend | Angular (NgModules) | 17 | Enterprise pattern; closer to legacy COSMIC than standalone components |
| Backend | Spring Boot WAR | 3.2 / JDK 17 | COSMIC is Spring-based; WAR exercises the real JBoss deployment path |
| App server | WildFly | 30 | Open-source JBoss; identical runtime to COSMIC production |
| Database | MS SQL Server | 2022 | `mcr.microsoft.com/mssql/server:2022-latest` (free dev license) |
| Event bus | RabbitMQ | 3.12 | Domain event streaming (clinical.events, audit.log); management UI on :15672 |
| Schema migrations | Flyway | 9.x | Version-controlled schema; no manual SQL scripts |
| CI/CD | Jenkins | LTS | What most Java enterprise shops run; Jenkinsfile is the artefact |
| Observability | LGTM stack | latest | **Independent of messaging** — Loki · Grafana · Tempo · Prometheus; all fed directly from app, not via RabbitMQ |
| Load testing | JMeter + Gatling + k6 | latest | All three — same scenario, compare outputs |
| Profiling | JFR + async-profiler | JDK built-in | JFR is zero-overhead in production; JMC for analysis |
| E2E testing | Playwright | 1.x | Cross-browser, reliable selectors, CI-friendly, screenshot on failure |
| Containerisation | Docker + Compose | latest | Full stack runs with single `docker-compose up` |

### 2.2 Why WAR on WildFly, not Spring Boot embedded Tomcat?

Spring Boot defaults to embedding Tomcat and running as a fat JAR. COSMIC does not do this — it deploys as a WAR into JBoss/WildFly, which provides the servlet container (Undertow). CosmoLab replicates this exactly.

The practical implication: `spring-boot-starter-tomcat` must be `provided` scope in `pom.xml`, and `SpringBootServletInitializer` must be extended. WildFly controls the thread pool, HTTP listener, and datasource JNDI bindings. This is where WildFly-specific tuning (Undertow worker threads, buffer pools) actually matters.

### 2.3 Known gotchas

- WildFly 30 uses Jakarta EE 10 (`jakarta.*` namespace). Spring Boot 3.x already uses Jakarta, so no conflicts — but any old `javax.*` dependency in the classpath will cause classloading failures.
- The MSSQL JDBC driver must be bundled inside the WAR. WildFly does not provide it as a module in this setup.
- MSSQL container takes 15–25 seconds to become ready. Docker Compose healthcheck + `depends_on: condition: service_healthy` is mandatory or Flyway will fail on startup with connection refused.
- Spring Boot auto-configuration scanning must be explicitly limited when deploying to WildFly to avoid conflicts with container-managed resources.
- WildFly ships with its own versions of Jackson, Hibernate, and Weld. The WAR must include `jboss-deployment-structure.xml` to exclude WildFly's bundled modules and force the WAR's versions.
- Angular `HashLocationStrategy` simplifies Nginx SPA routing — avoids 404s on deep-link refresh without complex Nginx try_files config.
- **Messaging and observability are strictly independent pipelines.** RabbitMQ carries domain events only. Metrics flow from Micrometer → `/actuator/prometheus` → Prometheus. Logs flow from Logback → Docker log driver → Loki. Traces flow from OpenTelemetry → OTLP → Tempo. None of these go through RabbitMQ. A RabbitMQ outage must not affect the observability stack, and vice versa.

---

## 3. Architecture

### 3.1 System context

The system has three external actors: a **clinical user** (browser running the Angular frontend), a **Jenkins operator** (triggers CI pipeline), and a **Grafana operator** (reads dashboards). All runtime traffic flows through Docker Compose on a single host for CosmoLab.

### 3.2 Containers and communication

```
── DOMAIN TRAFFIC ──────────────────────────────────────────────────────────
Browser
  └─► cosmolab-frontend  (Nginx :80)
          └─► /api proxy ─► cosmolab-backend  (WildFly :8080)
                                  ├─► sqlserver     (MSSQL :1433)
                                  └─► rabbitmq      (AMQP :5672, Management UI :15672)

rabbitmq exchanges:
  clinical.events  ──► (future consumers / audit service)
  audit.log        ──► audit-queue (consumed by AuditEventConsumer in same app)

── OBSERVABILITY (independent — never routes through RabbitMQ) ──────────────
cosmolab-backend  ──► /actuator/prometheus  ◄── prometheus (:9090)
cosmolab-backend  ──► OTLP :4317           ──► tempo       (:3200)
cosmolab-backend  ──► Docker log driver    ──► loki        (:3100)
cosmolab-frontend ──► Docker log driver    ──► loki

prometheus ─► grafana (:3000)
loki       ─► grafana
tempo      ─► grafana

── CI / E2E ────────────────────────────────────────────────────────────────
Jenkins (:8090) ─► docker build ─► docker-compose deploy
Playwright      ─► http://localhost:80
```

### 3.3 Backend package structure (DDD-lite)

DDD-lite: a domain layer with zero framework dependencies, an application layer that orchestrates use cases, an infrastructure layer with all adapters (JPA, RabbitMQ), and an interfaces layer with REST controllers. This is the layering used in real enterprise codebases and what Cambio engineers will recognise.

The domain follows the **openEHR containment hierarchy**: EHR → Composition → Entry subtypes (OBSERVATION, EVALUATION). See [`docs/clinical-domain.md`](docs/clinical-domain.md) for entity definitions and interview talking points. See [`docs/cosmolab-architecture.md`](docs/cosmolab-architecture.md) for the full annotated package tree.

```
com.cosmolab
├── config/
│   ├── WebConfig.java             # CORS, MVC config
│   ├── RabbitMQConfig.java        # Exchange, queue, and binding bean declarations
│   ├── ObservabilityConfig.java   # Micrometer tracing + OpenTelemetry export (independent of messaging)
│   └── SecurityConfig.java        # Spring Security (disabled; scaffolded for future)
│
├── domain/                        # zero framework dependencies
│   ├── patient/     Patient, PatientStatus, Gender, PatientRepository (port)
│   ├── ehr/         EhrRecord, EhrRepository (port)
│   ├── composition/ Composition, CompositionType, CompositionStatus, repository
│   ├── observation/ VitalSigns, repository
│   └── evaluation/  ProblemListEntry, ProblemStatus, Severity (MILD|MODERATE|SEVERE), repository
│
├── application/                   # use-case orchestration; @Service, @Transactional
│   ├── ehr/         EhrService
│   ├── composition/ CompositionService
│   ├── observation/ VitalSignsService
│   ├── evaluation/  ProblemListService
│   └── ward/        WardOverviewService  # aggregation query — primary load test target
│
├── infrastructure/
│   ├── persistence/ JPA implementations of all domain repository ports
│   └── messaging/
│       ├── ClinicalEvent.java           # Record: eventType, aggregateId, payload, occurredAt
│       ├── ClinicalEventPublisher.java  # RabbitTemplate wrapper — publishes to clinical.events exchange
│       └── AuditEventConsumer.java      # @RabbitListener on audit-queue; idempotent DB write
│
└── interfaces/rest/
    ├── EhrController, CompositionController, VitalSignsController
    ├── ProblemListController, WardOverviewController
    ├── GlobalExceptionHandler.java      # @RestControllerAdvice, ProblemDetail (RFC 7807)
    └── dto/                             # Request/Response DTOs + PagedResponse<T> + WardOverviewResponse
```

### 3.4 Frontend module structure (NgModules)

`CoreModule` is imported once in `AppModule` and contains singleton services and interceptors. `SharedModule` is imported by every feature module and contains reusable components. Feature modules are lazy-loaded via the Angular router. See [`docs/frontend-angular.md`](docs/frontend-angular.md) for routing code, auth pattern, component conventions, and the full `data-testid` attribute inventory.

```
src/app/
├── app.module.ts
├── app-routing.module.ts            # default → /ward/ICU; lazy loads all feature modules
│
├── core/
│   ├── guards/auth.guard.ts         # Redirects if no token in sessionStorage
│   ├── interceptors/api.interceptor.ts  # Injects Bearer token; maps API errors to notifications
│   └── services/
│       ├── auth.service.ts          # Stores/retrieves mock JWT
│       └── notification.service.ts  # Global snackbar messages
│
├── shared/components/
│   ├── page-header/
│   ├── loading-spinner/
│   ├── severity-badge/              # MILD/MODERATE/SEVERE + ROUTINE/URGENT/CRITICAL chips
│   └── vital-signs-chart/           # Sparkline; cells amber/red outside normal range
│
└── features/
    ├── ward/                        # Route: /ward/:wardId — WardOverviewComponent (primary view)
    ├── patients/                    # Routes: /patients, /patients/:id
    │   ├── patient-list/            # Paginated table, search, ward/status filter
    │   └── patient-detail/          # mat-tab-group: Overview | Vitals | Problems | Compositions
    └── admin/                       # Route: /admin — links to Grafana, RabbitMQ UI, Actuator
```

### 3.5 Domain model and schema

The schema follows the openEHR containment hierarchy. Full column definitions are in [`docs/cosmolab-architecture.md`](docs/cosmolab-architecture.md).

```
patients            id, first_name, last_name, personal_number UNIQUE, date_of_birth,
                    gender, ward, status (ACTIVE|DISCHARGED|TRANSFERRED), created_at, updated_at
                    INDEX: UNIQUE(personal_number), NONCLUSTERED(ward, status)

ehr_records         ehr_id PK, subject_id FK→patients, system_id, created_at, status (ACTIVE|INACTIVE)

compositions        id PK, ehr_id FK→ehr_records, composition_type, author_id,
                    start_time (clinical), commit_time (system), facility_name, status

vital_signs         id PK, composition_id FK→compositions, recorded_at, recorded_by,
                    systolic_bp, diastolic_bp, heart_rate, respiratory_rate,
                    temperature DECIMAL(4,1), oxygen_saturation DECIMAL(5,2), weight DECIMAL(5,2)
                    INDEX: NONCLUSTERED(composition_id, recorded_at DESC)

problem_list_entries id PK, composition_id FK→compositions, ehr_id FK→ehr_records (direct — efficient queries),
                    icd10_code, display_name, severity (MILD|MODERATE|SEVERE),
                    status (ACTIVE|INACTIVE|RESOLVED|REFUTED), onset_date, resolved_date, recorded_at
```

Flyway migration sequence: V1 patients+ehr → V2 compositions → V3 vital_signs → V4 problem_list_entries → V5 seed patients → V6 seed clinical data.

All text columns use `NVARCHAR` — SQL Server VARCHAR silently corrupts Swedish characters (å, ä, ö). This is a common interview gotcha; always use `@Column(columnDefinition = "NVARCHAR(100)")` in JPA entities.

### 3.6 REST API

Full endpoint list with query parameters is in [`docs/cosmolab-architecture.md`](docs/cosmolab-architecture.md).

```
# Demographics
GET|POST          /api/v1/patients
GET|PUT|DELETE    /api/v1/patients/{id}         # DELETE = soft-delete (status=DISCHARGED)

# EHR
POST              /api/v1/ehr
GET               /api/v1/ehr/{ehrId}
GET               /api/v1/ehr/subject/{patientId}

# Compositions
POST              /api/v1/ehr/{ehrId}/compositions
GET               /api/v1/ehr/{ehrId}/compositions    ?type=&page=&size=
GET|PUT           /api/v1/ehr/{ehrId}/compositions/{id}

# Vital Signs
POST              /api/v1/ehr/{ehrId}/compositions/{cid}/vitals
GET               /api/v1/ehr/{ehrId}/vitals           ?from=&to=
GET               /api/v1/ehr/{ehrId}/vitals/latest

# Problem List
POST              /api/v1/ehr/{ehrId}/problems
GET               /api/v1/ehr/{ehrId}/problems         ?status=ACTIVE
GET|PUT           /api/v1/ehr/{ehrId}/problems/{id}

# Ward Overview (primary load test target — 4-table join + window function)
GET               /api/v1/ward/{wardId}/overview

# Actuator
GET               /actuator/health
GET               /actuator/prometheus
GET               /actuator/info
```

All error responses use RFC 7807 `ProblemDetail` (Spring 6 built-in). Validation errors (400) include a `fieldErrors` map in `properties`. See [`docs/backend-java.md`](docs/backend-java.md) for the `GlobalExceptionHandler` implementation.

### 3.7 RabbitMQ topology

RabbitMQ carries **domain events only**. It has no role in observability.

```
Exchange: clinical.events   (type: topic, durable)
  Routing keys:
    patient.created         ──► (no consumer in CosmoLab; ready for future audit/analytics service)
    patient.updated
    patient.discharged
    note.created
    note.updated

Exchange: audit.log         (type: direct, durable)
  Routing key: audit
    └──► Queue: audit-queue (durable, quorum queue)
           └──► AuditEventConsumer  @RabbitListener
                  └──► Persists to audit_events table (idempotent: checks eventId before insert)
```

Why topic exchange for `clinical.events`: topic exchanges let future consumers subscribe by pattern (e.g. `patient.*` for a patient analytics service, `*.created` for a provisioning service) without changing the publisher. Direct exchange for `audit.log` is deliberate — audit has exactly one consumer and topic routing would add unnecessary complexity.

Why quorum queues for `audit-queue`: quorum queues replicate to a majority of nodes and survive broker restarts without message loss. Classic mirrored queues are deprecated in RabbitMQ 3.12. Even on a single-node dev setup, declaring as quorum queue ensures the config is production-correct.

---

## 4. Infrastructure

### 4.1 Docker Compose services

> Full Dockerfile content, healthcheck YAML, and Nginx config are in [`docs/devops-infra.md`](docs/devops-infra.md).

| Service | Image | Ports | Notes |
|---|---|---|---|
| `sqlserver` | `mcr.microsoft.com/mssql/server:2022-latest` | 1433 | `ACCEPT_EULA=Y`, `MSSQL_PID=Developer`; TCP healthcheck |
| `rabbitmq` | `rabbitmq:3.12-management` | 5672 (AMQP), 15672 (Management UI), 15692 (Prometheus plugin) | management image includes the management plugin; Prometheus plugin exposes metrics on 15692 |
| `cosmolab-backend` | custom (WildFly 30) | 8080, 9990, 9999 (JMX) | Depends on sqlserver + rabbitmq healthy; JMX enables async-profiler + JConsole |
| `cosmolab-frontend` | custom (Nginx) | 80 | Proxies `/api` to `cosmolab-backend:8080` |
| `prometheus` | `prom/prometheus` | 9090 | Scrapes `/actuator/prometheus` — independent of RabbitMQ |
| `loki` | `grafana/loki` | 3100 | Receives container logs via Docker log driver — independent of RabbitMQ |
| `tempo` | `grafana/tempo` | 3200, 4317 | OTLP ingest on 4317 — independent of RabbitMQ |
| `grafana` | `grafana/grafana` | 3000 | Datasources provisioned from `devops/observability/grafana/` |

MSSQL startup time is 15–25 seconds. The healthcheck must probe TCP port 1433 (not HTTP). The backend `depends_on` must use `condition: service_healthy`. Without this, Flyway attempts to connect before SQL Server is accepting connections and throws a non-retryable exception.

### 4.2 WildFly configuration

Key customisations to `standalone.xml`:

**Undertow worker threads** (first tuning target on Day 7):
```xml
<subsystem xmlns="urn:jboss:domain:undertow:12.0">
  <worker name="default" io-threads="4" task-max-threads="200"/>
</subsystem>
```
Default `task-max-threads` is `8 * io-threads` = 32. Under 50 concurrent users with any DB query latency, this saturates and requests queue. Increasing to 200 is the single highest-impact change.

**JVM flags in `standalone.conf`** (added to `JAVA_OPTS`):
```bash
-Xms512m -Xmx1024m
-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=16m
-XX:+FlightRecorder
-XX:StartFlightRecording=duration=0,filename=/tmp/cosmolab.jfr,settings=profile,maxsize=256m
```

The always-on JFR recording with `duration=0` runs continuously and rolls over at 256 MB. This is the production-safe profiling pattern — zero overhead, data always available.

### 4.3 MS SQL Server in Docker

Image: `mcr.microsoft.com/mssql/server:2022-latest`

Required environment variables:
- `ACCEPT_EULA=Y` — mandatory, without this the container exits immediately
- `MSSQL_SA_PASSWORD` — must meet SQL Server complexity policy: minimum 8 characters with uppercase, lowercase, digit, and symbol. Use something like `CosmoLab@2024` in dev.
- `MSSQL_PID=Developer` — enables the full Developer edition for free (no production use)

Flyway connects as `sa` in dev via JDBC URL: `jdbc:sqlserver://sqlserver:1433;databaseName=cosmolab;encrypt=false;trustServerCertificate=true`. The `encrypt=false` avoids TLS certificate errors in the dev container setup.

---

## 5. Sprint Plan

Full sprint-by-sprint detail is in [`docs/sprint-plan.md`](docs/sprint-plan.md).

| Sprint | Focus | Exit criterion |
|---|---|---|
| 1 | Infrastructure + domain model | `docker-compose up` healthy; Flyway V1-V6 applied; `/actuator/health` UP |
| 2 | Backend API + RabbitMQ + unit tests | All endpoints correct; events in RabbitMQ UI; MockMvc tests pass |
| 3 | Angular frontend | All pages navigable; data from live backend; no console errors |
| 4 | Containerisation + Playwright E2E | Full stack in containers; Playwright green on Chromium + Firefox |
| 5 | Jenkins pipeline | All 7 stages green; Playwright report published as artefact |
| 6 | Observability + JMeter baseline | Grafana live under load; JMeter HTML report with p95 numbers |
| 7 | Gatling + k6 + performance optimisation | Before/after benchmark with profiling evidence; results table filled |

---

## 6. Backend Engineering Deep-Dive

> Full conventions, code samples, and JPA patterns are in [`docs/backend-java.md`](docs/backend-java.md). The sections below cover the key architectural decisions.

### 6.1 Spring Boot WAR on WildFly

When Spring Boot runs inside an external servlet container, `SpringBootServletInitializer.onStartup(ServletContext)` is the entry point instead of `main()`. WildFly calls this via the standard `ServletContainerInitializer` mechanism. WildFly provides: Undertow (servlet container), the JVM process, JNDI, and optionally JTA. Spring Boot provides: its `ApplicationContext`, all auto-configuration, and every Spring bean.

The classloading risk: WildFly bundles its own Jackson, Hibernate, and Weld JARs. If the WAR also bundles these (which it does with Spring Boot), WildFly's parent-first classloader will use its own versions and ignore the WAR's. The fix is `jboss-deployment-structure.xml`:

```xml
<jboss-deployment-structure>
  <deployment>
    <exclusions>
      <module name="org.hibernate.validator"/>
      <module name="com.fasterxml.jackson.core.jackson-databind"/>
    </exclusions>
  </deployment>
</jboss-deployment-structure>
```

### 6.2 JPA and Hibernate on MS SQL Server

Hibernate 6 auto-detects `SQLServer2012Dialect` — no manual `spring.jpa.database-platform` needed. Verify by enabling `logging.level.org.hibernate.dialect=DEBUG` on first startup.

Pagination: Hibernate 6 generates `OFFSET ? ROWS FETCH NEXT ? ROWS ONLY` which is correct for SQL Server 2012+. Do not use old `ROW_NUMBER()` approaches seen in legacy codebases.

N+1 problem: the primary risk is in `WardOverviewService`, which joins patients → ehr_records → latest vital_signs → problem count. Loading 20 patients and calling association getters in a loop triggers dozens of queries. Detection: `spring.jpa.properties.hibernate.generate_statistics=true` then check `queries executed` in the log. Fix: native SQL with a window function subquery for latest vitals, or `@EntityGraph`/JOIN FETCH for simpler associations.

NVARCHAR requirement: SQL Server VARCHAR uses the database collation encoding (typically latin-1 or CP1252). NVARCHAR is always Unicode (UTF-16). Swedish characters å, ä, ö in patient names are code points above 127 and will either corrupt or throw an exception if the column is VARCHAR. Use NVARCHAR for all string columns. In Spring/JPA, annotate string fields with `@Column(columnDefinition = "NVARCHAR(100)")`.

### 6.3 RabbitMQ integration

**Spring AMQP vs Spring Kafka**: `spring-boot-starter-amqp` gives `RabbitTemplate` for publishing and `@RabbitListener` for consuming. The programming model is simpler than Kafka: no partition management, no consumer group coordination, no offset tracking. RabbitMQ handles delivery guarantees at the broker level via acknowledgements.

**Exchange and queue declaration**: CosmoLab declares all topology in `RabbitMQConfig.java` as Spring beans. This means the topology is created on application startup if it does not exist — useful in dev. In production, topology would be pre-declared by infrastructure tooling (Terraform/Ansible), and the app would connect to existing exchanges/queues with `declareExchanges=false`.

```java
@Bean
TopicExchange clinicalEventsExchange() {
    return new TopicExchange("clinical.events", true, false);
}

@Bean
DirectExchange auditExchange() {
    return new DirectExchange("audit.log", true, false);
}

@Bean
Queue auditQueue() {
    return QueueBuilder.durable("audit-queue")
        .quorum()       // replicates to majority; survives restarts
        .build();
}

@Bean
Binding auditBinding(Queue auditQueue, DirectExchange auditExchange) {
    return BindingBuilder.bind(auditQueue).to(auditExchange).with("audit");
}
```

**Publisher confirms for audit**: `clinical.events` publishes are fire-and-forget (`rabbitTemplate.convertAndSend()`). For `audit.log`, publisher confirms are enabled: the template is configured with `setConfirmCallback` and the send blocks until the broker ACKs. This guarantees at-least-once delivery for the audit trail without a synchronous round-trip on every clinical event.

**Consumer acknowledgement**: `AuditEventConsumer` uses `@RabbitListener` with `AcknowledgeMode.MANUAL`. It calls `channel.basicAck()` only after a successful DB insert. On exception it calls `channel.basicNack(deliveryTag, false, true)` to requeue. The consumer is idempotent: it checks for an existing `eventId` before inserting to handle redeliveries.

**Why RabbitMQ over Kafka here**: Kafka is the right choice when you need log compaction, event replay from arbitrary offsets, very high throughput (millions/sec), or multiple independent consumer groups reading the full history. CosmoLab has none of these requirements. RabbitMQ is operationally simpler (no Zookeeper, single container, built-in management UI), AMQP is a well-understood enterprise standard, and the routing model (exchanges, bindings, routing keys) maps naturally to the clinical event domain.

### 6.4 Validation and error handling

Request DTOs use Bean Validation: `@NotBlank`, `@Size(max=100)`, `@Pattern(regexp="\\d{8}-\\d{4}")` on `personalNumber`. Controllers annotate `@RequestBody @Valid PatientRequest`.

`GlobalExceptionHandler` catches:
- `MethodArgumentNotValidException` → 400; iterates `ex.getBindingResult().getFieldErrors()` to build the field error map
- `PatientNotFoundException`, `EhrNotFoundException`, `CompositionNotFoundException`, etc. → 404
- `DataIntegrityViolationException` → 409 (duplicate `personalNumber`)
- `Exception` (catch-all) → 500; logs full stack trace; response hides internal detail

All responses use Spring 6's `ProblemDetail.forStatusAndDetail()` — no custom error class needed.

### 6.5 Security scaffolding

`SecurityConfig.java` disables all Spring Security restrictions for CosmoLab:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
               .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
               .build();
}
```

The Angular `AuthGuard` and `ApiInterceptor` are scaffolded correctly — they check for a token and add the header. In production COSMIC this connects to a real identity provider. Framing this correctly in the interview: "I scaffolded the security boundary so the pattern is correct; swapping in a real IDP is a configuration change, not an architecture change."

---

## 7. Performance Engineering

> Full scripts, profiling commands, and the results table are in [`docs/testing-performance.md`](docs/testing-performance.md).

### 7.1 Load test results (fill in after Day 7)

All three tools run the same 3-scenario profile: 50 users, 30s ramp, 3 minutes steady state.

| Metric | JMeter | Gatling | k6 |
|---|---|---|---|
| Throughput (req/s) | | | |
| p50 latency (ms) | | | |
| p95 latency (ms) | | | |
| p99 latency (ms) | | | |
| Error rate | | | |
| Tool CPU during test | | | |

**Before/after optimisation delta** (fill after JFR analysis):

| Metric | Before tuning | After tuning | Change |
|---|---|---|---|
| p95 latency (ms) | | | |
| Throughput (req/s) | | | |
| GC pause p99 (ms) | | | |
| Error rate | | | |

### 7.2 Tool comparison summary

| Dimension | JMeter | Gatling | k6 |
|---|---|---|---|
| Script format | XML (GUI-driven) | Scala DSL | JavaScript |
| Protocol support | HTTP, JDBC, JMS, SOAP, FTP | HTTP, WebSocket, gRPC | HTTP, WebSocket, gRPC |
| Report quality | Requires plugins for HTML; GUI only by default | Built-in HTML report; clean and detailed | CLI summary; Grafana integration via remote write |
| CI integration | Maven plugin; XML diffs in git are unpleasant | Maven/sbt plugin; Scala is code-reviewable | CLI binary; JS is the cleanest for git |
| Resource usage | High — JVM, thread-per-user model | Medium — async Akka runtime | Low — Go runtime, coroutines |
| Learning curve | Medium (GUI hides complexity; XML is painful to hand-edit) | Medium (Scala syntax; DSL is logical once learned) | Low (JS; most engineers know it) |
| Best for | Broad protocol testing; teams that prefer GUI | Performance regression in CI; readable test code | High concurrency; Grafana-native observability |

### 7.3 JVM profiling workflow

1. Start full compose stack; wait for healthy
2. Warm-up run: Gatling at 25 users/s for 60 seconds (JIT compilation of hot paths; without this, the profile shows JIT overhead rather than application overhead)
3. **Option A — JFR**: `docker exec cosmolab-backend jcmd 1 JFR.start duration=120s filename=/tmp/cosmolab.jfr settings=profile`
4. **Option B — async-profiler** (now available since JMX is enabled): `docker exec cosmolab-backend ./profiler.sh -e cpu -d 120 -f /tmp/flame.html 1` — produces an interactive HTML flame graph directly; faster feedback loop than JFR+JMC for CPU hotspot analysis
5. Run full Gatling load for 120 seconds (overlaps with recording)
6. Wait for JFR to complete; copy out: `docker cp cosmolab-backend:/tmp/cosmolab.jfr ./profiling/`
7. Open in JMC → Method Profiling tab → sort by "Self time" (not total time)
7. Look for:
   - Wide flat bars at the top = hot methods in the application (good findings)
   - `ObjectMapper.writeValueAsBytes` = Jackson serialisation overhead (fix: `@JsonIgnore` unused fields; Jackson streaming if extreme)
   - `AbstractEntityPersister.load` = Hibernate loading overhead (check for N+1)
   - `NioWorker` / `WorkerThread` blocked = Undertow thread pool saturation (increase `task-max-threads`)
   - GC events visible as latency spikes on the Grafana HTTP latency panel

### 7.4 WildFly tuning knobs

**Undertow thread pool** (highest impact):
Default: `io-threads = availableProcessors` (2 on a dual-core Docker host), `task-max-threads = 8 * io-threads` = 16–32. This saturates under any meaningful concurrent load with DB-bound requests. Increase to 200.

**JVM GC flags**:
- `-XX:+UseG1GC` is default in JDK 17 but explicit is better in enterprise configuration
- `-XX:MaxGCPauseMillis=200` sets the G1 pause target
- `-XX:G1HeapRegionSize=16m` appropriate for a 1GB heap with medium-sized objects (Hibernate entity graphs)
- `-Xms512m -Xmx1024m` prevents heap resizing under load (resizing causes STW pauses)

**JMX remote access** (port 9999, exposed in Docker Compose):
```bash
-Dcom.sun.management.jmxremote=true
-Dcom.sun.management.jmxremote.port=9999
-Dcom.sun.management.jmxremote.rmi.port=9999
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
-Djava.rmi.server.hostname=0.0.0.0
```
JMX on port 9999 enables three things:
- **async-profiler attach**: `./profiler.sh -e cpu -d 60 -f /tmp/flame.html <pid>` from outside the container via `docker exec`
- **JConsole / VisualVM**: connect from the host at `localhost:9999` to inspect heap, thread dumps, and MBean values live during a load test
- **WildFly CLI**: `jboss-cli.sh --connect --controller=localhost:9990` uses a separate management port (9990) but both benefit from JMX being open

**Always-on JFR** (production-safe, zero overhead):
```bash
-XX:StartFlightRecording=duration=0,filename=/tmp/cosmolab.jfr,settings=profile,maxsize=256m,dumponexit=true
```
`duration=0` means continuous. `maxsize=256m` rolls over the file. `dumponexit=true` writes the final chunk on JVM shutdown — useful in container environments where the JVM exits on `docker stop`.

### 7.5 Database query analysis

Enable SQL logging for development: `spring.jpa.show-sql=true` and `spring.jpa.properties.hibernate.format_sql=true`. In production this is too noisy — use a slow query threshold instead: `spring.jpa.properties.hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS=200`.

In MSSQL, check query plans:
```sql
SET STATISTICS IO ON
SET STATISTICS TIME ON
SELECT * FROM patients WHERE ward = 'ICU' AND status = 'ACTIVE' ORDER BY last_name OFFSET 0 ROWS FETCH NEXT 20 ROWS ONLY
```
Look for "Table Scan" in the execution plan — this means the `(ward, status)` index is not being used. If the index exists but is not used, it may be because the data volume is too small for SQL Server's query optimiser to prefer it over a table scan. With the seed data volume (20 patients), this is expected and acceptable.

---

## 8. End-to-End Testing with Playwright

### 8.1 Why Playwright

Playwright tests the full system end-to-end: Angular SPA → Nginx → WildFly WAR → MS SQL Server. It catches bugs that no other test layer can catch: CORS misconfig, wrong API base URL in the interceptor, broken Angular routes, missing form validation, incorrect pagination behaviour. Every other test in the project is a component test or unit test — Playwright is the only one that tests the assembled system.

Additional benefit: Playwright captures screenshots and videos on failure. These go into the Jenkins artefact archive and provide unambiguous evidence of what failed — essential for a portfolio project where the audience may not run it locally.

### 8.2 Test structure

Full config, Page Object Model rules, and detailed test scenarios are in [`docs/testing-e2e.md`](docs/testing-e2e.md).

```
testing/e2e/
├── playwright.config.ts           # baseURL, browsers, reporter, screenshot on failure
├── fixtures/
│   └── test-data.ts              # Seed IDs from V5/V6 migrations (fixed — no runtime data creation)
├── pages/                         # Page Object Model — all selectors live here, never in test files
│   ├── ward-overview.page.ts      # navigate(), patientRows(), vitalCell(), clickPatient()
│   ├── patient-list.page.ts       # navigate(), searchInput(), wardFilter(), paginator(), clickRow()
│   ├── patient-detail.page.ts     # getTab(), problemRows(), vitalSparklines(), compositionRows()
│   ├── create-vitals.page.ts      # fillForm(vitals), submit(), expectNewReading()
│   └── add-problem.page.ts        # icd10Input(), severitySelect(), submit(), expectInList()
└── tests/
    ├── ward-overview.spec.ts      # table renders; abnormal vitals show flag badge; row → patient detail
    ├── patient-list.spec.ts       # 20 rows; search; ward filter; paginator; status chip
    ├── patient-detail.spec.ts     # all 4 tabs; vitals sparkline; ICD-10 codes; compositions
    ├── create-vitals.spec.ts      # form opens; POST sent; new reading appears; abnormal flag shown
    ├── add-problem.spec.ts        # ICD-10 entry; appears in list; empty ICD-10 blocked
    └── navigation.spec.ts         # direct URL entry; invalid UUID error state; /admin links
```

Selectors use `data-testid` attributes — never CSS classes or text content. See [`docs/frontend-angular.md`](docs/frontend-angular.md) for the full `data-testid` attribute inventory.

### 8.3 Test scenarios (summary)

**Ward overview** — table renders with ≥1 patient; abnormal vital cells show flag badge; clicking a row navigates to `/patients/:id`.

**Patient list** — 20 rows by default; search by last name (debounced); ward dropdown filter; paginator; status chip toggle.

**Patient detail** — all 4 tabs (Overview, Vitals, Problems, Compositions) present and clickable; vitals sparkline colour-coded; ICD-10 codes and severity badges shown.

**Create vitals** — form opens; submitting sends POST (intercepted); new reading appears without full page reload; abnormal value triggers flag badge.

**Add problem** — ICD-10 + displayName + severity creates entry; new problem appears in list; empty ICD-10 shows validation error and blocks submit.

**Navigation** — `/patients`, `/ward/ICU`, `/patients/:id` with known seed ID render correctly; `/patients/invalid-uuid` shows error state; `/admin` shows links to Grafana, RabbitMQ UI, Actuator; nav active state updates on route change.

### 8.4 CI integration

In the Jenkinsfile E2E stage, `wait-for-health.sh` polls `/api/actuator/health` until it returns HTTP 200 or times out after 60 seconds. Without this gate, Playwright starts before Spring Boot finishes deploying to WildFly and every test fails with a connection error.

```bash
#!/bin/bash
# scripts/wait-for-health.sh <url> <timeout_seconds>
URL=$1; TIMEOUT=$2; ELAPSED=0
until curl -sf "$URL" > /dev/null 2>&1; do
  sleep 2; ELAPSED=$((ELAPSED + 2))
  [ $ELAPSED -ge $TIMEOUT ] && echo "Timed out waiting for $URL" && exit 1
done
echo "Health check passed: $URL"
```

---

## 9. Repository Structure

> Note: `docker-compose.yml` lives inside `devops/` for consistency.
> Run with `docker-compose -f devops/docker-compose.yml up` from the repo root,
> or add a root-level `Makefile` with a `up` target as a convenience wrapper.

```
cosmolab/
├── CLAUDE.md
├── docs/
│   ├── cosmolab-overview.md            # Project identity, stack, decisions (condensed)
│   ├── cosmolab-architecture.md        # Container diagram, full package tree, REST API, RabbitMQ topology
│   ├── clinical-domain.md              # openEHR entities, interview talking points
│   ├── backend-java.md                 # Spring Boot WAR, JPA, RabbitMQ, validation conventions
│   ├── frontend-angular.md             # NgModules, routing, auth, component conventions, data-testid
│   ├── devops-infra.md                 # Docker Compose, WildFly, JMX, JFR, MSSQL, Nginx
│   ├── sprint-plan.md                  # Sprint-by-sprint tasks and exit criteria
│   ├── testing-e2e.md                  # Playwright config, Page Object Model, test scenarios
│   └── testing-performance.md          # JMeter, Gatling, k6, profiling workflow, results table
├── cosmolab-backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/cosmolab/      # See §3.3
│       │   ├── resources/
│       │   │   ├── application.yml
│       │   │   └── db/migration/
│       │   │       ├── V1__init_schema.sql
│       │   │       └── V2__seed_data.sql
│       │   └── webapp/WEB-INF/
│       │       └── jboss-deployment-structure.xml
│       └── test/java/com/cosmolab/
│
├── cosmolab-frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   ├── angular.json
│   └── src/app/                        # See §3.4
│
├── testing/
│   ├── e2e/                            # System-level E2E (Playwright)
│   │   ├── playwright.config.ts
│   │   ├── fixtures/test-data.ts
│   │   ├── pages/
│   │   └── tests/
│   └── performance/                    # Load and performance tests
│       ├── jmeter/
│       │   └── cosmolab-baseline.jmx
│       ├── gatling/
│       │   ├── pom.xml
│       │   └── src/test/scala/cosmolab/
│       │       └── CosmoLabSimulation.scala
│       └── k6/
│           └── patient-load.js
│
└── devops/
    ├── docker-compose.yml              # Full stack
    ├── docker-compose.dev.yml          # Dev overrides (source mounts, debug port 5005)
    ├── scripts/
    │   └── wait-for-health.sh
    ├── jenkins/
    │   ├── Dockerfile
    │   └── Jenkinsfile
    └── observability/
        ├── prometheus.yml
        ├── loki/
        │   └── loki-config.yml
        └── grafana/
            ├── datasources/
            │   └── datasources.yml
            └── dashboards/
                ├── cosmolab-jvm.json
                ├── cosmolab-http.json
                └── cosmolab-rabbitmq.json
```

---

## 10. Decisions (closed)

| # | Decision | Chosen |
|---|---|---|
| 1 | Angular UI library | **Angular Material** — enterprise-common; Material table handles pagination natively |
| 2 | Auth strategy | **Deferred** — implement later; see §10.1 for mock JWT explanation |
| 3 | Gatling runner | **Maven plugin** — `mvn gatling:test` integrates naturally with Jenkinsfile |
| 4 | k6 output | **Prometheus remote write** — k6 metrics flow into Grafana alongside app and JVM metrics |
| 5 | WildFly JMX | **JMX enabled** — port 9999 exposed; enables async-profiler attach and JConsole inspection |
| 6 | Playwright browsers | **Chromium + Firefox** — cross-browser coverage; worth the extra ~2 minutes in CI |
| 7 | Playwright location | **`testing/e2e/`** — under `testing/` alongside `testing/performance/`; all test concerns in one place |

### 10.1 Mock JWT — what it is and why it's sufficient for now

A real authentication flow requires an identity provider (Keycloak, Azure AD, etc.), a login page, token issuance, refresh logic, and Spring Security validation on the backend. That is a full feature in itself and is not the focus of CosmoLab.

Mock JWT is a pattern where a hardcoded, pre-signed JWT is stored in `sessionStorage` on app load. The Angular `ApiInterceptor` attaches it as `Authorization: Bearer <token>` on every outgoing HTTP request. The `AuthGuard` checks that the token exists before allowing navigation. Spring Security on the backend is scaffolded but configured to `permitAll()` — it does not validate the token.

What this gives you:
- The Angular security pattern is correct and complete (guard, interceptor, sessionStorage)
- All HTTP calls carry the auth header — exactly as they would in production
- Adding a real IDP later is a configuration change to Spring Security + one line in `AuthService.login()` — the rest of the code does not change

What it does not give you:
- Actual token validation (any string in the header would pass)
- Role-based access control
- Token expiry or refresh

When asked in the interview: "We scaffolded the full auth boundary — guard, interceptor, header propagation. The backend is configured to accept any authenticated request for now. In production this connects to the existing Cambio IDP; the application code doesn't change, only the Spring Security configuration."

---

*Last updated: Day 0 — full outline. Fill §7.1 performance tables after Day 7. Update §1.3 evidence column as each phase completes.*

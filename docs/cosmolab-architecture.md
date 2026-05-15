---
description: CosmoLab system architecture — container diagram, backend package structure, domain schema, REST API surface, RabbitMQ topology
alwaysApply: true
---

# CosmoLab — Architecture

## Container Communication

```
── DOMAIN TRAFFIC ──────────────────────────────────────────
Browser → cosmolab-frontend (Nginx :80)
            → /api proxy → cosmolab-backend (WildFly :8080)
                              → sqlserver (MSSQL :1433)
                              → rabbitmq  (AMQP :5672)

── OBSERVABILITY (never routes through RabbitMQ) ───────────
cosmolab-backend → /actuator/prometheus ← prometheus (:9090)
cosmolab-backend → OTLP :4317          → tempo (:3200)
cosmolab-backend → Docker log driver   → loki (:3100)
prometheus, loki, tempo → grafana (:3000)

── CI / E2E ────────────────────────────────────────────────
Jenkins (:8090) → docker build → docker-compose deploy
Playwright      → http://localhost:80
```

## Backend Package Structure (DDD-lite)

```
com.cosmolab
├── config/
│   ├── WebConfig.java              # CORS, MVC
│   ├── RabbitMQConfig.java         # exchanges, queues, bindings
│   ├── ObservabilityConfig.java    # Micrometer tracing + OTLP
│   └── SecurityConfig.java         # disabled; permitAll() scaffolded
├── domain/                         # zero framework dependencies
│   ├── ehr/         EhrRecord, EhrRepository (port)
│   ├── composition/ Composition, CompositionType, CompositionStatus, repository
│   ├── observation/ VitalSigns, repository
│   ├── evaluation/  ProblemListEntry, ProblemStatus, Severity, repository
│   └── patient/     Patient, PatientStatus, Gender, repository
├── application/                    # use-case orchestration
│   ├── ehr/         EhrService
│   ├── composition/ CompositionService
│   ├── observation/ VitalSignsService
│   ├── evaluation/  ProblemListService
│   └── ward/        WardOverviewService (aggregation — no new entity)
├── infrastructure/
│   ├── persistence/ JPA implementations of domain repository ports
│   └── messaging/   ClinicalEvent record, ClinicalEventPublisher, AuditEventConsumer
└── interfaces/rest/
    ├── EhrController, CompositionController, VitalSignsController
    ├── ProblemListController, WardOverviewController
    ├── GlobalExceptionHandler   # RFC 7807 ProblemDetail for all errors
    └── dto/                     # Request/Response DTOs + PagedResponse<T>
```

## Domain Schema (MS SQL Server)

```sql
patients            (id UUID PK, first_name, last_name, personal_number UNIQUE,
                     date_of_birth, gender, ward, status, created_at, updated_at)
                    INDEX: UNIQUE(personal_number), NONCLUSTERED(ward, status)

ehr_records         (ehr_id UUID PK, subject_id FK→patients, system_id,
                     created_at, status)

compositions        (id UUID PK, ehr_id FK→ehr_records, composition_type,
                     author_id, start_time, commit_time, facility_name, status)

vital_signs         (id UUID PK, composition_id FK→compositions, recorded_at,
                     recorded_by, systolic_bp, diastolic_bp, heart_rate,
                     respiratory_rate, temperature DECIMAL(4,1),
                     oxygen_saturation DECIMAL(5,2), weight DECIMAL(5,2))
                    INDEX: NONCLUSTERED(composition_id, recorded_at DESC)

problem_list_entries (id UUID PK, composition_id FK→compositions,
                      ehr_id FK→ehr_records,    ← direct FK for efficient queries
                      icd10_code, display_name, severity, status,
                      onset_date, resolved_date, recorded_at, recorded_by)
```

All text columns use `NVARCHAR` — SQL Server VARCHAR corrupts Swedish characters (å, ä, ö).

## REST API Surface

```
# Demographics
GET|POST          /api/v1/patients
GET|PUT|DELETE    /api/v1/patients/{id}

# EHR
POST              /api/v1/ehr
GET               /api/v1/ehr/{ehrId}
GET               /api/v1/ehr/subject/{patientId}

# Compositions
POST              /api/v1/ehr/{ehrId}/compositions
GET               /api/v1/ehr/{ehrId}/compositions     ?type=&page=&size=
GET|PUT           /api/v1/ehr/{ehrId}/compositions/{id}

# Vital Signs
POST              /api/v1/ehr/{ehrId}/compositions/{cid}/vitals
GET               /api/v1/ehr/{ehrId}/vitals            ?from=&to=
GET               /api/v1/ehr/{ehrId}/vitals/latest

# Problem List
POST              /api/v1/ehr/{ehrId}/problems
GET               /api/v1/ehr/{ehrId}/problems          ?status=ACTIVE
GET|PUT           /api/v1/ehr/{ehrId}/problems/{id}

# Ward Overview (aggregation — primary load test target)
GET               /api/v1/ward/{wardId}/overview

# Actuator
GET               /actuator/health
GET               /actuator/prometheus
GET               /actuator/info
```

All errors: RFC 7807 `ProblemDetail`. Validation errors include `fieldErrors` map in properties.

## RabbitMQ Topology

```
Exchange: clinical.events   (topic, durable)
  patient.created / patient.updated / patient.discharged
  note.created / note.updated
  → no consumer in CosmoLab; ready for future services

Exchange: audit.log         (direct, durable)
  routing key: audit
  → Queue: audit-queue (durable, quorum)
       → AuditEventConsumer @RabbitListener (manual ack, idempotent)
```

RabbitMQ Prometheus plugin exposes metrics on port 15692 (scraped independently by Prometheus).

## Flyway Migration Sequence

```
V1__patients_and_ehr.sql     patients + ehr_records + indexes
V2__compositions.sql          compositions
V3__vital_signs.sql           vital_signs
V4__problem_list.sql          problem_list_entries
V5__seed_patients.sql         20 patients + EHR records (ICU, Cardiology, Neurology)
V6__seed_clinical_data.sql    compositions + vitals + problems per patient
```

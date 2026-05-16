---
description: CosmoLab project identity, technology stack, decisions, and repository structure
alwaysApply: true
---

# CosmoLab — Project Overview

CosmoLab is a clinical information system simulating Cambio COSMIC's technology stack.
Built for: (1) Cambio Platform Performance Engineer interview preparation, (2) portfolio evidence.

## Purpose
- Mirror the exact stack Cambio COSMIC runs: Spring Boot WAR on WildFly, MS SQL Server, RabbitMQ
- Clinical domain: patient management, EHR records, compositions, vital signs, problem list, ward overview
- openEHR-aligned domain model — see `docs/FEATURES.md` for full feature detail
- Full engineering cycle: backend → frontend → containers → CI/CD → observability → load testing → optimisation

## Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Frontend | Angular (standalone) | 19 |
| Backend | Spring Boot WAR | 3.2 / JDK 17 |
| App server | WildFly | 30 |
| Database | MS SQL Server | 2022 |
| Message broker | RabbitMQ | 3.12-management |
| Schema migrations | Flyway | 9.x |
| CI/CD | Jenkins | LTS |
| Observability | LGTM (Loki, Grafana, Tempo, Prometheus) | latest |
| Load testing | JMeter + Gatling + k6 | latest |
| Profiling | JFR + async-profiler | JDK built-in |
| E2E testing | Playwright | 1.x |
| Containerisation | Docker + Compose | latest |

## Closed Decisions

| # | Decision | Chosen |
|---|---|---|
| 1 | Angular UI library | Angular Material |
| 2 | Auth strategy | Deferred — mock JWT scaffolded; `permitAll()` on backend |
| 3 | Gatling runner | Maven plugin (`mvn gatling:test`) |
| 4 | k6 output | Prometheus remote write → Grafana |
| 5 | WildFly JMX | Enabled on port 9999 (async-profiler + JConsole) |
| 6 | Playwright browsers | Chromium + Firefox |
| 7 | Playwright location | `testing/e2e/` |

## Repository Structure

```
cosmolab/
├── CLAUDE.md                        # Architecture, stack, deep-dives
├── docs/
│   ├── cosmolab-overview.md         # Project identity, stack, decisions (this file)
│   ├── cosmolab-architecture.md     # Container diagram, full package tree, REST API, RabbitMQ topology
│   ├── clinical-domain.md           # openEHR entities, interview talking points
│   ├── backend-java.md              # Spring Boot WAR, JPA, RabbitMQ, validation conventions
│   ├── frontend-angular.md          # NgModules, routing, auth, component conventions, data-testid
│   ├── devops-infra.md              # Docker Compose, WildFly, JMX, JFR, MSSQL, Nginx
│   ├── sprint-plan.md               # Sprint-by-sprint tasks and exit criteria
│   ├── testing-e2e.md               # Playwright config, Page Object Model, test scenarios
│   └── testing-performance.md       # JMeter, Gatling, k6, profiling workflow, results table
├── cosmolab-backend/                # Spring Boot WAR
├── cosmolab-frontend/               # Angular 17 SPA
├── testing/
│   ├── e2e/                         # Playwright (Chromium + Firefox)
│   └── performance/                 # JMeter + Gatling + k6
└── devops/
    ├── docker-compose.yml
    ├── docker-compose.dev.yml
    ├── scripts/wait-for-health.sh
    ├── jenkins/
    └── observability/               # Prometheus, Loki, Grafana, Tempo
```

## Messaging vs Observability — strict separation

RabbitMQ carries **domain events only** (`clinical.events`, `audit.log`).
Metrics: Micrometer → `/actuator/prometheus` → Prometheus. Never through RabbitMQ.
Logs: Logback → Docker log driver → Loki. Never through RabbitMQ.
Traces: OpenTelemetry → OTLP → Tempo. Never through RabbitMQ.

## Deferred / Future

- Real auth (Spring Security + IDP) — post-sprint 7
- WildFly 2-node cluster (`standalone-ha.xml`) — optional sprint 8
- CareInstruction archetype (INSTRUCTION) — optional sprint 8
- FHIR R4 API layer — phase 2

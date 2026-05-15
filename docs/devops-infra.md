---
description: DevOps and infrastructure — Docker Compose services, WildFly tuning, JMX, JFR, MSSQL, RabbitMQ container config
globs: devops/**,cosmolab-backend/Dockerfile,cosmolab-frontend/Dockerfile
alwaysApply: false
---

# DevOps and Infrastructure

## Docker Compose Services

| Service | Image | Ports | Notes |
|---|---|---|---|
| `sqlserver` | `mcr.microsoft.com/mssql/server:2022-latest` | 1433 | `ACCEPT_EULA=Y`, `MSSQL_PID=Developer` |
| `rabbitmq` | `rabbitmq:3.12-management` | 5672, 15672, 15692 | management UI + Prometheus plugin |
| `cosmolab-backend` | custom (WildFly 30) | 8080, 9990, 9999 | depends_on: sqlserver + rabbitmq healthy |
| `cosmolab-frontend` | custom (Nginx) | 80 | proxies /api to backend |
| `prometheus` | `prom/prometheus` | 9090 | scrapes actuator + rabbitmq |
| `loki` | `grafana/loki` | 3100 | receives Docker log driver output |
| `tempo` | `grafana/tempo` | 3200, 4317 | OTLP ingest on 4317 |
| `grafana` | `grafana/grafana` | 3000 | datasources auto-provisioned |

## Critical: Healthcheck Ordering

MSSQL takes 15–25 seconds to start. Backend **must** use `condition: service_healthy`.
Without this, Flyway fails with connection refused on startup.

```yaml
sqlserver:
  healthcheck:
    test: ["CMD", "bash", "-c", "echo > /dev/tcp/localhost/1433"]
    interval: 5s
    timeout: 3s
    retries: 10
    start_period: 20s

rabbitmq:
  healthcheck:
    test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
    interval: 5s
    timeout: 3s
    retries: 10

cosmolab-backend:
  depends_on:
    sqlserver:  { condition: service_healthy }
    rabbitmq:   { condition: service_healthy }
```

## MSSQL Server Container

Required environment variables:
- `ACCEPT_EULA=Y` — mandatory; container exits without it
- `MSSQL_SA_PASSWORD` — must meet complexity policy (8+ chars, upper+lower+digit+symbol). Use `CosmoLab@2024` in dev.
- `MSSQL_PID=Developer` — full developer edition, free

JDBC URL for dev (inside Docker network):
```
jdbc:sqlserver://sqlserver:1433;databaseName=cosmolab;encrypt=false;trustServerCertificate=true
```
`encrypt=false;trustServerCertificate=true` avoids TLS certificate errors in the dev container setup.

## WildFly Dockerfile (multi-stage)

```dockerfile
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM quay.io/wildfly/wildfly:30.0.Final
COPY --from=build /app/target/*.war /opt/jboss/wildfly/standalone/deployments/
ENV JAVA_OPTS="-Xms512m -Xmx1024m \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=16m \
  -XX:+FlightRecorder \
  -XX:StartFlightRecording=duration=0,filename=/tmp/cosmolab.jfr,settings=profile,maxsize=256m,dumponexit=true \
  -Dcom.sun.management.jmxremote=true \
  -Dcom.sun.management.jmxremote.port=9999 \
  -Dcom.sun.management.jmxremote.rmi.port=9999 \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false \
  -Djava.rmi.server.hostname=0.0.0.0"
```

## WildFly standalone.xml Tuning

Key change from default — Undertow thread pool:
```xml
<subsystem xmlns="urn:jboss:domain:undertow:12.0">
  <worker name="default" io-threads="4" task-max-threads="200"/>
</subsystem>
```
Default `task-max-threads` is `8 × io-threads` = 32. Under 50 concurrent users with DB latency,
this saturates immediately. Increasing to 200 is the highest-impact single tuning change.

## JMX — What it enables

Port 9999 exposed in Docker Compose enables:
- **async-profiler attach**: `docker exec cosmolab-backend ./profiler.sh -e cpu -d 60 -f /tmp/flame.html 1`
- **JConsole / VisualVM**: connect from host at `localhost:9999` during load tests
- **jcmd**: `docker exec cosmolab-backend jcmd 1 JFR.start duration=120s filename=/tmp/cosmolab.jfr settings=profile`

## Always-on JFR

`duration=0` = continuous recording. `maxsize=256m` = rolls over. `dumponexit=true` = writes on `docker stop`.
This is the production-safe profiling pattern — zero overhead, always-available recording.

## Nginx Config (frontend)

```nginx
server {
  listen 80;
  root /usr/share/nginx/html;

  location /api {
    proxy_pass http://cosmolab-backend:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
  }

  location / {
    try_files $uri $uri/ /index.html;
  }
}
```

## Observability — Directory Structure

```
devops/observability/
├── prometheus.yml            # scrape: cosmolab-backend:8080/actuator/prometheus (15s)
│                             #         rabbitmq:15692/metrics (15s) — independent scrape
├── loki/loki-config.yml      # filesystem storage; retention 168h
└── grafana/
    ├── datasources/datasources.yml    # auto-provision Prometheus + Loki + Tempo
    └── dashboards/
        ├── cosmolab-jvm.json          # heap, GC pauses, threads, CPU
        ├── cosmolab-http.json         # request rate, error rate, p50/p95/p99
        └── cosmolab-rabbitmq.json     # publish rate, queue depth, consumer ack rate
```

RabbitMQ Prometheus plugin (port 15692) is scraped independently — not from the application.
The application's Micrometer pipeline remains independent of RabbitMQ status.

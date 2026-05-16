# CosmoLab — JBoss/WildFly Deployment

> How the Spring Boot WAR is packaged and deployed into WildFly/JBoss EAP, what classloading conflicts were encountered, how they were resolved, and where the deployment could be improved.

---

## 1. Why WAR on WildFly, Not an Embedded JAR

Spring Boot defaults to a fat JAR with an embedded Tomcat. CosmoLab deliberately uses the opposite: a WAR deployed into WildFly (the open-source upstream of JBoss EAP). This mirrors the COSMIC production stack, where JBoss/WildFly acts as the servlet container and provides the thread pool, HTTP listener, and JNDI.

Practical consequences of this choice:

| Concern | Embedded Tomcat (default) | WildFly WAR (CosmoLab) |
|---|---|---|
| Entry point | `main()` method | `SpringBootServletInitializer.onStartup()` |
| Server lifecycle | Spring Boot owns it | WildFly owns it |
| Thread pool tuning | `server.tomcat.*` properties | WildFly `standalone.xml` Undertow subsystem |
| JNDI datasources | Not applicable | Available via WildFly naming |
| Classloading | Flat Spring Boot classpath | WildFly parent-first with module system |
| JDBC driver | Bundled in fat JAR | Must be bundled in WAR (no WildFly module) |

---

## 2. WAR Packaging Configuration

### 2.1 `pom.xml` — `provided` scope for Tomcat

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-tomcat</artifactId>
    <scope>provided</scope>
</dependency>
```

`provided` tells Maven to include Tomcat on the compile classpath but exclude it from the WAR. Without this, both the embedded Tomcat JARs and WildFly's Undertow would be in the classpath simultaneously, causing servlet container conflicts.

### 2.2 `SpringBootServletInitializer`

```java
@SpringBootApplication
public class CosmoLabApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(CosmoLabApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(CosmoLabApplication.class);
    }
}
```

`configure()` is the hook WildFly calls via the `ServletContainerInitializer` SPI. Without extending `SpringBootServletInitializer`, WildFly deploys the WAR but Spring never boots.

### 2.3 Context root — `jboss-web.xml`

```xml
<jboss-web>
    <context-root>/</context-root>
</jboss-web>
```

Without this file, WildFly derives the context root from the WAR artifact name (`cosmolab-backend-1.0.0-SNAPSHOT`), mounting the app at `/cosmolab-backend-1.0.0-SNAPSHOT`. All API paths, CORS origins, and Nginx proxy rules would break. The explicit `/` root keeps paths predictable across versions.

### 2.4 CDI bean archive — `beans.xml`

```xml
<beans version="4.0" bean-discovery-mode="none">
</beans>
```

`bean-discovery-mode="none"` prevents WildFly's CDI (Weld) engine from scanning the WAR for CDI beans. Spring manages its own IoC container; Weld scanning the same classpath would cause duplicate bean definitions and startup failures.

---

## 3. Classloading Conflicts and Resolutions

WildFly uses a hierarchical module classloader. Its modules — Hibernate Validator, SLF4J, Jackson — are loaded parent-first and would shadow the versions bundled inside the WAR. Each conflict below was encountered during development and fixed in `jboss-deployment-structure.xml`.

### 3.1 Current state of `jboss-deployment-structure.xml`

```xml
<jboss-deployment-structure>
    <deployment>
        <exclude-subsystems>
            <subsystem name="weld"/>
            <subsystem name="batch-jberet"/>
            <subsystem name="jsf"/>
        </exclude-subsystems>
        <exclusions>
            <module name="org.hibernate.validator"/>
            <module name="org.slf4j"/>
            <module name="org.slf4j.impl"/>
        </exclusions>
    </deployment>
</jboss-deployment-structure>
```

### 3.2 Subsystem exclusions

| Subsystem | Why excluded |
|---|---|
| `weld` | Spring Boot uses its own IoC. Weld (CDI) scanning the WAR classpath causes duplicate bean registration and startup failure. |
| `batch-jberet` | JBoss Batch depends on a CDI `BeanManager`. With Weld excluded, `batch-jberet` fails to initialise on deployment — it must be excluded alongside `weld`. |
| `jsf` | JBoss EAP 8 auto-registers `com.sun.faces.config.ConfigureListener` for any WAR it considers a JSF application. This listener requires CDI. With `weld` excluded, JSF activation causes `IllegalStateException: CDI not available`. |

### 3.3 Module exclusions

| Module | Why excluded |
|---|---|
| `org.hibernate.validator` | WildFly bundles Hibernate Validator 8.x. Spring Boot 3.5 bundles its own compatible version. WildFly's copy would shadow the WAR's copy, causing version mismatches in constraint annotations. |
| `org.slf4j` | WildFly ships `slf4j-api`. Spring Boot bundles a potentially different version. Without exclusion, multiple SLF4J binding warnings appear and logging configuration is unreliable. |
| `org.slf4j.impl` | WildFly's SLF4J implementation module is `slf4j-jboss-logmanager`, which routes logs to JBoss LogManager. Spring Boot expects to bind Logback as the SLF4J implementation. Both being on the classpath causes `SLF4J: Class path contains multiple SLF4J bindings` and one of them silently wins — typically WildFly's, which discards the Spring `logback-spring.xml` configuration. |

---

## 4. Environment Configuration

### 4.1 Docker Compose (container deployment)

Environment variables are passed in `devops/docker-compose.yml` and consumed via Spring property placeholders:

```yaml
environment:
  MSSQL_HOST: sqlserver       # Docker Compose service name
  MSSQL_USER: sa
  MSSQL_PASSWORD: CosmoLab@2024
  RABBITMQ_HOST: rabbitmq
  RABBITMQ_USER: guest
  RABBITMQ_PASSWORD: guest
```

`application.yml` resolves these with defaults:
```yaml
spring:
  datasource:
    url: jdbc:sqlserver://${MSSQL_HOST:sqlserver}:1433;databaseName=cosmolab;...
  rabbitmq:
    host: ${RABBITMQ_HOST:rabbitmq}
```

The defaults (`sqlserver`, `rabbitmq`) match the Docker Compose service names and are wrong for local development.

### 4.2 Local development profile

`application-dev.yml` overrides the Docker hostnames with `localhost` for running the backend directly against a locally exposed SQL Server and RabbitMQ container:

```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=cosmolab;encrypt=false;trustServerCertificate=true
  rabbitmq:
    host: localhost
```

Activate with `-Dspring.profiles.active=dev` in the WildFly run configuration, or `SPRING_PROFILES_ACTIVE=dev` as an environment variable.

### 4.3 Database prerequisite

WildFly (and Flyway) expect the `cosmolab` database to already exist. SQL Server does not auto-create databases from JDBC URLs. Before the first deployment:

```bash
docker exec -it cosmolab-sqlserver \
  /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U sa -P 'CosmoLab@2024' \
  -No -C -Q "CREATE DATABASE cosmolab"
```

---

## 5. Deployment Sequence

```
1. docker-compose up sqlserver rabbitmq
   └── wait for sqlserver healthcheck (TCP :1433) — takes 15–25 s

2. mvn -pl backend package -DskipTests
   └── produces backend/target/cosmolab-backend-1.0.0-SNAPSHOT.war

3. WildFly picks up the WAR (auto-deploy or CLI deploy)
   └── WildFly calls SpringBootServletInitializer.onStartup()
   └── Spring ApplicationContext starts
   └── Flyway runs migrations V1–V6
   └── RabbitMQ topology declared (exchanges, queues, bindings)
   └── Actuator /health endpoint becomes UP

4. Nginx frontend proxies /api/* → WildFly :8080
```

---

## 6. Known Limitations and Improvement Opportunities

### 6.1 `encrypt=false` on the JDBC URL

```
jdbc:sqlserver://...;encrypt=false;trustServerCertificate=true
```

TLS is disabled between the application and SQL Server. This is acceptable for a single-host Docker Compose development setup where all traffic is on `cosmolab-net` (a Docker bridge network with no external exposure). In any multi-host or production deployment, `encrypt=true` with a real certificate is mandatory.

**Improvement**: Generate a self-signed certificate for the MSSQL container and set `encrypt=true;trustServerCertificate=false` with the certificate mounted as a secret. In production, use a CA-signed certificate and certificate rotation via Kubernetes Secrets or HashiCorp Vault.

---

### 6.2 Credentials in `docker-compose.yml`

`MSSQL_PASSWORD` and `RABBITMQ_PASSWORD` are hardcoded plaintext in `devops/docker-compose.yml`. This is acceptable for a local portfolio project but is a disqualifying pattern in any shared or production codebase.

**Improvement options**:
- Docker Secrets (`docker secret create`) for Swarm deployments
- A `.env` file (committed as `.env.example`, gitignored for the real values) with `docker-compose --env-file`
- HashiCorp Vault or AWS Secrets Manager with a sidecar injector for Kubernetes
- At minimum: move credentials to a gitignored `docker-compose.override.yml`

---

### 6.3 No WildFly datasource JNDI binding

CosmoLab uses a Spring-managed HikariCP datasource (`application.yml`). WildFly is not aware of the datasource at all. This is simpler to configure but misses two enterprise capabilities:

- **WildFly connection pool tuning** via `standalone.xml` — pool min/max, validation query, timeout — without redeployment
- **XA transactions** across datasource + messaging — WildFly's JTA can coordinate a distributed transaction that spans a SQL Server insert and a RabbitMQ publish. Spring AMQP + Hibernate without JTA gives at-least-once delivery via `basicNack`/retry, not true atomicity.

**Improvement**: Declare the datasource in `standalone.xml` as a WildFly managed datasource, reference it from `application.yml` as a JNDI name (`java:jboss/datasources/cosmolab`), and use `spring.datasource.jndi-name` instead of `url`. This enables live pool reconfiguration via WildFly CLI without redeployment.

---

### 6.4 Undertow thread pool is at defaults

WildFly's default `task-max-threads` is `8 × io-threads`. On a 2-vCPU Docker host, that is 16 worker threads. Under any meaningful concurrent load (> 16 users with > 1 ms DB latency), all threads saturate and HTTP requests queue in Undertow's acceptor. The effect shows as a sharp p95 latency spike under load with no application-level bottleneck.

**Improvement**: Set explicitly in `standalone.xml`:

```xml
<subsystem xmlns="urn:jboss:domain:undertow:12.0">
  <worker name="default" io-threads="4" task-max-threads="200"/>
</subsystem>
```

`io-threads` controls NIO event loop threads (2–4 is correct for a 2–4 vCPU host). `task-max-threads` controls the blocking work queue for request handlers. 200 allows 200 concurrent database-bound requests before backpressure kicks in. This is documented as Day 7 tuning in `docs/testing-performance.md`.

---

### 6.5 Always-on JFR not configured in Docker image

The WildFly `standalone.conf` JVM flags are documented in `CLAUDE.md` but not verified as present in the Docker image. Without `StartFlightRecording`, profiling requires manually issuing `jcmd` during a load test, which is error-prone.

**Improvement**: Confirm `standalone.conf` in the WildFly Docker image contains:
```bash
-XX:StartFlightRecording=duration=0,filename=/tmp/cosmolab.jfr,settings=profile,maxsize=256m,dumponexit=true
```

Add a Dockerfile `COPY` step to ensure this configuration is version-controlled:
```dockerfile
COPY standalone.conf $WILDFLY_HOME/bin/standalone.conf
```

---

### 6.6 No graceful shutdown configuration

WildFly supports graceful shutdown (`/subsystem=undertow:write-attribute(name=statistics-enabled,value=true)` + request drain). Without it, a `docker stop` sends SIGTERM to WildFly which immediately closes active HTTP connections, causing in-flight requests to fail with connection reset. For a load test target, this makes the post-test cool-down phase unreliable.

**Improvement**: Configure graceful shutdown timeout in `standalone.xml`:
```xml
<server name="default-server">
    <http-listener name="default" ... disallowed-methods="TRACE"/>
</server>
```
And in WildFly CLI: `shutdown --timeout=30` allows 30 seconds for in-flight requests to drain. For Docker, set `stop_grace_period: 35s` in `docker-compose.yml` to give the JVM time to finish the shutdown sequence before Docker force-kills it.

---

### 6.7 WAR version baked into the filename

The WAR is named `cosmolab-backend-1.0.0-SNAPSHOT.war`. WildFly derives the deployment name from the filename. If WildFly is configured for auto-deploy (watching a `deployments/` directory), a new deployment drops a new WAR file and WildFly hot-deploys it — but the old WAR name stays on disk and may be redeployed on restart.

**Improvement**: Use a fixed, versionless WAR name in `pom.xml`:
```xml
<build>
    <finalName>cosmolab-backend</finalName>
</build>
```

This makes deployments idempotent — dropping `cosmolab-backend.war` always replaces the previous deployment regardless of version.

---

## 7. Quick Reference — Deployment Checklist

| Step | Command / Action |
|---|---|
| Start dependencies | `docker-compose -f devops/docker-compose.yml up sqlserver rabbitmq -d` |
| Create database (first run) | `docker exec cosmolab-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'CosmoLab@2024' -No -C -Q "CREATE DATABASE cosmolab"` |
| Build WAR | `mvn -pl backend package -DskipTests` |
| Check deployment | `curl http://localhost:8080/actuator/health` |
| View WildFly log | `docker logs cosmolab-backend -f` |
| Reload without restart | `curl -X POST http://localhost:9990/management` (WildFly management API) |
| Force undeploy | WildFly CLI: `undeploy cosmolab-backend.war` |

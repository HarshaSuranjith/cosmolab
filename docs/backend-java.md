---
description: Backend Java conventions — Spring Boot WAR on WildFly, JPA/MSSQL patterns, RabbitMQ integration, validation, error handling
globs: cosmolab-backend/src/**/*.java
alwaysApply: false
---

# Backend Java Conventions

## Spring Boot WAR on WildFly

`spring-boot-starter-tomcat` must be scope `provided` — WildFly provides Undertow.
Every application needs `CosmoLabServletInitializer extends SpringBootServletInitializer`.
`webapp/WEB-INF/jboss-deployment-structure.xml` must exclude WildFly's bundled Jackson and Hibernate
or WildFly's parent-first classloader will use its own versions and ignore the WAR's.

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

WildFly provides the servlet container (Undertow), JVM, JNDI.
Spring Boot provides ApplicationContext, auto-configuration, all Spring beans.

## DDD-lite Layer Rules

- **`domain/`** — zero framework dependencies. No Spring annotations. No JPA on interfaces.
  Entities are annotated with JPA but contain no Spring imports.
  Repository interfaces are pure ports: `PatientRepository`, `EhrRepository`, etc.
- **`application/`** — orchestrates domain + infrastructure. `@Service`, `@Transactional` allowed.
  No HTTP concerns. No Jackson. No RabbitMQ. Those belong in infrastructure or interfaces.
- **`infrastructure/`** — all adapters. JPA implementations, Kafka/RabbitMQ publishers, consumers.
- **`interfaces/rest/`** — controllers and DTOs only. Never expose domain entities directly.
  All request/response objects are DTOs in `interfaces/rest/dto/`.

## JPA and Hibernate on MS SQL Server

Hibernate 6 auto-detects SQL Server dialect — do not set `spring.jpa.database-platform` manually.

**NVARCHAR is mandatory** for all String columns:
```java
@Column(columnDefinition = "NVARCHAR(100)")
private String firstName;
```
VARCHAR silently corrupts Swedish characters (å, ä, ö) and any Unicode above code point 127.

**`java.time.Instant` → `DATETIMEOFFSET(6)`** — Hibernate 6 changed the MSSQL mapping for `Instant`
from `datetime2` (Hibernate 5) to `datetimeoffset` (`TIMESTAMP_UTC`). Flyway DDL for all `Instant`
columns must use `DATETIMEOFFSET(6)`:
```sql
recorded_at  DATETIMEOFFSET(6)  NOT NULL,
start_time   DATETIMEOFFSET(6)  NOT NULL,
commit_time  DATETIMEOFFSET(6)  NOT NULL,
```
Using `DATETIME2` causes a `SchemaManagementException` at startup with `ddl-auto: validate`.
`LocalDateTime` fields stay as `DATETIME2` — only `Instant` is affected.

**Pagination**: Hibernate 6 generates `OFFSET ? ROWS FETCH NEXT ? ROWS ONLY` — correct for SQL Server 2012+.

**N+1 detection**: enable `spring.jpa.properties.hibernate.generate_statistics=true` in dev.
Check `queries executed` count in logs. Fix with `@EntityGraph` or JOIN FETCH JPQL.

**Slow query logging**: `spring.jpa.properties.hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS=200`

**MSSQL JDBC URL** (dev):
```
jdbc:sqlserver://sqlserver:1433;databaseName=cosmolab;encrypt=false;trustServerCertificate=true
```

## RabbitMQ Integration

All topology declared as Spring beans in `RabbitMQConfig.java`.

```java
// clinical.events — topic exchange (future fan-out)
@Bean TopicExchange clinicalEventsExchange() {
    return new TopicExchange("clinical.events", true, false);
}

// audit.log — direct exchange, single consumer, quorum queue
@Bean Queue auditQueue() {
    return QueueBuilder.durable("audit-queue").quorum().build();
}
```

**Publishing strategy**:
- `clinical.events` — async fire-and-forget: `rabbitTemplate.convertAndSend()`
- `audit.log` — sync with publisher confirms: blocks until broker ACKs before HTTP response returns

**Consumer**:
```java
@RabbitListener(queues = "audit-queue", ackMode = "MANUAL")
public void consume(ClinicalEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
    // check (aggregateId, occurredAt) for duplicate before insert
    // channel.basicAck(tag, false) on success
    // channel.basicNack(tag, false, true) on failure — requeue
}
```
Consumer must be idempotent — RabbitMQ delivers at-least-once.

## Validation and Error Handling

Request DTOs use Bean Validation: `@NotBlank`, `@Size`, `@Pattern`.
Controllers annotate `@RequestBody @Valid`.

`GlobalExceptionHandler` (`@RestControllerAdvice`) maps all exceptions to RFC 7807 `ProblemDetail`:
```java
// Not found
ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "EHR " + id + " not found")

// Validation failure — include field errors in properties
ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
pd.setProperty("fieldErrors", buildFieldErrorMap(ex.getBindingResult()));

// Duplicate (personalNumber unique constraint)
ProblemDetail.forStatus(HttpStatus.CONFLICT)

// Unhandled — log full stack trace, hide detail from client
ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR)
```
Never let domain exceptions or stack traces leak to the HTTP response body.

## Security Scaffolding

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
               .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
               .build();
}
```
Backend currently permits all. Mock JWT is attached by Angular interceptor and propagated
on HTTP requests but not validated server-side. Auth is deferred — see decision §10 in CLAUDE.md.

## OpenAPI / Swagger UI

Dependency: `springdoc-openapi-starter-webmvc-ui:2.5.0` (compatible with Spring Boot 3.2.x).

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI spec (JSON): `http://localhost:8080/v3/api-docs`

Global metadata, server URL, and `bearerAuth` security scheme declared in `OpenApiConfig.java`.

Controllers are annotated with `@Tag(name="…")`, `@Operation`, and `@ApiResponse` per endpoint.
DTOs use `@Schema` on every field with descriptions and clinical-unit examples (mmHg, bpm, °C, %).

`springdoc.show-actuator: false` — actuator endpoints are excluded from the spec.

## Modern Java Patterns

These patterns are applied consistently across the application layer:

**`Optional` — never call `.get()` without guarding; never check-then-act with a separate `exists*` call:**
```java
// Wrong — TOCTOU race and two DB hits
if (repo.existsBySubjectId(id)) return repo.findBySubjectId(id).get();

// Correct
return repo.findBySubjectId(id).orElseGet(() -> {
    EhrRecord ehr = new EhrRecord();
    ehr.setSubjectId(id);
    return repo.save(ehr);
});
```

**Functional streams over for-loops with mutable accumulators:**
```java
// WardOverviewService pattern — Optional.stream() skips patients without an EHR
return patients.stream()
    .flatMap(p -> ehrRepo.findBySubjectId(p.getId()).map(e -> buildSummary(p, e)).stream())
    .toList();
```

**`Optional.ofNullable` per nullable field instead of chained null checks:**
```java
Optional.ofNullable(v.getSystolicBp()).filter(bp -> bp > 140).ifPresent(bp -> flags.add("systolicBP:HIGH"));
```

**Typed exceptions** — never throw bare `RuntimeException` from service layer. Each domain concept
has its own exception (`PatientNotFoundException`, `EhrNotFoundException`,
`CompositionNotFoundException`, `ProblemDiagnosisNotFoundException`) — all registered in
`GlobalExceptionHandler` and mapped to 404.

**Return `List.copyOf()`** from methods that build flag lists — returns an unmodifiable view,
prevents callers from mutating internal state.

## Testing — Testcontainers

Tests run against real SQL Server 2022 and RabbitMQ 3.12 containers — not H2 mocks.

`AbstractIntegrationTest` (extend for all `@SpringBootTest` tests):
```java
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static final MSSQLServerContainer<?> MSSQL =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
                    .acceptLicense()
                    .withPassword("CosmoLab@2024")
                    .withInitScript("init-db.sql");   // creates cosmolab database

    @Container
    static final RabbitMQContainer RABBIT =
            new RabbitMQContainer("rabbitmq:3.12-management");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> String.format(
                "jdbc:sqlserver://%s:%d;databaseName=cosmolab;encrypt=false;trustServerCertificate=true",
                MSSQL.getHost(), MSSQL.getMappedPort(1433)));
        registry.add("spring.datasource.username", MSSQL::getUsername);
        registry.add("spring.datasource.password", MSSQL::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
    }
}
```

`init-db.sql` (on test classpath): `IF NOT EXISTS (...) CREATE DATABASE cosmolab;`

`application-test.yml` only overrides `show-sql: true` and `tracing.enabled: false` — all
connection config comes from `@DynamicPropertySource` at runtime.

`static` containers start once per JVM and are shared across test classes via Spring's context
cache — container startup (~25 s for MSSQL) is paid once per `mvn test` run.

## WardOverviewService — Performance Notes

`WardOverviewService` is the primary load test target. It joins:
- `patients` + `ehr_records` + latest `vital_signs` per EHR (window function) + `problem_list_entries` count

Use a native SQL query or Spring Data `@Query` with a window function subquery.
Do NOT load all vitals and filter in Java — this becomes a full table scan under load.
The `(ward, status)` index on `patients` must be used — verify with `SET STATISTICS IO ON`.

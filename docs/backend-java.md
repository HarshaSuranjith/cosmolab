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

@Column(columnDefinition = "NVARCHAR(MAX)")
private String content;
```
VARCHAR silently corrupts Swedish characters (å, ä, ö) and any Unicode above code point 127.

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

## WardOverviewService — Performance Notes

`WardOverviewService` is the primary load test target. It joins:
- `patients` + `ehr_records` + latest `vital_signs` per EHR (window function) + `problem_list_entries` count

Use a native SQL query or Spring Data `@Query` with a window function subquery.
Do NOT load all vitals and filter in Java — this becomes a full table scan under load.
The `(ward, status)` index on `patients` must be used — verify with `SET STATISTICS IO ON`.

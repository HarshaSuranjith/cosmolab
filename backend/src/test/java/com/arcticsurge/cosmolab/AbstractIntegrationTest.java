package com.arcticsurge.cosmolab;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.RabbitMQContainer;

import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    // Singleton containers: started once per JVM, shared across all subclasses so
    // they stay alive for the full test run. Spring's test-context cache reuses the
    // same ApplicationContext across all test classes; having per-class container
    // lifecycle (@Testcontainers + @Container) kills the containers while the cached
    // context still holds open connection pools.
    //
    // Ryuk is disabled in this environment (rootless Podman), so orphaned containers
    // from a previous aborted run are cleaned up via a labelled podman rm -f sweep
    // before starting fresh ones. A JVM shutdown hook does the same at run end.
    static final MSSQLServerContainer<?> MSSQL =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
                    .acceptLicense()
                    .withLabel("cosmolab-testcontainer", "true")
                    .withPassword("CosmoLab@2024")
                    .withInitScript("init-db.sql");

    static final RabbitMQContainer RABBIT =
            new RabbitMQContainer("rabbitmq:3.12-management")
                    .withLabel("cosmolab-testcontainer", "true");

    static {
        purgeOrphanedContainers();
        MSSQL.start();
        RABBIT.start();
        Runtime.getRuntime().addShutdownHook(new Thread(AbstractIntegrationTest::purgeOrphanedContainers));
    }

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

    private static void purgeOrphanedContainers() {
        try {
            Process process = new ProcessBuilder(
                    "bash", "-c",
                    "podman ps -a --filter 'label=cosmolab-testcontainer=true' --format '{{.ID}}' | xargs -r podman rm -f"
            ).inheritIO().start();
            process.waitFor(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Container cleanup failed (non-fatal): {}", e.getMessage());
        }
    }
}

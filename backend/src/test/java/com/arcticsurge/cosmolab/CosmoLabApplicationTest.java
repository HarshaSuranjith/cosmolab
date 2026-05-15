package com.arcticsurge.cosmolab;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CosmoLabApplicationTest {

    // RabbitAutoConfiguration excluded in test profile; mock satisfies RabbitMQConfig's bean factories
    @MockBean
    ConnectionFactory connectionFactory;

    @Test
    void contextLoads() {
    }
}

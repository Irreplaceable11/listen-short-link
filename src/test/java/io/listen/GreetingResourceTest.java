package io.listen;

import io.listen.generator.SnowflakeIdGenerator;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class GreetingResourceTest {

    @Inject
    SnowflakeIdGenerator snowflakeIdGenerator;

    @Test
    void testId() {
        System.out.println(snowflakeIdGenerator.nextId());
    }

}
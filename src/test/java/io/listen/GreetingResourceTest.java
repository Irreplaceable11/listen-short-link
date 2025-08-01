package io.listen;

import io.listen.generator.ShortCodeGenerator;
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

    @Inject
    ShortCodeGenerator shortCodeGenerator;

    @Test
    void testId() {
        System.out.println(snowflakeIdGenerator.nextId());
    }

    @Test
    void testShortCode() {
        System.out.println(shortCodeGenerator.generateShortCode());
    }

}
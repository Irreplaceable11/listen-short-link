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

    @Test
    void testHelloEndpoint() {
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(is("Hello from Quarkus REST"));
    }

}
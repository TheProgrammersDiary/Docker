package com.evalvis;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;

import static io.restassured.RestAssured.get;

@Testcontainers
public class PostCommentTest {

    @Container
    private static final DockerComposeContainer<?> dockerComposeContainer =
            new DockerComposeContainer<>(
                    new File("src/test/resources/docker-compose-test.yaml")
            )
                    .withExposedService("postgres", 5432)
                    .withExposedService("mongodb", 27017)
                    .withExposedService("blog", 8080)
                    .withExposedService("post", 8080);

    @Test
    public void check() {
        get("http://localhost:8081/posts/test").then().assertThat().statusCode(200);
    }
}
package com.evalvis.globaltests;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.annotations.SnapshotName;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static shadow.org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith({SnapshotExtension.class})
public class PostCommentTest {
    private Expect expect;
    private final String sslPassword = System.getenv("ssl_blog_passphrase");

    @Test
    @SnapshotName("createsPostWithComments")
    public void createsPostWithComments() throws IOException {
        signUp();
        Cookie jwt = login();
        String postId = given()
                .trustStore("blog.p12", sslPassword)
                .baseUri("https://localhost:8081")
                .contentType("application/json")
                .body(
                        new ObjectMapper()
                                .createObjectNode()
                                .put("author", "Human")
                                .put("title", "Testing matters")
                                .put(
                                        "content",
                                        "You either test first, test along coding, or don't test at all."
                                )
                                .toString()
                )
                .cookie(jwt)
                .when()
                .post("/posts/create")
                .getBody()
                .jsonPath()
                .get("id")
                .toString();
        int commentCount = 2;
        String[] commentIds = new String[commentCount];
        for(int i = 0; i < commentCount; i++) {
            commentIds[i] = given()
                    .trustStore("blog.p12", sslPassword)
                    .baseUri("https://localhost:8080")
                    .contentType("application/json")
                    .body(
                            new ObjectMapper()
                                    .createObjectNode()
                                    .put("author", "author" + i)
                                    .put("content", "content" + i)
                                    .put("postId", postId)
                                    .toString()
                    )
                    .cookie(jwt)
                    .post("/comments/create")
                    .getBody()
                    .jsonPath()
                    .get("id");
        }
        ArrayNode comments = (ArrayNode) new ObjectMapper()
                .readTree(
                        given()
                                .trustStore("blog.p12", sslPassword)
                                .baseUri("https://localhost:8080")
                                .contentType("application/json")
                                .cookie(jwt)
                                .get("/comments/list-comments/" + postId)
                                .getBody()
                                .asString()
                );
        assertThat(comments.size()).isEqualTo(commentCount);
        for(int i = 0; i < comments.size(); i++) {
            assertThat(comments.get(i).get("id").textValue()).isEqualTo(commentIds[i]);
        }
        maskProperties(comments, "id", "postEntryId");
        expect.toMatchSnapshot(comments.toString());
    }

    private void signUp() {
        given()
                .trustStore("blog.p12", sslPassword)
                .baseUri("https://localhost:8080")
                .contentType("application/json")
                .body(
                        new ObjectMapper()
                                .createObjectNode()
                                .put("username", "testuser")
                                .put("password", "test")
                                .put("email", "testuser@test.com")
                                .toString()
                )
                .post("/users/signup");
    }

    private Cookie login() {
        return given()
                .trustStore("blog.p12", sslPassword)
                .baseUri("https://localhost:8080")
                .contentType("application/json")
                .body(
                        new ObjectMapper()
                                .createObjectNode()
                                .put("username", "testuser")
                                .put("password", "test")
                                .toString()
                )
                .post("/users/login")
                .getDetailedCookie("jwt");
    }

    private void maskProperties(ArrayNode node, String... properties) {
        node.forEach(element ->
                Arrays
                        .stream(properties)
                        .forEach(property -> ((ObjectNode) element).put(property, "#hidden#"))
        );
    }
}

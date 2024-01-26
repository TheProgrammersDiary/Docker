package com.evalvis.globaltests;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.annotations.SnapshotName;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static shadow.org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith({SnapshotExtension.class})
public class PostCommentTest {
    private Expect expect;
    private final String sslPassword;

    private final String postUrl;
    private final String blogUrl;

    public PostCommentTest() {
        this.sslPassword = System.getenv("ssl_blog_passphrase");
        this.postUrl = "https://localhost:8081";
        this.blogUrl = "https://localhost:8080";
    }

    @Test
    @SnapshotName("createsPostWithComments")
    public void createsPostWithComments() throws IOException {
        signUp();
        Response login = login();
        Cookie jwt = login.getDetailedCookie("jwt");
        String csrf = login.getBody().jsonPath().get("csrf").toString();
        String postId = given()
                .trustStore("blog.p12", sslPassword)
                .baseUri(postUrl)
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
                .header("X-CSRF-TOKEN", csrf)
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
                    .baseUri(blogUrl)
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
                                .baseUri(blogUrl)
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
                .baseUri(blogUrl)
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

    private Response login() {
        return given()
                .trustStore("blog.p12", sslPassword)
                .baseUri(blogUrl)
                .contentType("application/json")
                .body(
                        new ObjectMapper()
                                .createObjectNode()
                                .put("username", "testuser")
                                .put("password", "test")
                                .toString()
                )
                .post("/users/login");
    }

    private void maskProperties(ArrayNode node, String... properties) {
        node.forEach(element ->
                Arrays
                        .stream(properties)
                        .forEach(property -> ((ObjectNode) element).put(property, "#hidden#"))
        );
    }
}

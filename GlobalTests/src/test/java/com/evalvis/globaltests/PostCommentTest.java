package com.evalvis.globaltests;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.annotations.SnapshotName;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.node.ArrayNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Arrays;

import static io.restassured.RestAssured.*;
import static shadow.org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith({SnapshotExtension.class})
public class PostCommentTest {

    private Expect expect;

    @Test
    @SnapshotName("createsPostWithComments")
    public void createsPostWithComments() throws IOException {
        String postId = given()
                .baseUri("http://localhost:8081")
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
                    .baseUri("http://localhost:8080")
                    .contentType("application/json")
                    .body(
                            new ObjectMapper()
                                    .createObjectNode()
                                    .put("author", "author" + i)
                                    .put("content", "content" + i)
                                    .put("postId", postId)
                                    .toString()
                    )
                    .post("/comments/create")
                    .getBody()
                    .jsonPath()
                    .get("id");
        }
        ArrayNode comments = (ArrayNode) new ObjectMapper().readTree(
                get("http://localhost:8080/comments/list-comments/" + postId)
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

    private void maskProperties(ArrayNode node, String... properties) {
        node.forEach(element ->
                Arrays
                        .stream(properties)
                        .forEach(property -> ((ObjectNode) element).put(property, "#hidden#"))
        );
    }
}

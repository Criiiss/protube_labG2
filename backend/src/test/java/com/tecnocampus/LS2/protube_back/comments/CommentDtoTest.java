package com.tecnocampus.LS2.protube_back.comments;

import com.tecnocampus.LS2.protube_back.comments.adapter.in.web.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentDtoTest {
    @Test
    void preservesCommentResponseFields() {
        var time = Instant.parse("2026-01-01T00:00:00Z");
        var dto = new CommentResponseDTO(1L, 2L, 3L, "user", "Name", "/avatar",
                "content", 4L, 5, true, time);
        assertEquals(1L, dto.id());
        assertEquals(2L, dto.videoId());
        assertEquals(4L, dto.parentId());
        assertTrue(dto.likedByCurrentUser());
        assertEquals(time, dto.createdAt());
    }

    @Test
    void preservesRequestFields() {
        var create = new CreateCommentRequestDTO("content", 2L);
        var update = new UpdateCommentRequestDTO("updated");
        assertEquals("content", create.content());
        assertEquals(2L, create.parentId());
        assertEquals("updated", update.content());
    }
}

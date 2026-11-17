package com.tecnocampus.LS2.protube_back.comments;

import com.tecnocampus.LS2.protube_back.auth.AuthFailure;
import com.tecnocampus.LS2.protube_back.comments.application.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CommentServiceValidationTest {
    private JdbcTemplate jdbc;
    private CommentService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        service = new CommentService(jdbc);
    }

    @Test
    void rejectsUnavailableVideos() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1L))).thenReturn(0);
        assertThrows(IllegalArgumentException.class, () -> service.createComment(1L, 2L, "hello", null));
    }

    @Test
    void rejectsBlankAndOversizedContent() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1L))).thenReturn(1);
        assertThrows(IllegalArgumentException.class, () -> service.createComment(1L, 2L, "  ", null));
        assertThrows(IllegalArgumentException.class, () -> service.createComment(1L, 2L, "x".repeat(2001), null));
    }

    @Test
    void rejectsParentFromAnotherVideo() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1L))).thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(9L), eq(1L))).thenReturn(0);
        assertThrows(AuthFailure.class, () -> service.createComment(1L, 2L, "hello", 9L));
    }

    @Test
    void validatesUpdateContentAndOwnership() {
        assertThrows(IllegalArgumentException.class, () -> service.updateComment(1L, 2L, 3L, ""));
        assertThrows(IllegalArgumentException.class, () -> service.updateComment(1L, 2L, 3L, "x".repeat(2001)));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(2L), eq(1L))).thenReturn(0);
        assertThrows(IllegalArgumentException.class, () -> service.updateComment(1L, 2L, 3L, "ok"));
    }

    @Test
    void rejectsNonOwnerDeleteAndMissingLikes() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(2L), eq(1L), eq(3L))).thenReturn(0);
        assertThrows(AuthFailure.class, () -> service.deleteComment(1L, 2L, 3L));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(2L), eq(1L))).thenReturn(0);
        assertThrows(AuthFailure.class, () -> service.likeComment(1L, 2L, 3L));
        assertThrows(AuthFailure.class, () -> service.unlikeComment(1L, 2L, 3L));
    }
}

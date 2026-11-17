package com.tecnocampus.LS2.protube_back.comments;

import com.tecnocampus.LS2.protube_back.auth.AuthService;
import com.tecnocampus.LS2.protube_back.comments.adapter.in.web.*;
import com.tecnocampus.LS2.protube_back.comments.application.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommentControllerTest {
    private final CommentService comments = mock(CommentService.class);
    private final AuthService auth = mock(AuthService.class);
    private final CommentController controller = new CommentController(comments, auth);
    private final CommentResponseDTO response = new CommentResponseDTO(
            3L, 9L, 2L, "user", "User", null, "content", null, 0, false, Instant.now());

    @Test
    void createsCommentForAuthenticatedUser() {
        when(auth.version(2L)).thenReturn(0);
        when(comments.createComment(9L, 2L, "content", null)).thenReturn(response);

        var result = controller.createComment(9L, new CreateCommentRequestDTO("content", null), authenticated());

        assertEquals(201, result.getStatusCode().value());
        assertEquals(response, result.getBody());
        verify(comments).createComment(9L, 2L, "content", null);
    }

    @Test
    void getsCommentsWithCurrentUserAndAllowsAnonymousRequests() {
        when(comments.getComments(9L, null)).thenReturn(List.of(response));
        var result = controller.getComments(9L, new MockHttpServletRequest());
        assertEquals(List.of(response), result.getBody());
        verify(comments).getComments(9L, null);
    }

    @Test
    void getsCommentsWithAuthenticatedCurrentUser() {
        when(auth.version(2L)).thenReturn(0);
        when(comments.getComments(9L, 2L)).thenReturn(List.of(response));
        var result = controller.getComments(9L, authenticated());
        assertEquals(List.of(response), result.getBody());
        verify(comments).getComments(9L, 2L);
    }

    @Test
    void updatesAndDeletesComment() {
        when(auth.version(2L)).thenReturn(0);
        when(comments.updateComment(9L, 3L, 2L, "updated")).thenReturn(response);
        HttpServletRequest request = authenticated();

        assertEquals(response, controller.updateComment(9L, 3L,
                new UpdateCommentRequestDTO("updated"), request).getBody());
        assertEquals(204, controller.deleteComment(9L, 3L, request).getStatusCode().value());
        verify(comments).deleteComment(9L, 3L, 2L);
    }

    @Test
    void likesAndUnlikesComment() {
        when(auth.version(2L)).thenReturn(0);
        when(comments.likeComment(9L, 3L, 2L)).thenReturn(response);
        when(comments.unlikeComment(9L, 3L, 2L)).thenReturn(response);
        HttpServletRequest request = authenticated();

        assertEquals(response, controller.likeComment(9L, 3L, request).getBody());
        assertEquals(response, controller.unlikeComment(9L, 3L, request).getBody());
        verify(comments).likeComment(9L, 3L, 2L);
        verify(comments).unlikeComment(9L, 3L, 2L);
    }

    @Test
    void rejectsMutationWithoutAuthentication() {
        assertThrows(RuntimeException.class, () -> controller.createComment(
                9L, new CreateCommentRequestDTO("content", null), new MockHttpServletRequest()));
        verifyNoInteractions(comments);
    }

    private MockHttpServletRequest authenticated() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authUser", 2L);
        session.setAttribute("authVersion", 0);
        session.setAttribute("authDeadline", Instant.now().plusSeconds(300));
        request.setSession(session);
        return request;
    }
}

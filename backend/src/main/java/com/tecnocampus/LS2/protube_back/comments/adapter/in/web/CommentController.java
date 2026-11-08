package com.tecnocampus.LS2.protube_back.comments.adapter.in.web;

import com.tecnocampus.LS2.protube_back.auth.AuthController;
import com.tecnocampus.LS2.protube_back.auth.AuthService;
import com.tecnocampus.LS2.protube_back.comments.application.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/videos/{videoId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final AuthService authService;

    public CommentController(CommentService commentService, AuthService authService) {
        this.commentService = commentService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<CommentResponseDTO> createComment(@PathVariable Long videoId, @RequestBody CreateCommentRequestDTO request, HttpServletRequest servletRequest) {
        Long userId = AuthController.requireUser(servletRequest, authService);
        CommentResponseDTO comment = commentService.createComment(videoId, userId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @GetMapping
    public ResponseEntity<List<CommentResponseDTO>> getComments(@PathVariable Long videoId) {
        return ResponseEntity.ok(commentService.getComments(videoId));
    }

}

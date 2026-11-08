package com.tecnocampus.LS2.protube_back.comments.adapter.in.web;

import java.time.Instant;

public record CommentResponseDTO(Long id, Long videoId, Long authorId, String username, String displayName, String avatarUrl, String content, Long parentAt, long likeCount, Instant createdAt) {
}

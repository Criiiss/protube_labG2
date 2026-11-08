package com.tecnocampus.LS2.protube_back.comments.application.service;

import com.tecnocampus.LS2.protube_back.auth.AuthFailure;
import com.tecnocampus.LS2.protube_back.comments.adapter.in.web.CommentResponseDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.List;

@Service
public class CommentService {
    private final JdbcTemplate jdbcTemplate;
    public CommentService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public CommentResponseDTO createComment(Long videoId, Long authId, String content, Long parentId){
        ensureVideoAvailable(videoId);
        String normalizedContent = content== null ? "" : content.trim();
        if (normalizedContent.isBlank()) throw new IllegalArgumentException("Comment content cannot be empty");
        if (normalizedContent.length() > 2000) throw new IllegalArgumentException("Comment content cannot exceed 2000 characters");
        if (parentId != null) ensureParentCommentBelongsToVideo(parentId, videoId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO comments (
                        video_id,
                        author_id,
                        parent_id,
                        content,
                        like_count,
                        created_at,
                        updated_at
                    )
                    VALUES (?, ?, NULL, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, videoId);
            statement.setLong(2, authId);
            if (parentId == null)statement.setNull(3, Types.BIGINT);
            else statement.setLong(3, parentId);
            statement.setString(3, normalizedContent);
            return statement;
        }, keyHolder);
        Number generatedId = keyHolder.getKey();
        if (generatedId == null) throw new IllegalStateException("Failed to retrieve generated comment ID");
        return jdbcTemplate.queryForObject(
                """
                   SELECT
                       c.id,
                       c.video_id,
                       c.author_id,
                       u.username,
                       up.display_name,
                       up.avatar_url,
                       c.content,
                       c.parent_id,
                       c.like_count,
                       c.created_at
                   FROM comments c
                   INNER JOIN users u ON u.id = c.author_id
                   LEFT JOIN user_profiles up ON up.user_id = u.id
                   WHERE c.id = ?
                   """,
                (resultSet, rowNumber) -> mapComment(resultSet),
                generatedId.longValue()
        );
    }


    public List<CommentResponseDTO> getComments(Long videoId) {
        ensureVideoAvailable(videoId);
        return jdbcTemplate.query(
            """
                    SELECT
                        c.id,
                        c.video_id,
                        c.author_id,
                        u.username,
                        up.display_name,
                        up.avatar_url,
                        c.content,
                        c.parent_id,
                        c.like_count,
                        c.created_at
                    FROM comments c
                    INNER JOIN users u ON u.id = c.author_id
                    LEFT JOIN user_profiles up ON up.user_id = u.id
                    WHERE c.video_id = ?
                    ORDER BY c.created_at DESC 
                    """,
                    (resultSet, rowNumber) -> mapComment(resultSet),
                    videoId
        );
    }

    private void ensureVideoAvailable(Long videoId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM videos WHERE id = ? AND status = 'PUBLISHED' AND visibility = 'PUBLIC'",
                Integer.class,
                videoId
        );
        if (count == null || count == 0) throw new IllegalArgumentException("Video not found or not available for comments");

    }

    private CommentResponseDTO mapComment(ResultSet resultSet) throws SQLException {
        return new CommentResponseDTO(
                resultSet.getLong("id"),
                resultSet.getLong("video_id"),
                resultSet.getLong("author_id"),
                resultSet.getString("username"),
                resultSet.getString("display_name"),
                getAvatarUrl(resultSet.getString("avatar_url")),
                resultSet.getString("content"),
                getNullableLong(resultSet, "parent_id"),
                resultSet.getLong("like_count"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }

    private Long getNullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private String getAvatarUrl(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) return null;
        if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://") || avatarUrl.startsWith("data:")) return avatarUrl;
        return "/media/" + avatarUrl;
    }

    private void ensureParentCommentBelongsToVideo(Long parentId, Long videoId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM comments
                WHERE id = ?
                  AND video_id = ?
                """,
                Integer.class,
                parentId,
                videoId
        );
        if (count == null || count == 0) {
            throw new AuthFailure(
                    400,
                    "El comentario padre no pertenece a este vídeo."
            );
        }
    }
}

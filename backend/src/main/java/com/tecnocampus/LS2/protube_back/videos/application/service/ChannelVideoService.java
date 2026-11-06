package com.tecnocampus.LS2.protube_back.videos.application.service;

import com.tecnocampus.LS2.protube_back.videos.adapter.in.web.ChannelVideoResponseDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class ChannelVideoService {
    private final JdbcTemplate jdbcTemplate;

    public ChannelVideoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ChannelVideoResponseDTO> getPublicVideosByUserId(Long userId) {
        String sql = """
                SELECT
                    v.id,
                    v.title,
                    u.username,
                    up.display_name,
                    up.avatar_url,
                    v.description,
                    v.view_count,
                    v.created_at,
                    v.duration_sec,
                    v.file_name,
                    v.thumbnail_name,
                    v.status,
                    v.visibility
                FROM videos v
                INNER JOIN users u ON u.id = v.owner_id
                LEFT JOIN user_profiles up ON up.user_id = u.id
                WHERE v.owner_id = ?
                  AND v.status = 'PUBLISHED'
                  AND v.visibility = 'PUBLIC'
                ORDER BY v.created_at DESC
                """;

        return jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> mapVideo(resultSet),
                userId
        );
    }

    public List<ChannelVideoResponseDTO> getOwnerVideosByStatus(Long userId, String status) {
        String sql = """
                SELECT
                    v.id,
                    v.title,
                    u.username,
                    v.view_count,
                    v.created_at,
                    v.duration_sec,
                    v.file_name,
                    v.thumbnail_name,
                    v.status,
                    v.visibility
                FROM videos v
                INNER JOIN users u ON u.id = v.owner_id
                WHERE v.owner_id = ?
                  AND v.status = ?
                ORDER BY v.created_at DESC
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> mapVideo(resultSet), userId, status);
    }

    private ChannelVideoResponseDTO mapVideo(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        long durationSeconds = resultSet.getLong("duration_sec");
        long viewCount = resultSet.getLong("view_count");
        String thumbnailName = resultSet.getString("thumbnail_name");
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        String fileName = resultSet.getString("file_name");
        return new ChannelVideoResponseDTO(
                resultSet.getString("id"),
                resultSet.getString("title"),
                resultSet.getString("username"),
                resultSet.getString("display_name"),
                getAvatarUrl(resultSet.getString("avatar_url")),
                resultSet.getString("description"),
                viewCount + " visualizaciones",
                "",
                formatDuration(durationSeconds),
                "all",
                getThumbnailUrl(thumbnailName),
                getVideoUrl(fileName),
                viewCount,
                createdAt == null
                        ? null
                        : createdAt.toInstant().toString(),
                resultSet.getString("status"),
                resultSet.getString("visibility")
        );
    }

    private String formatDuration(long durationSeconds) {
        long minutes = durationSeconds / 60;
        long seconds = durationSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }

    private String getThumbnailUrl(String thumbnailName) {
        if (thumbnailName == null || thumbnailName.isBlank()) return "/dog.png";
        if (thumbnailName.startsWith("http://") || thumbnailName.startsWith("https://") || thumbnailName.startsWith("/")) return thumbnailName;
        return "/media/" + thumbnailName;
    }

    private String getVideoUrl(String fileName) {
        if (fileName == null || fileName.isBlank()) return null;
        if (fileName.startsWith("http://")|| fileName.startsWith("https://")|| fileName.startsWith("/"))return fileName;
        return "/media/" + fileName;
    }
    private String getAvatarUrl(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) return null;
        if (avatarUrl.startsWith("http://")|| avatarUrl.startsWith("https://")|| avatarUrl.startsWith("data:") || avatarUrl.startsWith("/")) return avatarUrl;
        return "/media/" + avatarUrl;
    }
}

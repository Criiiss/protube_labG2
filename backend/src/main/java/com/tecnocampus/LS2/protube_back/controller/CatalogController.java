package com.tecnocampus.LS2.protube_back.controller;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.sql.*;

/** Keeps the legacy /api/videos contract intact; exposes metadata from the existing schema. */
@RestController
public class CatalogController {
    private final JdbcTemplate jdbc;
    public CatalogController(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Video(
            String id,
            String title,
            String channel,
            String views,
            String published,
            String duration,
            String categoryId,
            String thumbnail,
            long viewCount,
            String createdAt
    ) {
    }

    @GetMapping("/api/catalog")
    public List<Video> videos() {
        if (!videosTableExists()) {
            return List.of();
        }

        return jdbc.query(
                """
                SELECT
                    v.id,
                    v.title,
                    u.username,
                    v.view_count,
                    v.created_at,
                    v.duration_sec,
                    v.thumbnail_name
                FROM videos v
                INNER JOIN users u ON u.id = v.owner_id
                WHERE v.status = 'PUBLISHED' AND v.visibility = 'PUBLIC'
                ORDER BY v.created_at DESC
                """,
                (resultSet, rowNumber) -> {
                    long durationSeconds = resultSet.getLong("duration_sec");
                    long viewCount = resultSet.getLong("view_count");
                    String thumbnailName = resultSet.getString("thumbnail_name");
                    Timestamp createdAt = resultSet.getTimestamp("created_at");

                    return new Video(
                            resultSet.getString("id"),
                            resultSet.getString("title"),
                            resultSet.getString("username"),
                            viewCount + " visualizaciones",
                            "",
                            formatDuration(durationSeconds),
                            "all",
                            getThumbnailUrl(thumbnailName),
                            viewCount,
                            createdAt == null
                                    ? null
                                    : createdAt.toInstant().toString()
                    );
                }
        );
    }

    private boolean videosTableExists() {
        Boolean exists = jdbc.execute((ConnectionCallback<Boolean>) connection -> {
            try (var tables = connection.getMetaData().getTables(
                    null,
                    null,
                    "%",
                    new String[]{"TABLE"}
            )) {
                while (tables.next()) {
                    if ("videos".equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                        return true;
                    }
                }
                return false;
            }
        });
        return Boolean.TRUE.equals(exists);
    }

    private String formatDuration(long durationSeconds) {
        long minutes = durationSeconds / 60;
        long seconds = durationSeconds % 60;

        return minutes + ":" + String.format("%02d", seconds);
    }

    private String getThumbnailUrl(String thumbnailName) {
        if (thumbnailName == null || thumbnailName.isBlank()) {
            return "/dog.png";
        }

        if (thumbnailName.startsWith("http://")
                || thumbnailName.startsWith("https://")
                || thumbnailName.startsWith("/")) {
            return thumbnailName;
        }

        return "/media/" + thumbnailName;
    }
}

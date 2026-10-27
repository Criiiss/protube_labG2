package com.tecnocampus.LS2.protube_back.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.sql.*;

/** Keeps the legacy /api/videos contract intact; exposes metadata from the existing schema. */
@RestController
public class CatalogController {
    private final JdbcTemplate jdbc;
    public CatalogController(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Video(String id, String title, String channel, String views, String published, String duration,
        String categoryId, String thumbnail, long viewCount, String createdAt) {}
    @GetMapping("/api/catalog") public List<Video> videos() {
        Boolean exists = jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>) connection -> {
            try (var tables = connection.getMetaData().getTables(null, null, "%", new String[] { "TABLE" })) {
                while (tables.next()) if ("videos".equalsIgnoreCase(tables.getString("TABLE_NAME"))) return true;
                return false;
            }
        });
        if (!Boolean.TRUE.equals(exists)) return List.of();
        return jdbc.query("SELECT v.id, v.title, u.username, v.view_count, v.created_at, v.duration_sec FROM videos v JOIN users u ON u.id = v.owner_id ORDER BY v.created_at DESC", (row, index) -> {
            long seconds = row.getLong("duration_sec"); long views = row.getLong("view_count");
            return new Video(row.getString("id"), row.getString("title"), row.getString("username"), views + " visualizaciones", "", seconds / 60 + ":" + String.format("%02d", seconds % 60),
                "all", "/dog.png", views, row.getTimestamp("created_at").toInstant().toString());
        });
    }
}

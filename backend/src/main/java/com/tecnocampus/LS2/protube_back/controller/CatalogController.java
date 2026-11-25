package com.tecnocampus.LS2.protube_back.controller;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
import java.sql.*;
import jakarta.servlet.http.HttpServletRequest;
import com.tecnocampus.LS2.protube_back.auth.AuthService;
import com.tecnocampus.LS2.protube_back.auth.AuthAccounts;
import com.tecnocampus.LS2.protube_back.studio.StudioController;
import org.springframework.beans.factory.annotation.Autowired;

/** Catalogue: public published videos (imported and published from Studio). 18+ ones only for adults and their owner. */
@RestController
public class CatalogController {
    private final JdbcTemplate jdbc;
    @Autowired private AuthService auth;
    @Autowired private AuthAccounts accounts;
    public CatalogController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Video(
            String id,
            String title,
            String channel,
            String displayName,
            String avatarUrl,
            String description,
            String views,
            String published,
            String duration,
            String categoryId,
            String thumbnail,
            String videoUrl,
            long viewCount,
            String createdAt
    ) {
    }

    @GetMapping("/api/catalog")
    public List<Video> catalog(HttpServletRequest request) {
        Long viewer = StudioController.viewer(request, auth);
        boolean adult = viewer != null && accounts.findById(viewer)
                .map(account -> account.birthDate != null && !account.birthDate.isAfter(LocalDate.now().minusYears(18)))
                .orElse(false);
        return videos(viewer, adult);
    }

    /** What an anonymous visitor sees. */
    public List<Video> videos() {
        return videos(null, false);
    }

    public List<Video> videos(Long viewer, boolean adult) {
        if (!tableExists("videos")) {
            return List.of();
        }
        Map<Long, String> categories = new HashMap<>();
        if (tableExists("video_categories"))
            jdbc.query("SELECT vc.video_id, MIN(c.name) FROM video_categories vc JOIN categories c ON c.id = vc.category_id GROUP BY vc.video_id",
                    row -> { categories.put(row.getLong(1), row.getString(2)); });

        return jdbc.query(
                """
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
                    v.thumbnail_name
                FROM videos v
                INNER JOIN users u ON u.id = v.owner_id
                LEFT JOIN user_profiles up ON up.user_id = u.id
                WHERE v.status = 'PUBLISHED' AND v.visibility = 'PUBLIC'
                  AND (v.age_restricted = FALSE OR ? OR v.owner_id = ?)
                ORDER BY v.created_at DESC
                """,
                (resultSet, rowNumber) -> {
                    long durationSeconds = resultSet.getLong("duration_sec");
                    long viewCount = resultSet.getLong("view_count");
                    String thumbnailName = resultSet.getString("thumbnail_name");
                    String fileName = resultSet.getString("file_name");
                    Timestamp createdAt = resultSet.getTimestamp("created_at");
                    String displayName = resultSet.getString("display_name");
                    String avatarUrl = resultSet.getString("avatar_url");
                    String description = resultSet.getString("description");

                    return new Video(
                            resultSet.getString("id"),
                            resultSet.getString("title"),
                            resultSet.getString("username"),
                            displayName,
                            getAvatarUrl(avatarUrl),
                            description,
                            viewCount + " visualizaciones",
                            "",
                            formatDuration(durationSeconds),
                            categories.getOrDefault(resultSet.getLong("id"), "all"),
                            getThumbnailUrl(thumbnailName),
                            getVideoUrl(fileName),
                            viewCount,
                            createdAt == null
                                    ? null
                                    : createdAt.toInstant().toString()
                    );
                },
                adult, viewer == null ? -1L : viewer
        );
    }

    private boolean tableExists(String name) {
        Boolean exists = jdbc.execute((ConnectionCallback<Boolean>) connection -> {
            try (var tables = connection.getMetaData().getTables(
                    null,
                    null,
                    "%",
                    new String[]{"TABLE"}
            )) {
                while (tables.next()) {
                    if (name.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
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
        if (thumbnailName.startsWith("http://")|| thumbnailName.startsWith("https://")|| thumbnailName.startsWith("/")) return thumbnailName;
        return "/media/" + thumbnailName;
    }

    private String getVideoUrl(String fileName) {
        if (fileName == null || fileName.isBlank()) return null;
        if (fileName.startsWith("http://")|| fileName.startsWith("https://")|| fileName.startsWith("/")) return fileName;
        return "/media/" + fileName;
    }
    private String getAvatarUrl(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) return null;
        if (avatarUrl.startsWith("http://")|| avatarUrl.startsWith("https://")|| avatarUrl.startsWith("data:") || avatarUrl.startsWith("/")) return avatarUrl;
        return "/media/" + avatarUrl;
    }
}

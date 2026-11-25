package com.tecnocampus.LS2.protube_back.studio;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/**
 * Publishes READY Studio uploads as rows of the videos table, so the catalogue, comments, playlists and search treat
 * them like any other video. The files stay in Studio storage and are served by /api/studio/play/{id}/...
 */
@Component
public class StudioPublisher {
    private final JdbcTemplate jdbc;
    private Boolean schemaReady;

    public StudioPublisher(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Creates or updates the videos row of a READY upload. Run inside the caller's transaction. */
    void sync(StudioVideo video, VideoMetadata m, Instant now) {
        if (video.status != StudioVideo.Status.READY || !schemaReady()) return;
        String status = m.draft() ? "DRAFT" : m.publishAt() != null && m.publishAt().isAfter(now) ? "SCHEDULED" : "PUBLISHED";
        String thumbnail = "/api/studio/play/" + video.id + "/thumbnail";
        String file = "/api/studio/play/" + video.id + "/720.mp4";
        Timestamp publishAt = m.publishAt() == null ? null : Timestamp.from(m.publishAt());
        if (video.videoId == null) {
            video.videoId = jdbc.queryForObject("""
                    INSERT INTO videos (owner_id, title, description, file_name, thumbnail_name, duration_sec, created_at,
                                        visibility, status, publish_at, age_restricted, comments_allowed, language)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class,
                    video.ownerId, m.title(), m.description(), file, thumbnail, video.duration, Timestamp.from(video.createdAt),
                    m.visibility().name(), status, publishAt, m.ageRestricted(), m.commentsAllowed(), m.language());
        } else {
            jdbc.update("""
                    UPDATE videos SET title = ?, description = ?, thumbnail_name = ?, duration_sec = ?, visibility = ?,
                                      status = ?, publish_at = ?, age_restricted = ?, comments_allowed = ?, language = ?,
                                      updated_at = now()
                    WHERE id = ?""", m.title(), m.description(), thumbnail, video.duration, m.visibility().name(), status,
                    publishAt, m.ageRestricted(), m.commentsAllowed(), m.language(), video.videoId);
        }
        replace(video.videoId, "tags", "video_tags", "tag_id", m.tags());
        replace(video.videoId, "categories", "video_categories", "category_id",
                m.category() == null || m.category().equals("all") ? List.of() : List.of(m.category()));
    }

    /** Scheduled videos become PUBLISHED once their date arrives. Returns how many changed. */
    int publishDue(Instant now) {
        if (!schemaReady()) return 0;
        return jdbc.update("UPDATE videos SET status = 'PUBLISHED', updated_at = now() WHERE status = 'SCHEDULED' AND publish_at <= ?",
                Timestamp.from(now));
    }

    /** Tags and categories are lowercase and shared between videos (chk_*_name_lowercase). */
    private void replace(long videoId, String table, String joinTable, String column, List<String> names) {
        jdbc.update("DELETE FROM " + joinTable + " WHERE video_id = ?", videoId);
        Set<String> clean = new LinkedHashSet<>();
        for (String name : names == null ? List.<String>of() : names)
            if (name != null && !name.isBlank()) clean.add(name.strip().toLowerCase(Locale.ROOT));
        for (String name : clean) {
            jdbc.update("INSERT INTO " + table + " (name) VALUES (?) ON CONFLICT (name) DO NOTHING", name);
            Long id = jdbc.queryForObject("SELECT id FROM " + table + " WHERE name = ?", Long.class, name);
            jdbc.update("INSERT INTO " + joinTable + " (video_id, " + column + ") VALUES (?, ?)", videoId, id);
        }
    }

    /** The videos table comes from Flyway (prod profile); the dev profile (H2) has none, so nothing is published. */
    private boolean schemaReady() {
        if (schemaReady == null) schemaReady = Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection -> {
            try (var tables = connection.getMetaData().getTables(null, null, "%", new String[] { "TABLE" })) {
                while (tables.next()) if ("videos".equalsIgnoreCase(tables.getString("TABLE_NAME"))) return true;
                return false;
            }
        }));
        return schemaReady;
    }
}

package com.tecnocampus.LS2.protube_back.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.Instant;
import java.util.*;

/**
 * Copies what may be searched from Postgres into the Mongo index: public published videos, public ready Studio
 * uploads (once the studio_videos table exists) and public playlists.
 * shortcut: full rebuild every pro_tube.search.reindex-ms; switch to updating one item on create/edit if the
 * catalogue grows past a few thousand videos.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SearchIndexer {
    private static final Logger LOG = LoggerFactory.getLogger(SearchIndexer.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final JdbcTemplate jdbc;
    private final SearchService search;

    public SearchIndexer(JdbcTemplate jdbc, SearchService search) {
        this.jdbc = jdbc; this.search = search;
    }

    @Scheduled(fixedDelayString = "${pro_tube.search.reindex-ms:60000}")
    public synchronized void reindex() {
        try {
            List<SearchDocument> videos = videos();
            List<SearchPlaylist> playlists = playlists();
            search.replaceAll(videos, playlists);
            LOG.info("Índice de búsqueda actualizado: {} vídeos, {} playlists", videos.size(), playlists.size());
        } catch (RuntimeException ex) { // Mongo down must not break the rest of the app; next run retries
            LOG.warn("No se pudo actualizar el índice de búsqueda: {}", ex.getMessage());
        }
    }

    List<SearchDocument> videos() {
        if (!tableExists("videos")) return List.of(); // dev profile (H2) has no videos table
        Map<Long, List<String>> tags = new HashMap<>();
        if (tableExists("video_tags"))
            jdbc.query("SELECT vt.video_id, t.name FROM video_tags vt JOIN tags t ON t.id = vt.tag_id",
                    row -> { tags.computeIfAbsent(row.getLong(1), id -> new ArrayList<>()).add(row.getString(2)); });
        List<SearchDocument> docs = new ArrayList<>(jdbc.query("""
                SELECT v.id, v.title, v.description, v.duration_sec, v.view_count, v.created_at, u.username
                FROM videos v JOIN users u ON u.id = v.owner_id
                WHERE v.status = 'PUBLISHED' AND v.visibility = 'PUBLIC'""", (row, index) -> SearchDocument.of(
                row.getString("id"), row.getString("title"), row.getString("username"),
                tags.getOrDefault(row.getLong("id"), List.of()), row.getString("description"),
                row.getDouble("duration_sec"), row.getLong("view_count"),
                row.getTimestamp("created_at").toInstant(), "/dog.png")));
        docs.addAll(studioVideos());
        return docs;
    }

    /** Same rule as the Studio catalog: ready, public, not a draft, already published and not 18+. */
    private List<SearchDocument> studioVideos() {
        if (!tableExists("studio_videos")) return List.of();
        List<SearchDocument> docs = new ArrayList<>();
        jdbc.query("""
                SELECT s.id, s.metadata, s.duration, s.created_at, u.username
                FROM studio_videos s JOIN users u ON u.id = s.owner_id WHERE s.status = 'READY'""", row -> {
            SearchDocument doc = studioVideo(row.getString("id"), row.getString("metadata"), row.getDouble("duration"),
                    row.getTimestamp("created_at").toInstant(), row.getString("username"), Instant.now());
            if (doc != null) docs.add(doc);
        });
        return docs;
    }

    static SearchDocument studioVideo(String id, String metadata, double duration, Instant createdAt, String owner, Instant now) {
        try {
            JsonNode m = JSON.readTree(metadata);
            Instant publishAt = m.path("publishAt").isTextual() ? Instant.parse(m.path("publishAt").asText())
                    : m.path("publishAt").isNumber() ? Instant.ofEpochMilli((long) (m.path("publishAt").asDouble() * 1000)) : null;
            if (!"PUBLIC".equals(m.path("visibility").asText()) || m.path("draft").asBoolean()
                    || m.path("ageRestricted").asBoolean() || (publishAt != null && publishAt.isAfter(now))) return null;
            List<String> tags = new ArrayList<>();
            m.path("tags").forEach(tag -> tags.add(tag.asText()));
            return SearchDocument.of(id, m.path("title").asText(), owner, tags, m.path("description").asText(""), duration, 0,
                    publishAt == null ? createdAt : publishAt, "/api/studio/play/" + id + "/thumbnail");
        } catch (Exception ex) { // one broken metadata blob must not stop the whole reindex
            LOG.warn("Metadatos de Studio no válidos en {}: {}", id, ex.getMessage());
            return null;
        }
    }

    List<SearchPlaylist> playlists() {
        if (!tableExists("playlists")) return List.of();
        return jdbc.query("""
                SELECT p.id, p.title, p.description, p.created_at, u.username, COUNT(pv.video_id) AS videos
                FROM playlists p JOIN users u ON u.id = p.owner_id
                LEFT JOIN playlist_videos pv ON pv.playlist_id = p.id
                WHERE p.visibility = 'PUBLIC'
                GROUP BY p.id, p.title, p.description, p.created_at, u.username""", (row, index) -> SearchPlaylist.of(
                row.getString("id"), row.getString("title"), row.getString("username"), row.getString("description"),
                row.getLong("videos"), row.getTimestamp("created_at").toInstant()));
    }

    private boolean tableExists(String name) {
        return Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection -> {
            try (var tables = connection.getMetaData().getTables(null, null, "%", new String[] { "TABLE" })) {
                while (tables.next()) if (name.equalsIgnoreCase(tables.getString("TABLE_NAME"))) return true;
                return false;
            }
        }));
    }
}

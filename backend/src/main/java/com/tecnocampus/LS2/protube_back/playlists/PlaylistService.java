package com.tecnocampus.LS2.protube_back.playlists;

import com.tecnocampus.LS2.protube_back.auth.AuthFailure;
import com.tecnocampus.LS2.protube_back.search.SearchDocument;
import com.tecnocampus.LS2.protube_back.search.SearchService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;

/** Playlists of catalogue videos (table videos). Only the owner can change a playlist; private ones are owner-only. */
@Service
public class PlaylistService {
    public record Summary(long id, String title, String visibility, long videoCount, boolean hasVideo) {}
    public record Detail(long id, String title, String description, String visibility, String owner, boolean mine,
                         List<SearchService.VideoHit> videos) {}
    public record NewPlaylist(String title, String description, String visibility) {}

    private final JdbcTemplate jdbc;
    public PlaylistService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Summary> mine(long userId, Long videoId) {
        return jdbc.query("""
                SELECT p.id, p.title, p.visibility, COUNT(pv.video_id) AS videos,
                       COALESCE(MAX(CASE WHEN pv.video_id = ? THEN 1 ELSE 0 END), 0) AS has_video
                FROM playlists p LEFT JOIN playlist_videos pv ON pv.playlist_id = p.id
                WHERE p.owner_id = ? GROUP BY p.id, p.title, p.visibility, p.created_at ORDER BY p.created_at DESC""",
                (row, i) -> new Summary(row.getLong("id"), row.getString("title"), row.getString("visibility"),
                        row.getLong("videos"), row.getInt("has_video") == 1), videoId, userId);
    }

    public Summary create(long userId, NewPlaylist body) {
        String title = body == null || body.title() == null ? "" : body.title().strip();
        if (title.isEmpty() || title.length() > 150) throw new AuthFailure(400, "El título debe tener entre 1 y 150 caracteres.");
        String description = body.description() == null ? null : body.description().strip();
        if (description != null && description.length() > 5000) throw new AuthFailure(400, "La descripción admite hasta 5000 caracteres.");
        String visibility = body.visibility() == null ? "PUBLIC" : body.visibility();
        if (!Set.of("PUBLIC", "PRIVATE").contains(visibility)) throw new AuthFailure(400, "La visibilidad debe ser pública o privada.");
        Long id = jdbc.queryForObject("INSERT INTO playlists (owner_id, title, description, visibility) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, userId, title, description == null || description.isEmpty() ? null : description, visibility);
        return new Summary(id, title, visibility, 0, false);
    }

    public Detail get(long playlistId, Long viewerId) {
        List<Detail> found = jdbc.query("""
                SELECT p.id, p.title, p.description, p.visibility, p.owner_id, u.username
                FROM playlists p JOIN users u ON u.id = p.owner_id WHERE p.id = ?""", (row, i) -> {
            boolean mine = viewerId != null && viewerId == row.getLong("owner_id");
            return new Detail(row.getLong("id"), row.getString("title"), row.getString("description"),
                    row.getString("visibility"), row.getString("username"), mine, List.of());
        }, playlistId);
        if (found.isEmpty() || (!found.get(0).mine() && found.get(0).visibility().equals("PRIVATE")))
            throw new AuthFailure(404, "Esta playlist no existe o es privada.");
        Detail playlist = found.get(0);
        // The owner keeps seeing videos that later became private; everyone else only public published ones
        List<SearchService.VideoHit> videos = jdbc.query("""
                SELECT v.id, v.title, v.duration_sec, v.view_count, v.created_at, u.username
                FROM playlist_videos pv JOIN videos v ON v.id = pv.video_id JOIN users u ON u.id = v.owner_id
                WHERE pv.playlist_id = ? AND (? OR (v.status = 'PUBLISHED' AND v.visibility = 'PUBLIC'))
                ORDER BY pv.position""", (row, i) -> SearchService.hit(SearchDocument.of(row.getString("id"),
                row.getString("title"), row.getString("username"), List.of(), "", row.getDouble("duration_sec"),
                row.getLong("view_count"), row.getTimestamp("created_at").toInstant(), "/dog.png")),
                playlistId, playlist.mine());
        return new Detail(playlist.id(), playlist.title(), playlist.description(), playlist.visibility(), playlist.owner(),
                playlist.mine(), videos);
    }

    @Transactional
    public void delete(long userId, long playlistId) {
        requireOwner(userId, playlistId);
        jdbc.update("DELETE FROM playlists WHERE id = ?", playlistId);
    }

    @Transactional
    public void addVideo(long userId, long playlistId, String videoId) {
        requireOwner(userId, playlistId);
        long id = catalogueId(videoId);
        Integer visible = jdbc.queryForObject(
                "SELECT COUNT(*) FROM videos WHERE id = ? AND ((status = 'PUBLISHED' AND visibility = 'PUBLIC') OR owner_id = ?)",
                Integer.class, id, userId);
        if (visible == null || visible == 0) throw new AuthFailure(404, "El vídeo no existe.");
        jdbc.update("""
                INSERT INTO playlist_videos (playlist_id, video_id, position)
                SELECT ?, ?, COALESCE(MAX(position) + 1, 0) FROM playlist_videos WHERE playlist_id = ?
                ON CONFLICT DO NOTHING""", playlistId, id, playlistId); // already in the playlist: nothing to do
        jdbc.update("UPDATE playlists SET updated_at = now() WHERE id = ?", playlistId);
    }

    @Transactional
    public void removeVideo(long userId, long playlistId, String videoId) {
        requireOwner(userId, playlistId);
        jdbc.update("DELETE FROM playlist_videos WHERE playlist_id = ? AND video_id = ?", playlistId, catalogueId(videoId));
        jdbc.update("UPDATE playlists SET updated_at = now() WHERE id = ?", playlistId);
    }

    private void requireOwner(long userId, long playlistId) {
        List<Long> owner = jdbc.queryForList("SELECT owner_id FROM playlists WHERE id = ?", Long.class, playlistId);
        if (owner.isEmpty() || owner.get(0) != userId) throw new AuthFailure(404, "Esta playlist no existe o no es tuya.");
    }

    /** playlist_videos points to videos(id): anything else (e.g. a Studio upload id) is rejected. */
    private static long catalogueId(String videoId) {
        try {
            return Long.parseLong(videoId);
        } catch (NumberFormatException | NullPointerException ex) {
            throw new AuthFailure(400, "Este vídeo no se puede añadir a una playlist.");
        }
    }
}

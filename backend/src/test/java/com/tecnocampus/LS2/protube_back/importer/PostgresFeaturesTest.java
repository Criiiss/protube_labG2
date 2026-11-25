package com.tecnocampus.LS2.protube_back.importer;

import com.tecnocampus.LS2.protube_back.auth.AuthFailure;
import com.tecnocampus.LS2.protube_back.playlists.PlaylistService;
import com.tecnocampus.LS2.protube_back.search.SearchDocument;
import com.tecnocampus.LS2.protube_back.search.SearchIndexerAccess;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs the real Flyway migrations (V1..V5) on Postgres 16 and exercises the importer, the indexer and playlists. */
@Testcontainers
class PostgresFeaturesTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
    static JdbcTemplate jdbc;
    static VideoImporter importer;
    static PlaylistService playlists;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").load().migrate();
        var dataSource = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        importer = new VideoImporter(jdbc, new DataSourceTransactionManager(dataSource));
        playlists = new PlaylistService(jdbc);
    }

    static long count(String sql, Object... args) { return jdbc.queryForObject(sql, Long.class, args); }

    @Test void importsVideoGrabberFilesOnceAndSkipsBrokenOnes(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("7.json"), """
                { "id": 7, "width": 1280, "height": 720, "duration": 61.25, "title": "Jazz en directo",
                  "user": "Canal Jazz", "timestamp": 1700000000,
                  "meta": { "description": "Una sesión", "categories": ["Music"], "tags": ["Jazz", "jazz ", "Directo"],
                            "view_count": 1234, "like_count": 56, "channel": "Canal Jazz", "channel_follower_count": 9,
                            "comments": [ { "text": "¡Genial!", "author": "fan1", "timestamp": 1700000500, "like_count": 3 },
                                          { "text": "   ", "author": "fan2", "timestamp": 1700000600, "like_count": 0 } ] } }""");
        Files.writeString(dir.resolve("8.json"), "{ not json");
        Files.writeString(dir.resolve("notes.txt"), "ignored");

        assertThat(importer.schemaReady()).isTrue();
        assertThat(importer.importDirectory(dir)).isEqualTo(1);
        assertThat(importer.importDirectory(dir)).isZero(); // second start: already imported
        assertThat(importer.importDirectory(dir.resolve("missing"))).isZero();

        Map<String, Object> video = jdbc.queryForMap("SELECT * FROM videos WHERE file_name = '7.mp4'");
        assertThat(video).containsEntry("title", "Jazz en directo").containsEntry("thumbnail_name", "7.webp")
                .containsEntry("width", 1280).containsEntry("view_count", 1234L).containsEntry("like_count", 56L)
                .containsEntry("status", "PUBLISHED").containsEntry("visibility", "PUBLIC");
        long id = (Long) video.get("id");
        assertThat(jdbc.queryForList("SELECT t.name FROM video_tags vt JOIN tags t ON t.id = vt.tag_id WHERE vt.video_id = ? ORDER BY 1",
                String.class, id)).containsExactly("directo", "jazz");
        assertThat(count("SELECT COUNT(*) FROM video_categories WHERE video_id = ?", id)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM comments WHERE video_id = ?", id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT display_name FROM user_profiles p JOIN users u ON u.id = p.user_id WHERE u.username = 'fan1'",
                String.class)).isEqualTo("fan1");
    }

    @Test void importsSamplesOnce() {
        importer.importSamples();
        assertThat(count("SELECT COUNT(*) FROM videos WHERE file_name LIKE 'sample-%'")).isEqualTo(24);
        assertThat(importer.importSamples()).isZero();
    }

    @Test void importedChannelsDontTakeOverRealAccounts() {
        jdbc.update("INSERT INTO users (username, email, password_hash, auth_provider) VALUES ('Real', 'r@x.com', 'h', 'LOCAL')");
        long imported = importer.importedUser("Real");
        assertThat(jdbc.queryForObject("SELECT username FROM users WHERE id = ?", String.class, imported)).isEqualTo("yt_Real");
        assertThat(importer.importedUser("Real")).isEqualTo(imported);
        assertThat(jdbc.queryForObject("SELECT username FROM users WHERE id = ?", String.class, importer.importedUser("  ")))
                .isEqualTo("desconocido");
    }

    @Test void indexesPublicVideosStudioUploadsAndPlaylists() {
        importer.importSamples();
        jdbc.update("UPDATE videos SET visibility = 'PRIVATE' WHERE file_name = 'sample-1.mp4'");
        long owner = count("SELECT MIN(id) FROM users");
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS studio_videos (id VARCHAR(255) PRIMARY KEY, owner_id BIGINT NOT NULL,
                  filename VARCHAR(255) NOT NULL, checksum VARCHAR(64) NOT NULL, total_bytes BIGINT NOT NULL,
                  received_bytes BIGINT NOT NULL, status VARCHAR(255) NOT NULL, metadata TEXT NOT NULL,
                  created_at TIMESTAMPTZ NOT NULL, duration DOUBLE PRECISION NOT NULL)""");
        // studio_videos comes from V3 (Studio); the CREATE above only matters when V3 is absent
        String insert = """
                INSERT INTO studio_videos (id, owner_id, filename, checksum, total_bytes, received_bytes, status, metadata, created_at, duration)
                VALUES (?, ?, 'v.mp4', 'abc', 1, 1, ?, ?, now(), 30) ON CONFLICT DO NOTHING""";
        jdbc.update(insert, "up-1", owner, "READY",
                "{\"title\":\"Subido en Studio\",\"tags\":[\"studio\"],\"visibility\":\"PUBLIC\",\"draft\":false,\"publishAt\":null}");
        jdbc.update(insert, "up-2", owner, "PROCESSING", "{\"title\":\"Aún no\",\"visibility\":\"PUBLIC\"}");
        var pl = playlists.create(owner, new PlaylistService.NewPlaylist("Mis favoritos", null, "PUBLIC"));
        playlists.create(owner, new PlaylistService.NewPlaylist("Secreta", null, "PRIVATE"));
        playlists.addVideo(owner, pl.id(), jdbc.queryForObject("SELECT id FROM videos WHERE file_name = 'sample-2.mp4'", String.class));

        List<SearchDocument> videos = SearchIndexerAccess.videos(jdbc);
        assertThat(videos).extracting(SearchDocument::title).contains("Subido en Studio", "Lo-fi para estudiar y concentrarse")
                .doesNotContain("Aún no", "Guitarra acústica para principiantes: primeros acordes");
        assertThat(SearchIndexerAccess.playlists(jdbc)).anySatisfy(p -> {
            assertThat(p.title()).isEqualTo("Mis favoritos");
            assertThat(p.videoCount()).isEqualTo(1);
        }).noneMatch(p -> p.title().equals("Secreta"));
    }

    @Test void playlistsBelongToTheirOwner() {
        importer.importSamples();
        long ana = jdbc.queryForObject("INSERT INTO users (username, auth_provider) VALUES ('ana_pl', 'IMPORTED') RETURNING id", Long.class);
        long bob = jdbc.queryForObject("INSERT INTO users (username, auth_provider) VALUES ('bob_pl', 'IMPORTED') RETURNING id", Long.class);
        String video = jdbc.queryForObject("SELECT id FROM videos WHERE file_name = 'sample-3.mp4'", String.class);

        var mine = playlists.create(ana, new PlaylistService.NewPlaylist("  Para el viaje ", "música", null));
        assertThat(mine.title()).isEqualTo("Para el viaje");
        playlists.addVideo(ana, mine.id(), video);
        playlists.addVideo(ana, mine.id(), video); // twice: still once
        assertThat(playlists.mine(ana, Long.valueOf(video))).singleElement().satisfies(s -> {
            assertThat(s.videoCount()).isEqualTo(1);
            assertThat(s.hasVideo()).isTrue();
        });
        assertThat(playlists.get(mine.id(), null).videos()).hasSize(1);
        assertThat(playlists.get(mine.id(), bob).mine()).isFalse();

        assertThatThrownBy(() -> playlists.addVideo(bob, mine.id(), video)).isInstanceOf(AuthFailure.class);
        assertThatThrownBy(() -> playlists.addVideo(ana, mine.id(), "up-1")).hasMessageContaining("Studio");
        assertThatThrownBy(() -> playlists.addVideo(ana, mine.id(), "999999")).hasMessageContaining("no existe");
        assertThatThrownBy(() -> playlists.create(ana, new PlaylistService.NewPlaylist(" ", null, null))).hasMessageContaining("título");
        assertThatThrownBy(() -> playlists.create(ana, new PlaylistService.NewPlaylist("x", "d".repeat(5001), null))).hasMessageContaining("descripción");
        assertThatThrownBy(() -> playlists.create(ana, new PlaylistService.NewPlaylist("x", null, "FRIENDS"))).hasMessageContaining("visibilidad");

        var hidden = playlists.create(ana, new PlaylistService.NewPlaylist("Privada", null, "PRIVATE"));
        assertThatThrownBy(() -> playlists.get(hidden.id(), bob)).isInstanceOf(AuthFailure.class);
        assertThat(playlists.get(hidden.id(), ana).mine()).isTrue();

        playlists.removeVideo(ana, mine.id(), video);
        assertThat(playlists.get(mine.id(), ana).videos()).isEmpty();
        assertThatThrownBy(() -> playlists.delete(bob, mine.id())).isInstanceOf(AuthFailure.class);
        playlists.delete(ana, mine.id());
        assertThatThrownBy(() -> playlists.get(mine.id(), ana)).isInstanceOf(AuthFailure.class);
    }
}

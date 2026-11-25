package com.tecnocampus.LS2.protube_back.importer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/** Imports the videoGrabber JSON files (and the bundled sample videos) into Postgres. Already imported files are skipped. */
@Service
public class VideoImporter {
    private static final Logger LOG = LoggerFactory.getLogger(VideoImporter.class);

    /** Shape written by tooling/videoGrabber/main.py */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Grabbed(Integer width, Integer height, Double duration, String title, String user, Long timestamp, Meta meta) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(String description, List<String> categories, List<String> tags, Long view_count, Long like_count,
                       List<GrabbedComment> comments) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GrabbedComment(String text, String author, Long timestamp, Long like_count) {}
    record Sample(String title, String channel, List<String> tags, String description, double durationSec, long views, int daysAgo) {}

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ObjectMapper json = new ObjectMapper();

    public VideoImporter(JdbcTemplate jdbc, PlatformTransactionManager transactions) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(transactions);
    }

    /** The import needs the Flyway schema, which only exists with the prod profile (Postgres). */
    public boolean schemaReady() {
        return Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection -> {
            try (var tables = connection.getMetaData().getTables(null, null, "%", new String[] { "TABLE" })) {
                while (tables.next()) if ("videos".equalsIgnoreCase(tables.getString("TABLE_NAME"))) return true;
                return false;
            }
        }));
    }

    /** Imports every {@code <id>.json} in the store dir. A broken file is logged and skipped. */
    public int importDirectory(Path dir) {
        if (!Files.isDirectory(dir)) return 0;
        int imported = 0;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(f -> f.getFileName().toString().matches("\\d+\\.json")).sorted().toList()) {
                String base = file.getFileName().toString().replace(".json", "");
                try {
                    Grabbed grabbed = json.readValue(file.toFile(), Grabbed.class);
                    if (importVideo(base, grabbed, base + ".webp")) imported++;
                } catch (IOException | RuntimeException ex) {
                    LOG.warn("No se pudo importar {}: {}", file.getFileName(), ex.getMessage());
                }
            }
        } catch (IOException ex) {
            LOG.warn("No se pudo leer la carpeta de vídeos {}: {}", dir, ex.getMessage());
        }
        return imported;
    }

    /** Sample catalogue so the app has content before anyone runs the videoGrabber. */
    public int importSamples() {
        try (InputStream in = new ClassPathResource("samples/sample-videos.json").getInputStream()) {
            List<Sample> samples = json.readValue(in, new TypeReference<>() {});
            int imported = 0;
            for (int i = 0; i < samples.size(); i++) {
                Sample s = samples.get(i);
                long when = Instant.now().minus(Duration.ofDays(s.daysAgo())).getEpochSecond();
                Grabbed grabbed = new Grabbed(null, null, s.durationSec(), s.title(), s.channel(), when,
                        new Meta(s.description(), List.of(), s.tags(), s.views(), 0L, List.of()));
                if (importVideo("sample-" + (i + 1), grabbed, null)) imported++;
            }
            return imported;
        } catch (IOException ex) {
            throw new IllegalStateException("samples/sample-videos.json no se puede leer", ex);
        }
    }

    /** One transaction per video: either everything (video, tags, comments...) is saved or nothing. */
    boolean importVideo(String base, Grabbed g, String thumbnail) {
        String fileName = base + ".mp4";
        if (exists("SELECT COUNT(*) FROM videos WHERE file_name = ?", fileName)) return false;
        if (g.title() == null || g.title().isBlank()) throw new IllegalArgumentException("el vídeo no tiene título");
        Meta meta = g.meta() == null ? new Meta(null, null, null, null, null, null) : g.meta();
        tx.executeWithoutResult(status -> {
            long owner = importedUser(g.user());
            Long videoId = jdbc.queryForObject("""
                    INSERT INTO videos (owner_id, title, description, file_name, thumbnail_name, width, height,
                                        duration_sec, view_count, like_count, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class,
                    owner, cut(g.title().strip(), 255), meta.description(), fileName, thumbnail,
                    positiveOrNull(g.width()), positiveOrNull(g.height()),
                    g.duration() == null ? null : Math.max(0, g.duration()),
                    Math.max(0, Objects.requireNonNullElse(meta.view_count(), 0L)),
                    Math.max(0, Objects.requireNonNullElse(meta.like_count(), 0L)), time(g.timestamp()));
            link(videoId, meta.tags(), "tags", "video_tags", "tag_id");
            link(videoId, meta.categories(), "categories", "video_categories", "category_id");
            for (GrabbedComment c : Objects.requireNonNullElse(meta.comments(), List.<GrabbedComment>of())) {
                if (c.text() == null || c.text().isBlank()) continue;
                jdbc.update("INSERT INTO comments (video_id, author_id, content, like_count, created_at) VALUES (?, ?, ?, ?, ?)",
                        videoId, importedUser(c.author()), c.text(),
                        Math.max(0, Objects.requireNonNullElse(c.like_count(), 0L)), time(c.timestamp()));
            }
        });
        return true;
    }

    /** Reuses the IMPORTED user with that name; if a real account already owns the name, uses "yt_<name>". */
    long importedUser(String name) {
        String clean = name == null || name.isBlank() ? "desconocido" : name.strip();
        for (String username : List.of(cut(clean, 100), cut("yt_" + clean, 100))) {
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT id, auth_provider FROM users WHERE username = ?", username);
            if (rows.isEmpty()) {
                Long id = jdbc.queryForObject("INSERT INTO users (username, auth_provider) VALUES (?, 'IMPORTED') RETURNING id",
                        Long.class, username);
                jdbc.update("INSERT INTO user_profiles (user_id, display_name) VALUES (?, ?)", id, cut(clean, 150));
                return id;
            }
            if ("IMPORTED".equals(rows.get(0).get("auth_provider"))) return ((Number) rows.get(0).get("id")).longValue();
        }
        throw new IllegalStateException("los nombres " + clean + " y yt_" + clean + " ya pertenecen a cuentas reales");
    }

    /** Tags and categories are stored lowercase (chk_*_name_lowercase) and reused if they already exist. */
    private void link(long videoId, List<String> names, String table, String joinTable, String column) {
        if (names == null) return;
        Set<String> clean = new LinkedHashSet<>();
        for (String name : names) if (name != null && !name.isBlank()) clean.add(cut(name.strip().toLowerCase(Locale.ROOT), 100));
        for (String name : clean) {
            jdbc.update("INSERT INTO " + table + " (name) VALUES (?) ON CONFLICT (name) DO NOTHING", name);
            Long id = jdbc.queryForObject("SELECT id FROM " + table + " WHERE name = ?", Long.class, name);
            jdbc.update("INSERT INTO " + joinTable + " (video_id, " + column + ") VALUES (?, ?) ON CONFLICT DO NOTHING", videoId, id);
        }
    }

    private boolean exists(String sql, Object arg) {
        Integer count = jdbc.queryForObject(sql, Integer.class, arg);
        return count != null && count > 0;
    }
    private static Timestamp time(Long epochSeconds) {
        return Timestamp.from(epochSeconds == null ? Instant.now() : Instant.ofEpochSecond(epochSeconds));
    }
    private static Integer positiveOrNull(Integer value) { return value == null || value <= 0 ? null : value; }
    private static String cut(String text, int max) { return text.length() > max ? text.substring(0, max) : text; }
}

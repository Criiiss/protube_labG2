package com.tecnocampus.LS2.protube_back.search;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SearchIndexerTest {
    @Test void indexesOnlyPublicPublishedNonAdultVideosWithTheirTags() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:search-indexer;DB_CLOSE_DELAY=-1"));
        SearchIndexer indexer = new SearchIndexer(jdbc, mock(SearchService.class));
        assertThat(indexer.videos()).isEmpty(); // no tables yet (dev profile)
        assertThat(indexer.playlists()).isEmpty();

        jdbc.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, username VARCHAR(100))");
        jdbc.execute("""
                CREATE TABLE videos (id BIGINT PRIMARY KEY, owner_id BIGINT, title VARCHAR(255), description TEXT,
                  duration_sec NUMERIC(10,2), view_count BIGINT, created_at TIMESTAMP WITH TIME ZONE,
                  status VARCHAR(20), visibility VARCHAR(20), thumbnail_name VARCHAR(255), age_restricted BOOLEAN DEFAULT FALSE)""");
        jdbc.execute("CREATE TABLE tags (id BIGINT PRIMARY KEY, name VARCHAR(100))");
        jdbc.execute("CREATE TABLE video_tags (video_id BIGINT, tag_id BIGINT)");
        jdbc.update("INSERT INTO users VALUES (1, 'ana')");
        String insert = """
                INSERT INTO videos (id, owner_id, title, description, duration_sec, view_count, created_at, status, visibility,
                                    thumbnail_name, age_restricted)
                VALUES (?, 1, ?, 'd', 61.5, 7, CURRENT_TIMESTAMP, ?, ?, ?, ?)""";
        jdbc.update(insert, 10, "Visible", "PUBLISHED", "PUBLIC", "/api/studio/play/x/thumbnail", false);
        jdbc.update(insert, 11, "Privado", "PUBLISHED", "PRIVATE", null, false);
        jdbc.update(insert, 12, "Borrador", "DRAFT", "PUBLIC", null, false);
        jdbc.update(insert, 13, "Solo adultos", "PUBLISHED", "PUBLIC", null, true);
        jdbc.update(insert, 14, "Oculto", "PUBLISHED", "UNLISTED", null, false);
        jdbc.update("INSERT INTO tags VALUES (1, 'jazz')");
        jdbc.update("INSERT INTO video_tags VALUES (10, 1)");

        List<SearchDocument> docs = indexer.videos();

        assertThat(docs).singleElement().satisfies(doc -> {
            assertThat(doc.id()).isEqualTo("10");
            assertThat(doc.channel()).isEqualTo("ana");
            assertThat(doc.tags()).containsExactly("jazz");
            assertThat(doc.durationSec()).isEqualTo(61.5);
            assertThat(doc.thumbnail()).isEqualTo("/api/studio/play/x/thumbnail");
        });
        assertThat(SearchIndexer.thumbnail("3.webp")).isEqualTo("/media/3.webp");
        assertThat(SearchIndexer.thumbnail(" ")).isEqualTo("/dog.png");
    }
}

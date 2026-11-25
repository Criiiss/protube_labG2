package com.tecnocampus.LS2.protube_back.search;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SearchIndexerTest {
    @Test void indexesOnlyPublicPublishedVideosWithTheirTags() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:search-indexer;DB_CLOSE_DELAY=-1"));
        SearchIndexer indexer = new SearchIndexer(jdbc, mock(SearchService.class));
        assertThat(indexer.videos()).isEmpty(); // no tables yet (dev profile)
        assertThat(indexer.playlists()).isEmpty();

        jdbc.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, username VARCHAR(100))");
        jdbc.execute("""
                CREATE TABLE videos (id BIGINT PRIMARY KEY, owner_id BIGINT, title VARCHAR(255), description TEXT,
                  duration_sec NUMERIC(10,2), view_count BIGINT, created_at TIMESTAMP WITH TIME ZONE,
                  status VARCHAR(20), visibility VARCHAR(20))""");
        jdbc.execute("CREATE TABLE tags (id BIGINT PRIMARY KEY, name VARCHAR(100))");
        jdbc.execute("CREATE TABLE video_tags (video_id BIGINT, tag_id BIGINT)");
        jdbc.update("INSERT INTO users VALUES (1, 'ana')");
        jdbc.update("INSERT INTO videos VALUES (10, 1, 'Visible', 'd', 61.5, 7, CURRENT_TIMESTAMP, 'PUBLISHED', 'PUBLIC')");
        jdbc.update("INSERT INTO videos VALUES (11, 1, 'Privado', 'd', 10, 1, CURRENT_TIMESTAMP, 'PUBLISHED', 'PRIVATE')");
        jdbc.update("INSERT INTO videos VALUES (12, 1, 'Borrador', 'd', 10, 1, CURRENT_TIMESTAMP, 'DRAFT', 'PUBLIC')");
        jdbc.update("INSERT INTO tags VALUES (1, 'jazz')");
        jdbc.update("INSERT INTO video_tags VALUES (10, 1)");

        List<SearchDocument> docs = indexer.videos();

        assertThat(docs).singleElement().satisfies(doc -> {
            assertThat(doc.id()).isEqualTo("10");
            assertThat(doc.channel()).isEqualTo("ana");
            assertThat(doc.tags()).containsExactly("jazz");
            assertThat(doc.durationSec()).isEqualTo(61.5);
        });
    }
}

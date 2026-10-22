package com.tecnocampus.LS2.protube_back.auth;
import com.tecnocampus.LS2.protube_back.controller.CatalogController;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;
class CatalogTest {
    @Test void emptyDatabaseAndExistingVideoMetadataHaveSeparateStates() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:catalog;DB_CLOSE_DELAY=-1", "sa", ""));
        var controller = new CatalogController(jdbc); assertTrue(controller.videos().isEmpty());
        jdbc.execute("CREATE TABLE users (id bigint primary key, username varchar(50))");
        jdbc.execute("CREATE TABLE videos (id bigint primary key, owner_id bigint, title varchar(200), view_count bigint, duration_sec decimal, created_at timestamp)");
        jdbc.update("INSERT INTO users VALUES (1,'creator')");
        jdbc.update("INSERT INTO videos VALUES (1,1,'A real video',42,125,CURRENT_TIMESTAMP)");
        var video = controller.videos().getFirst(); assertEquals("A real video", video.title()); assertEquals("creator", video.channel()); assertEquals("2:05", video.duration()); assertEquals(42, video.viewCount()); assertNotNull(video.createdAt());
    }
}

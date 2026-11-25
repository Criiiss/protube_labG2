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
        jdbc.execute("CREATE TABLE users (id bigint primary key, username varchar(50), display_name varchar(100), avatar_url varchar(255))");
        jdbc.execute("CREATE TABLE user_profiles (user_id bigint primary key, display_name varchar(100), avatar_url varchar(255))");
        jdbc.execute("CREATE TABLE videos (id bigint primary key, owner_id bigint, title varchar(200), description varchar(500), view_count bigint, duration_sec decimal, created_at timestamp, file_name varchar(255), thumbnail_name varchar(255), status varchar(30), visibility varchar(30), age_restricted boolean default false)");
        jdbc.update("INSERT INTO users (id, username) VALUES (1,'creator')");
        jdbc.update("INSERT INTO videos (id, owner_id, title, description, view_count, duration_sec, created_at, file_name, thumbnail_name, status, visibility) VALUES (1,1,'A real video',null,42,125,CURRENT_TIMESTAMP,null,null,'PUBLISHED','PUBLIC')");
        var video = controller.videos().getFirst(); assertEquals("A real video", video.title()); assertEquals("creator", video.channel()); assertEquals("2:05", video.duration()); assertEquals(42, video.viewCount()); assertNotNull(video.createdAt());
    }

    @Test
    void returnsEmptyWhenVideosTableDoesNotExist() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:catalog_empty;DB_CLOSE_DELAY=-1", "sa", ""));
        var controller = new CatalogController(jdbc);
        assertTrue(controller.videos().isEmpty());
    }

    @Test
    void returnsVideosWhenVideosTableExists() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:catalog_videos;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE users (id bigint primary key, username varchar(50), display_name varchar(100), avatar_url varchar(255))");
        jdbc.execute("CREATE TABLE user_profiles (user_id bigint primary key, display_name varchar(100), avatar_url varchar(255))");
        jdbc.execute("""
            CREATE TABLE videos (
                id bigint primary key,
                owner_id bigint,
                title varchar(200),
                description varchar(500),
                view_count bigint,
                duration_sec decimal,
                created_at timestamp,
                file_name varchar(255),
                thumbnail_name varchar(255),
                status varchar(30),
                visibility varchar(30),
                age_restricted boolean default false
            )
            """);
        jdbc.update("INSERT INTO users (id, username) VALUES (1, 'creator')");
        jdbc.update("""
            INSERT INTO videos (id, owner_id, title, description, view_count, duration_sec, created_at, file_name, thumbnail_name, status, visibility)
            VALUES (1, 1, 'A real video', null, 42, 125, CURRENT_TIMESTAMP, null, null, 'PUBLISHED', 'PUBLIC')
            """);
        var controller = new CatalogController(jdbc);
        assertEquals(1, controller.videos().size());
    }
    @Test
    void findsVideosTableAfterOtherTables() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:catalog_multiple_tables;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE another_table (id bigint primary key)");
        jdbc.execute("CREATE TABLE users (id bigint primary key, username varchar(50), display_name varchar(100), avatar_url varchar(255))");
        jdbc.execute("CREATE TABLE user_profiles (user_id bigint primary key, display_name varchar(100), avatar_url varchar(255))");
        jdbc.execute("""
            CREATE TABLE videos (
                id bigint primary key,
                owner_id bigint,
                title varchar(200),
                description varchar(500),
                view_count bigint,
                duration_sec decimal,
                created_at timestamp,
                file_name varchar(255),
                thumbnail_name varchar(255),
                status varchar(30),
                visibility varchar(30),
                age_restricted boolean default false
            )
            """);

        jdbc.update("INSERT INTO users (id, username) VALUES (1, 'creator')");
        jdbc.update("""
            INSERT INTO videos (id, owner_id, title, description, view_count, duration_sec, created_at, file_name, thumbnail_name, status, visibility)
            VALUES (1, 1, 'Video', null, 10, 60, CURRENT_TIMESTAMP, null, null, 'PUBLISHED', 'PUBLIC')
            """);

        var controller = new CatalogController(jdbc);
        assertEquals(1, controller.videos().size());
    }
}

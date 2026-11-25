package com.tecnocampus.LS2.protube_back.videos;

import com.tecnocampus.LS2.protube_back.videos.adapter.in.web.ChannelVideoResponseDTO;
import com.tecnocampus.LS2.protube_back.videos.application.service.ChannelVideoService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChannelVideoServiceTest {
    @Test
    void mapsPublicVideosAndBuildsMediaUrls() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ResultSet resultSet = videoResultSet("clip.mp4", "thumb.webp", "avatar.png", 125, 42, true);
        when(jdbc.query(anyString(), any(RowMapper.class), anyLong()))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 0));
                });

        ChannelVideoResponseDTO video = new ChannelVideoService(jdbc).getPublicVideosByUserId(7L).getFirst();

        assertEquals("7", video.id());
        assertEquals("2:05", video.duration());
        assertEquals("42 visualizaciones", video.views());
        assertEquals("/media/clip.mp4", video.videoUrl());
        assertEquals("/media/thumb.webp", video.thumbnail());
        assertEquals("/media/avatar.png", video.avatarUrl());
        assertNotNull(video.createdAt());
    }

    @Test
    void mapsOwnerVideoWithFallbacksAndAbsoluteUrls() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ResultSet resultSet = videoResultSet("", null, "https://avatar.test/a", 0, 0, false);
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any()))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 0));
                });

        ChannelVideoResponseDTO video = new ChannelVideoService(jdbc).getOwnerVideosByStatus(7L, "DRAFT").getFirst();

        assertEquals("0:00", video.duration());
        assertEquals("/dog.png", video.thumbnail());
        assertNull(video.videoUrl());
        assertEquals("https://avatar.test/a", video.avatarUrl());
        assertNull(video.createdAt());
    }

    @Test
    void preservesDataAndHttpMediaUrls() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ResultSet resultSet = videoResultSet("https://video.test/v", "http://thumb.test/t", "data:image/png;base64,x", 61, 1, false);
        when(jdbc.query(anyString(), any(RowMapper.class), anyLong()))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 0));
                });

        ChannelVideoResponseDTO video = new ChannelVideoService(jdbc).getPublicVideosByUserId(1L).getFirst();

        assertEquals("https://video.test/v", video.videoUrl());
        assertEquals("http://thumb.test/t", video.thumbnail());
        assertEquals("data:image/png;base64,x", video.avatarUrl());
        assertEquals("1:01", video.duration());
    }

    private ResultSet videoResultSet(String file, String thumbnail, String avatar,
                                     long duration, long views, boolean withDate) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("duration_sec")).thenReturn(duration);
        when(rs.getLong("view_count")).thenReturn(views);
        when(rs.getString("id")).thenReturn("7");
        when(rs.getString("title")).thenReturn("Title");
        when(rs.getString("username")).thenReturn("owner");
        when(rs.getString("display_name")).thenReturn("Owner");
        when(rs.getString("description")).thenReturn("Description");
        when(rs.getString("avatar_url")).thenReturn(avatar);
        when(rs.getString("thumbnail_name")).thenReturn(thumbnail);
        when(rs.getString("file_name")).thenReturn(file);
        when(rs.getString("status")).thenReturn("PUBLISHED");
        when(rs.getString("visibility")).thenReturn("PUBLIC");
        when(rs.getTimestamp("created_at")).thenReturn(withDate ? Timestamp.from(Instant.parse("2024-01-01T00:00:00Z")) : null);
        return rs;
    }
}

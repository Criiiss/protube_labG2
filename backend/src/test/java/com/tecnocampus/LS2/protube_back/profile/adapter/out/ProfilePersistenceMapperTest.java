package com.tecnocampus.LS2.protube_back.profile.adapter.out;
import com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence.ProfileJpaEntity;
import com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence.ProfilePersistenceMapper;
import com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence.UserJpaEntity;
import com.tecnocampus.LS2.protube_back.profile.domain.model.PrivacyVisibility;
import com.tecnocampus.LS2.protube_back.profile.domain.model.ProfilePrivacySettings;
import com.tecnocampus.LS2.protube_back.profile.domain.model.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

public class ProfilePersistenceMapperTest {
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-10T08:00:00Z");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-10-11T09:00:00Z");

    private final ProfilePersistenceMapper mapper = new ProfilePersistenceMapper(Clock.fixed(UPDATED_AT.toInstant(), ZoneOffset.UTC));


    @Test
    public void mapsDomainToJpaEntity() {
        UserJpaEntity user = new UserJpaEntity((long) 1D,"maria_23","maria@exemple.com","password","LOCAL",null,CREATED_AT,UPDATED_AT);
        ProfilePrivacySettings settings = new ProfilePrivacySettings(PrivacyVisibility.PUBLIC, PrivacyVisibility.PRIVATE);
        UserProfile profile = new UserProfile((long) 1D,"maria_23","Maria","/avatar.png","Biography","/banner.png",settings,UPDATED_AT);
        ProfileJpaEntity entity = mapper.toEntity(profile, user);
        assertEquals((long) 1D, entity.getUserId());
        assertEquals(user, entity.getUser());
        assertEquals("Maria", entity.getDisplayName());
        assertEquals("/avatar.png", entity.getAvatarUrl());
        assertEquals("Biography", entity.getBiography());
        assertEquals("/banner.png", entity.getBannerUrl());
        assertEquals(UPDATED_AT, entity.getUpdatedAt());
        assertEquals(PrivacyVisibility.PUBLIC, entity.getSubscriptionsVisibility());
        assertEquals(PrivacyVisibility.PRIVATE, entity.getLikedVideosVisibility());
    }

    @Test
    public void mapsJpaEntityToDomain() {
        UserJpaEntity user = new UserJpaEntity((long) 1D,"maria_23","maria@exemple.com","password","LOCAL",null,CREATED_AT,UPDATED_AT);
        ProfileJpaEntity entity = new ProfileJpaEntity(user,"Maria","/avatar.png","Biography","/banner.png",UPDATED_AT,PrivacyVisibility.PUBLIC,PrivacyVisibility.PRIVATE);
        UserProfile profile = mapper.toDomain(entity);
        assertEquals((long) 1D, profile.getUserId());
        assertEquals("maria_23", profile.getUsername());
        assertEquals("Maria", profile.getDisplayName());
        assertEquals("/avatar.png", profile.getAvatarUrl());
        assertEquals("Biography", profile.getBiography());
        assertEquals("/banner.png", profile.getBannerUrl());
        assertEquals(CREATED_AT, profile.getCreatedAt());
        assertEquals(PrivacyVisibility.PUBLIC, profile.getPrivacySettings().getSubscriptionsVisibility());
        assertEquals(PrivacyVisibility.PRIVATE, profile.getPrivacySettings().getLikedVideosVisibility());
    }

    @Test
    void mapsDomainToEntityUsingProvidedClock() {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        var mapper = new ProfilePersistenceMapper(Clock.fixed(now.toInstant(), ZoneOffset.UTC));
        var user = new UserJpaEntity(1L, "user", "u@example.com", "hash", "LOCAL", null, now, now);
        var profile = new UserProfile(1L, "user", "Display", "/a", "Bio", "/b",
                new ProfilePrivacySettings(PrivacyVisibility.PUBLIC, PrivacyVisibility.PRIVATE), now);
        var entity = mapper.toEntity(profile, user);
        assertEquals("Display", entity.getDisplayName());
        assertEquals(now, entity.getUpdatedAt());
        assertEquals(PrivacyVisibility.PUBLIC, entity.getSubscriptionsVisibility());
    }

    @Test
    void mapsEntityToDomain() {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        var user = new UserJpaEntity(1L, "user", "u@example.com", "hash", "LOCAL", null, now, now);
        var entity = new ProfileJpaEntity(user, "Display", "/a", "Bio", "/b", now,
                PrivacyVisibility.PUBLIC, PrivacyVisibility.PRIVATE);
        var profile = new ProfilePersistenceMapper(Clock.systemUTC()).toDomain(entity);
        assertEquals(1L, profile.getUserId());
        assertEquals("user", profile.getUsername());
        assertTrue(profile.getPrivacySettings().isSubscriptionsVisible());
        assertFalse(profile.getPrivacySettings().isLikedVideosVisible());
    }
}

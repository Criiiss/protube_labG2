package com.tecnocampus.LS2.protube_back.profile.adapter.out;
import com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence.ProfileJpaEntity;
import com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence.ProfilePersistenceMapper;
import com.tecnocampus.LS2.protube_back.profile.domain.model.PrivacyVisibility;
import com.tecnocampus.LS2.protube_back.profile.domain.model.ProfilePrivacySettings;
import com.tecnocampus.LS2.protube_back.profile.domain.model.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProfilePersistenceMapperTest {
    private final ProfilePersistenceMapper mapper = new ProfilePersistenceMapper();

    @Test
    public void mapsDomainToJpaEntity() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-10-19T12:00:00Z");
        ProfilePrivacySettings settings = new ProfilePrivacySettings(PrivacyVisibility.PUBLIC, PrivacyVisibility.PRIVATE);
        UserProfile profile = new UserProfile((long) 1D,"maria_23","Maria","/avatar.png","Biography","/banner.png",settings,createdAt);
        ProfileJpaEntity entity = mapper.toEntity(profile);
        assertEquals((long) 1D, entity.getId());
        assertEquals("maria_23", entity.getUsername());
        assertEquals("Maria", entity.getDisplayName());
        assertEquals("/avatar.png", entity.getAvatarUrl());
        assertEquals("Biography", entity.getBiography());
        assertEquals("/banner.png", entity.getBannerUrl());
        assertEquals(createdAt, entity.getCreatedAt());
        assertEquals(PrivacyVisibility.PUBLIC, entity.getSubscriptionsVisibility());
        assertEquals(PrivacyVisibility.PRIVATE, entity.getLikedVideosVisibility());
    }

    @Test
    public void mapsJpaEntityToDomain() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-10-19T12:00:00Z");
        ProfileJpaEntity entity = new ProfileJpaEntity((long) 1D,"maria_23","Maria","/avatar.png","Biography","/banner.png",createdAt,PrivacyVisibility.PUBLIC,PrivacyVisibility.PRIVATE);

        UserProfile profile = mapper.toDomain(entity);
        assertEquals((long) 1D, profile.getUserId());
        assertEquals("maria_23", profile.getUsername());
        assertEquals("Maria", profile.getDisplayName());
        assertEquals("/avatar.png", profile.getAvatarUrl());
        assertEquals("Biography", profile.getBiography());
        assertEquals("/banner.png", profile.getBannerUrl());
        assertEquals(createdAt, profile.getCreatedAt());
        assertEquals(PrivacyVisibility.PUBLIC, profile.getPrivacySettings().getSubscriptionsVisibility());
        assertEquals(PrivacyVisibility.PRIVATE, profile.getPrivacySettings().getLikedVideosVisibility());
    }
}

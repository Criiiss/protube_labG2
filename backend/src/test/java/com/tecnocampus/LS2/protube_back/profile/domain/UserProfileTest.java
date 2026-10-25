package com.tecnocampus.LS2.protube_back.profile.domain;

import com.tecnocampus.LS2.protube_back.profile.domain.exception.InvalidUsernameException;
import com.tecnocampus.LS2.protube_back.profile.domain.model.ProfilePrivacySettings;
import com.tecnocampus.LS2.protube_back.profile.domain.model.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class UserProfileTest {
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    private UserProfile createProfile() {
        return new UserProfile((long) 1D,"maria_23","Maria","/avatar.png","Biography","/banner.png",ProfilePrivacySettings.defaultSettings(),CREATED_AT);
    }
    @Test
    void createsProfileWithValidData() {
        UserProfile profile = createProfile();
        assertEquals((long) 1D, profile.getUserId());
        assertEquals("maria_23", profile.getUsername());
        assertEquals("Maria", profile.getDisplayName());
        assertEquals("/avatar.png", profile.getAvatarUrl());
        assertEquals("/banner.png", profile.getBannerUrl());
        assertEquals(CREATED_AT, profile.getCreatedAt());
    }
    @Test
    void normalizesUsername() {
        UserProfile profile = new UserProfile((long) 1D," Maria_23 ","Maria",null, null, null, null,CREATED_AT);
        assertEquals("maria_23", profile.getUsername());
    }
    @Test
    void usesPrivatePrivacySettingsByDefault() {
        UserProfile profile = new UserProfile((long) 1D," Maria_23 ","Maria",null, null, null, null,CREATED_AT);
        assertFalse(profile.getPrivacySettings().isSubscriptionsVisible());
        assertFalse(profile.getPrivacySettings().isLikedVideosVisible());

    }
    @Test
    void rejectsInvalidUsername() {
        assertThrows(InvalidUsernameException.class,() -> new UserProfile((long) 1D,"invalid username","Maria",null, null, null, null,CREATED_AT));
    }

    @Test
    void updatesProfile() {
        UserProfile profile = createProfile();
        profile.updateProfile("New name","New biography","/new-avatar.png", "/new-banner.png");
        assertEquals("New name", profile.getDisplayName());
        assertEquals("New biography", profile.getBiography());
        assertEquals("/new-avatar.png", profile.getAvatarUrl());
        assertEquals("/new-banner.png", profile.getBannerUrl());
    }

    @Test
    void changesUsername() {
        UserProfile profile = createProfile();
        profile.changeUserName("new_name");
        assertEquals("new_name", profile.getUsername());
    }

    @Test
    void removesAvatar() {
        UserProfile profile = createProfile();
        profile.removeAvatar();
        assertNull(profile.getAvatarUrl());
    }

    @Test
    void removesBanner() {
        UserProfile profile = createProfile();
        profile.removeBanner();
        assertNull(profile.getBannerUrl());
    }
    @Test
    void updatesPrivacySettings() {
        UserProfile profile = createProfile();
        ProfilePrivacySettings settings =ProfilePrivacySettings.defaultSettings();
        settings.showSubscriptions();
        profile.updatePrivacySettings(settings);
        assertTrue(profile.getPrivacySettings().isSubscriptionsVisible());
    }
    @Test
    void rejectsNullCreatedAt() {
        assertThrows(IllegalArgumentException.class, () -> new UserProfile((long) 1D," Maria_23 ","Maria",null, null, null, null,null) );
    }

    @Test
    void usesDefaultPrivacySettingsWhenSettingsAreNull() {
        UserProfile profile = new UserProfile((long) 1D," Maria_23 ","Maria",null, null, null, null,CREATED_AT);
        assertEquals(ProfilePrivacySettings.defaultSettings().getSubscriptionsVisibility(), profile.getPrivacySettings().getSubscriptionsVisibility());
        assertEquals(ProfilePrivacySettings.defaultSettings().getLikedVideosVisibility(),profile.getPrivacySettings().getLikedVideosVisibility());
    }
    @Test
    void rejectsNullUserId() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new UserProfile(null, "maria_23", "Maria", null, null, null, null, CREATED_AT));
        assertEquals("User id cannot be null", exception.getMessage());
    }



}

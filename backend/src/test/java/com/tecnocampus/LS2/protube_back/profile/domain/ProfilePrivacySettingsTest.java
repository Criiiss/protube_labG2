package com.tecnocampus.LS2.protube_back.profile.domain;

import com.tecnocampus.LS2.protube_back.profile.domain.model.PrivacyVisibility;
import com.tecnocampus.LS2.protube_back.profile.domain.model.ProfilePrivacySettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ProfilePrivacySettingsTest {
    @Test
    void defaultSettingsArePrivate() {
        ProfilePrivacySettings settings = ProfilePrivacySettings.defaultSettings();
        assertEquals(PrivacyVisibility.PRIVATE,settings.getSubscriptionsVisibility());
        assertEquals(PrivacyVisibility.PRIVATE,settings.getLikedVideosVisibility());
        assertFalse(settings.isSubscriptionsVisible());
        assertFalse(settings.isLikedVideosVisible());
    }

    @Test
    void canShowSubscriptions() {
        ProfilePrivacySettings settings =ProfilePrivacySettings.defaultSettings();
        settings.showSubscriptions();
        assertTrue(settings.isSubscriptionsVisible());
    }

    @Test
    void canHideSubscriptions() {
        ProfilePrivacySettings settings = new ProfilePrivacySettings(PrivacyVisibility.PUBLIC,PrivacyVisibility.PRIVATE);
        settings.hideSubscriptions();
        assertFalse(settings.isSubscriptionsVisible());
    }

    @Test
    void canShowLikedVideos() {
        ProfilePrivacySettings settings = ProfilePrivacySettings.defaultSettings();
        settings.showLikedVideos();
        assertTrue(settings.isLikedVideosVisible());
    }

    @Test
    void canHideLikedVideos() {
        ProfilePrivacySettings settings = new ProfilePrivacySettings(PrivacyVisibility.PRIVATE, PrivacyVisibility.PUBLIC);
        settings.hideLikedVideos();
        assertFalse(settings.isLikedVideosVisible());
    }
}

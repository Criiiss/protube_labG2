package com.tecnocampus.LS2.protube_back.profile.adapter.in;

import com.tecnocampus.LS2.protube_back.profile.adapter.in.web.ChangeUsernameRequestDTO;
import com.tecnocampus.LS2.protube_back.profile.adapter.in.web.UpdatePrivacySettingsRequestDTO;
import com.tecnocampus.LS2.protube_back.profile.adapter.in.web.UpdateProfileRequestDTO;
import com.tecnocampus.LS2.protube_back.profile.domain.model.PrivacyVisibility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileRequestDTOTest {

    @Test
    void storesUsername() {
        ChangeUsernameRequestDTO request = new ChangeUsernameRequestDTO("new_username");
        assertEquals("new_username", request.username());
    }

    @Test
    void storesProfileFields() {
        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO("Display name", "Biography", "/avatar.png", "/banner.png");
        assertEquals("Display name", request.displayName());
        assertEquals("Biography", request.biography());
        assertEquals("/avatar.png", request.avatarUrl());
        assertEquals("/banner.png", request.bannerUrl());
    }

    @Test
    void storesPrivacySettings() {
        UpdatePrivacySettingsRequestDTO request =new UpdatePrivacySettingsRequestDTO(PrivacyVisibility.PRIVATE, PrivacyVisibility.PUBLIC);
        assertEquals(PrivacyVisibility.PRIVATE, request.subscriptionsVisibility());
        assertEquals(PrivacyVisibility.PUBLIC, request.likedVideosVisibility());
    }
}

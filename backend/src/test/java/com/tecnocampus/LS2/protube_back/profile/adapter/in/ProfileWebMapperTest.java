package com.tecnocampus.LS2.protube_back.profile.adapter.in;

import com.tecnocampus.LS2.protube_back.profile.adapter.in.web.*;
import com.tecnocampus.LS2.protube_back.profile.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileWebMapperTest {
    @Test
    void mapsRequestsToCommands() {
        var mapper = new ProfileWebMapper();
        var profile = mapper.toUpdateProfileCommand(new UpdateProfileRequestDTO("Name", "Bio", "/a", "/b"));
        assertEquals("Name", profile.displayName());
        assertEquals("Bio", profile.biography());
        assertEquals("new_name", mapper.toChangeUserNameCommand(new ChangeUsernameRequestDTO("new_name")).username());
        var privacy = mapper.toUpdatePrivacySettingsCommand(new UpdatePrivacySettingsRequestDTO(
                PrivacyVisibility.PUBLIC, PrivacyVisibility.PRIVATE));
        assertEquals(PrivacyVisibility.PUBLIC, privacy.subscriptionsVisibility());
    }

    @Test
    void mapsDomainToPublicAndPrivateResponses() {
        var created = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        var profile = new UserProfile(1L, "user", "Name", "/a", "Bio", "/b",
                new ProfilePrivacySettings(PrivacyVisibility.PUBLIC, PrivacyVisibility.PRIVATE), created);
        var mapper = new ProfileWebMapper();
        assertEquals("user", mapper.toProfileResponse(profile).username());
        assertEquals(PrivacyVisibility.PUBLIC, mapper.toMyProfileResponse(profile).subscriptionsVisibility());
        assertEquals(created, mapper.toMyProfileResponse(profile).createdAt());
    }
}

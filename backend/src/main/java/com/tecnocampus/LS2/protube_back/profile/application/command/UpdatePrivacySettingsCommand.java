package com.tecnocampus.LS2.protube_back.profile.application.command;

import com.tecnocampus.LS2.protube_back.profile.domain.model.PrivacyVisibility;

/**
 * Command to update the user's privacy settings
 * @param subscriptionsVisibility
 * @param likedVideosVisibility
 */
public record UpdatePrivacySettingsCommand(PrivacyVisibility subscriptionsVisibility, PrivacyVisibility likedVideosVisibility) {
}

package com.tecnocampus.LS2.protube_back.profile.application.command;

/**
 * Command to update the user profile information
 * @param displayName
 * @param biography
 * @param avatarUrl
 * @param bannerUrl
 */
public record UpdateProfileCommand(String displayName, String biography, String avatarUrl, String bannerUrl) {
}

package com.tecnocampus.LS2.protube_back.profile.application.command;

/**
 * Command to update the user profile information
 * @param displayName
 * @param bio
 * @param avatarUrl
 * @param bannerUrl
 */
public record UpdateProfileCommand(String displayName, String bio, String avatarUrl, String bannerUrl) {
}

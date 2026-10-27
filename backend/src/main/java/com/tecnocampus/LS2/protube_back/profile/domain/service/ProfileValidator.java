package com.tecnocampus.LS2.protube_back.profile.domain.service;

import com.tecnocampus.LS2.protube_back.profile.domain.exception.InvalidUsernameException;

public class ProfileValidator {
    private static final long MAX_BIO_LENGTH = 250;
    private static final long MAX_DISPLAY_NAME_LENGTH = 50;
    private static final long MIN_USERNAME_LENGTH = 3;
    private static final long MAX_USERNAME_LENGTH = 20;


    /**
     * username has to be lowercase, no spaces, and only letters, numbers, and underscores
     * @param username
     * @return
     */
    public String normalizeUsername(String username) {
        if (username == null || username.isEmpty()) throw new InvalidUsernameException(username,"Username cannot be null or empty");

        String normalizedUsername = username.toLowerCase().trim();

        if (normalizedUsername.length() < MIN_USERNAME_LENGTH || normalizedUsername.length() > MAX_USERNAME_LENGTH) throw new InvalidUsernameException(username, "Username must be between " + MIN_USERNAME_LENGTH + " and " + MAX_USERNAME_LENGTH + " characters");
        if (!normalizedUsername.matches("^[a-z0-9_]+$"))throw new InvalidUsernameException(username, "Username can only contain lowercase letters, numbers, and underscores");

        return normalizedUsername;
    }

    /**
     * validate the display name that is not null or exceed max length
     * @param displayName
     * @return
     */
    public String validateDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("Display name cannot be null or blank");
        if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) throw new IllegalArgumentException("Display name cannot exceed " + MAX_DISPLAY_NAME_LENGTH + " characters");
        displayName = displayName.trim();
        return displayName;
    }

    /**
     * validate the biography that is not null or exceed max length
     * @param biography
     * @return bio
     */
    public String validateBiography(String biography) {
        if (biography == null) return null;
        String biographyTrim = biography.trim();

        if (biographyTrim.length() > MAX_BIO_LENGTH) throw new IllegalArgumentException("Biography cannot exceed " + MAX_BIO_LENGTH + " characters");
        return biographyTrim;
    }
}

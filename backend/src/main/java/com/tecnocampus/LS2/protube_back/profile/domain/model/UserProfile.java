package com.tecnocampus.LS2.protube_back.profile.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

public class UserProfile {

    private static final long MAX_BIO_LENGTH = 250;
    private static final long MAX_DISPLAY_NAME_LENGTH = 50;

    @Getter
    private final Long userId;
    @Getter
    private final LocalDateTime createdAt;
    @Getter
    private String username;  // username unico x usuario
    @Getter
    private String displayName; //nombre del canal
    @Getter
    private String avatarUrl;
    @Getter
    private String biography;
    @Getter
    private String bannerUrl;
    @Getter
    private ProfilePrivacySettings privacySettings;

    public UserProfile(Long userId, String username, String displayName, String avatarUrl, String biography, String bannerUrl,ProfilePrivacySettings privacySettings, LocalDateTime createdAt) {
        if (createdAt == null) throw new IllegalArgumentException("Creation date cannot be null");
        if (userId == null)throw new IllegalArgumentException("User id cannot be null");

        this.userId = userId;
        this.username = normalizeUsername(username);
        this.displayName = validateDisplayName(displayName);
        this.biography = validateBiography(biography);
        this.avatarUrl = avatarUrl;
        this.bannerUrl = bannerUrl;
        this.privacySettings = privacySettings;
        this.createdAt = createdAt;
    }
    public void updateProfile(String displayName,String biography,String avatarUrl,String bannerUrl) {
        this.displayName = validateDisplayName(displayName);
        this.biography = validateBiography(biography);
        this.avatarUrl = avatarUrl;
        this.bannerUrl = bannerUrl;
    }

    /**
     * normalize username to validate format
     * @param username
     */
    public void changeUserName (String username) {
        this.username = normalizeUsername(username);
    }

    /**
     * username has to be lowercase, no spaces, and only letters, numbers, and underscores
     * @param username
     * @return
     */
    private static String normalizeUsername(String username) {
        if (username == null || username.isBlank())throw new IllegalArgumentException("Username cannot be null or blank");

        String normalizedUsername = username.toLowerCase().trim();
        if (!normalizedUsername.matches("^[a-z0-9_]+$"))throw new IllegalArgumentException("Username can only contain lowercase letters, numbers, and underscores");

        return normalizedUsername;
    }

    /**
     * changes setting of privacy of the user profile
     * @param profilePrivacySettings
     */
    public void updatePrivacySettings(ProfilePrivacySettings profilePrivacySettings) {
        if (profilePrivacySettings == null) throw new IllegalArgumentException("Profile privacy settings cannot be null");

        this.privacySettings = profilePrivacySettings;
    }

    /**
     * removes the avatar adn put default one
     */
    public void removeAvatar() {
        this.avatarUrl = null;
    }

    /**
     * remove the banner and put default one
     */
    public void removeBanner() {
        this.bannerUrl = null;
    }

    /**
     * validate the display name that is not null or exceed max length
     * @param displayName
     * @return
     */
    private String validateDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("Display name cannot be null or blank");
        if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) throw new IllegalArgumentException("Display name cannot exceed " + MAX_DISPLAY_NAME_LENGTH + " characters");
        return displayName;
    }

    private String validateBiography(String biography) {
        if (biography == null) return null;
        if (biography != null && biography.length() > MAX_BIO_LENGTH) throw new IllegalArgumentException("Biography cannot exceed " + MAX_BIO_LENGTH + " characters");

        return biography;
    }








}

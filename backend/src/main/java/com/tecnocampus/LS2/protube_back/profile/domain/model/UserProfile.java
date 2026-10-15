package com.tecnocampus.LS2.protube_back.profile.domain.model;

import com.tecnocampus.LS2.protube_back.profile.domain.exception.InvalidUsernameException;
import com.tecnocampus.LS2.protube_back.profile.domain.service.ProfileValidator;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

public class UserProfile {

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
    private final ProfileValidator profileValidator;


    public UserProfile(Long userId, String username, String displayName, String avatarUrl, String biography, String bannerUrl,ProfilePrivacySettings privacySettings, LocalDateTime createdAt) {
        if (createdAt == null) throw new IllegalArgumentException("Creation date cannot be null");
        if (userId == null)throw new IllegalArgumentException("User id cannot be null");

        this.profileValidator = new ProfileValidator();
        this.userId = userId;
        this.username = profileValidator.normalizeUsername(username);
        this.displayName = profileValidator.validateDisplayName(displayName);
        this.biography = profileValidator.validateBiography(biography);
        this.avatarUrl = avatarUrl;
        this.bannerUrl = bannerUrl;
        privacySettings = privacySettings == null ? ProfilePrivacySettings.defaultSettings() : privacySettings;
        this.privacySettings = privacySettings;
        this.createdAt = createdAt;
    }
    public void updateProfile(String displayName,String biography,String avatarUrl,String bannerUrl) {
        this.displayName = profileValidator.validateDisplayName(displayName);
        this.biography = profileValidator.validateBiography(biography);
        this.avatarUrl = avatarUrl;
        this.bannerUrl = bannerUrl;
    }

    /**
     * normalize username to validate format
     * @param username
     */
    public void changeUserName (String username) {
        this.username = profileValidator.normalizeUsername(username);
    }

    /**
     * changes setting of privacy of the user profile
     * @param profilePrivacySettings
     */
    public void updatePrivacySettings(ProfilePrivacySettings profilePrivacySettings) {
        this.privacySettings = Objects.requireNonNull(profilePrivacySettings, "Privacy settings cannot be null");
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












}

package com.tecnocampus.LS2.protube_back.profile.domain.model;

public class ProfilePrivacySettings {
    private PrivacyVisibility subscriptionsVisibility;
    private PrivacyVisibility likedVideosVisibility;

    public ProfilePrivacySettings(PrivacyVisibility subscriptionsVisibility, PrivacyVisibility likedVideosVisibility) {
        this.subscriptionsVisibility = subscriptionsVisibility;
        this.likedVideosVisibility = likedVideosVisibility;
    }

    /**
     * default privacy settings for subscripription and liked videos from the user
     * @return
     */
    public static ProfilePrivacySettings defaultSettings() {
        return new ProfilePrivacySettings(PrivacyVisibility.PRIVATE, PrivacyVisibility.PRIVATE);
    }










}

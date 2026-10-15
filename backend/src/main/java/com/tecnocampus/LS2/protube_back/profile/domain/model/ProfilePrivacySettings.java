package com.tecnocampus.LS2.protube_back.profile.domain.model;

import lombok.Getter;

import java.util.Objects;

public class ProfilePrivacySettings {
    @Getter
    private PrivacyVisibility subscriptionsVisibility;
    @Getter
    private PrivacyVisibility likedVideosVisibility;

    /**
     * constructor for privacy settings checking null values
     * @param subscriptionsVisibility
     * @param likedVideosVisibility
     */
    public ProfilePrivacySettings(PrivacyVisibility subscriptionsVisibility, PrivacyVisibility likedVideosVisibility) {
        this.subscriptionsVisibility = Objects.requireNonNull(subscriptionsVisibility, "Subscriptions visibility cannot be null");
        this.likedVideosVisibility = Objects.requireNonNull(likedVideosVisibility, "Liked videos visibility cannot be null");
    }

    /**
     * default privacy settings for subscripription and liked videos from the user
     * @return default privacy settings
     */
    public static ProfilePrivacySettings defaultSettings() {
        return new ProfilePrivacySettings(PrivacyVisibility.PRIVATE, PrivacyVisibility.PRIVATE);
    }

    public void showSubscriptions() {
        this.subscriptionsVisibility = PrivacyVisibility.PUBLIC;
    }
    public void hideSubscriptions() {
        this.subscriptionsVisibility = PrivacyVisibility.PRIVATE;
    }
    public void showLikedVideos() {
        this.likedVideosVisibility = PrivacyVisibility.PUBLIC;
    }
    public void hideLikedVideos() {
        this.likedVideosVisibility = PrivacyVisibility.PRIVATE;
    }

    public void updatePrivacySettings(PrivacyVisibility subscriptionsVisibility, PrivacyVisibility likedVideosVisibility) {
        this.subscriptionsVisibility = subscriptionsVisibility;
        this.likedVideosVisibility = likedVideosVisibility;
    }

    public boolean isSubscriptionsVisible() {
        return this.subscriptionsVisibility == PrivacyVisibility.PUBLIC;
    }
    public boolean isLikedVideosVisible() {
        return this.likedVideosVisibility == PrivacyVisibility.PUBLIC;
    }

}

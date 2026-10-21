package com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence;

import com.tecnocampus.LS2.protube_back.profile.domain.model.PrivacyVisibility;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;


@Entity
@Table(name = "user_profiles")
@Getter
public class ProfileJpaEntity {
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @Column(name = "display_name", nullable = false,length = 50)
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "description",length = 250)
    private String biography;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscriptions_visibility", nullable = false)
    private PrivacyVisibility subscriptionsVisibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "liked_videos_visibility", nullable = false)
    private PrivacyVisibility likedVideosVisibility;

    protected ProfileJpaEntity() {
        // Default constructor for JPA
    }

    public ProfileJpaEntity(UserJpaEntity user, String displayName, String avatarUrl, String biography, String bannerUrl, OffsetDateTime updatedAt, PrivacyVisibility subscriptionsVisibility, PrivacyVisibility likedVideosVisibility) {
        this.user = user;
        this.userId = user.getId();
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.biography = biography;
        this.bannerUrl = bannerUrl;
        this.updatedAt = updatedAt;
        this.subscriptionsVisibility = subscriptionsVisibility;
        this.likedVideosVisibility = likedVideosVisibility;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}

package com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence;

import com.tecnocampus.LS2.protube_back.profile.domain.model.PrivacyVisibility;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

//TODO habra que cambiar cosas cuando este toda la bd

@Entity
@Table(name = "user_profiles", uniqueConstraints = @UniqueConstraint(name ="uk_user_profiles_username", columnNames = "username"))
public class ProfileJpaEntity {
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    @Getter
    private Long id;

    @Column(name = "username", nullable = false,length = 20)
    @Getter
    private String username;

    @Column(name = "display_name", nullable = false,length = 50)
    @Getter
    private String displayName;

    @Column(name = "avatar_url")
    @Getter
    private String avatarUrl;

    @Column(name = "biography",length = 250)
    @Getter
    private String biography;

    @Column(name = "banner_url")
    @Getter
    private String bannerUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Getter
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscriptions_visibility", nullable = false)
    @Getter
    private PrivacyVisibility subscriptionsVisibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "liked_videos_visibility", nullable = false)
    @Getter
    private PrivacyVisibility likedVideosVisibility;

    protected ProfileJpaEntity() {
        // Default constructor for JPA
    }

    public ProfileJpaEntity(Long id, String username, String displayName, String avatarUrl, String biography, String bannerUrl, LocalDateTime createdAt, PrivacyVisibility subscriptionsVisibility, PrivacyVisibility likedVideosVisibility) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.biography = biography;
        this.bannerUrl = bannerUrl;
        this.createdAt = createdAt;
        this.subscriptionsVisibility = subscriptionsVisibility;
        this.likedVideosVisibility = likedVideosVisibility;
    }
}

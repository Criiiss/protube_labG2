package com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence;

import com.tecnocampus.LS2.protube_back.profile.domain.model.ProfilePrivacySettings;
import com.tecnocampus.LS2.protube_back.profile.domain.model.UserProfile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
public class ProfilePersistenceMapper {

    private final Clock clock;

    public ProfilePersistenceMapper(Clock clock) {
        this.clock = clock;
    }

    public UserProfile toDomain(ProfileJpaEntity entity) {
        ProfilePrivacySettings privacySettings = new ProfilePrivacySettings(
                entity.getSubscriptionsVisibility(),
                entity.getLikedVideosVisibility()
        );

        return new UserProfile(
                entity.getUserId(),
                entity.getUser().getUsername(),
                entity.getDisplayName(),
                entity.getAvatarUrl(),
                entity.getBiography(),
                entity.getBannerUrl(),
                privacySettings,
                entity.getUser().getCreatedAt()
        );
    }

    public ProfileJpaEntity toEntity(UserProfile profile, UserJpaEntity user) {
        return new ProfileJpaEntity(
                user,
                profile.getDisplayName(),
                profile.getAvatarUrl(),
                profile.getBiography(),
                profile.getBannerUrl(),
                OffsetDateTime.now(clock),
                profile.getPrivacySettings().getSubscriptionsVisibility(),
                profile.getPrivacySettings().getLikedVideosVisibility()
        );
    }
}

package com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence;

import com.tecnocampus.LS2.protube_back.profile.domain.model.ProfilePrivacySettings;
import com.tecnocampus.LS2.protube_back.profile.domain.model.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class ProfilePersistenceMapper {

    public UserProfile toDomain(ProfileJpaEntity entity) {
        ProfilePrivacySettings privacySettings = new ProfilePrivacySettings(
                entity.getSubscriptionsVisibility(),
                entity.getLikedVideosVisibility()
        );

        return new UserProfile(
                entity.getId(),
                entity.getUsername(),
                entity.getDisplayName(),
                entity.getAvatarUrl(),
                entity.getBiography(),
                entity.getBannerUrl(),
                privacySettings,
                entity.getCreatedAt()

        );
    }

    public ProfileJpaEntity toEntity(UserProfile profile) {
        return new ProfileJpaEntity(
                profile.getUserId(),
                profile.getUsername(),
                profile.getDisplayName(),
                profile.getAvatarUrl(),
                profile.getBiography(),
                profile.getBannerUrl(),
                profile.getCreatedAt(),
                profile.getPrivacySettings().getSubscriptionsVisibility(),
                profile.getPrivacySettings().getLikedVideosVisibility()
        );
    }
}

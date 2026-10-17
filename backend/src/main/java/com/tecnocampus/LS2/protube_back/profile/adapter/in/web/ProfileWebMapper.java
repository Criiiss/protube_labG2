package com.tecnocampus.LS2.protube_back.profile.adapter.in.web;

import com.tecnocampus.LS2.protube_back.profile.application.command.ChangeUserNameCommand;
import com.tecnocampus.LS2.protube_back.profile.application.command.UpdatePrivacySettingsCommand;
import com.tecnocampus.LS2.protube_back.profile.application.command.UpdateProfileCommand;
import com.tecnocampus.LS2.protube_back.profile.domain.model.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class ProfileWebMapper {

    public UpdateProfileCommand toUpdateProfileCommand(UpdateProfileRequestDTO requestDTO) {
        return new UpdateProfileCommand(
                requestDTO.displayName(),
                requestDTO.biography(),
                requestDTO.avatarUrl(),
                requestDTO.bannerUrl()
        );
    }
    public ChangeUserNameCommand toChangeUserNameCommand(ChangeUsernameRequestDTO requestDTO) {
        return new ChangeUserNameCommand(
                requestDTO.username()
        );
    }

    public UpdatePrivacySettingsCommand toUpdatePrivacySettingsCommand(UpdatePrivacySettingsRequestDTO requestDTO) {
        return new UpdatePrivacySettingsCommand(
                requestDTO.subscriptionsVisibility(),
                requestDTO.likedVideosVisibility()
        );
    }

    public ProfileResponseDTO toProfileResponse(UserProfile profile){
        return new ProfileResponseDTO(
                profile.getUserId(),
                profile.getUsername(),
                profile.getDisplayName(),
                profile.getBiography(),
                profile.getAvatarUrl(),
                profile.getBannerUrl(),
                profile.getCreatedAt()
        );
    }

    public MyProfileResponseDTO toMyProfileResponse(UserProfile profile){
        return new MyProfileResponseDTO(
                profile.getUserId(),
                profile.getUsername(),
                profile.getDisplayName(),
                profile.getBiography(),
                profile.getAvatarUrl(),
                profile.getBannerUrl(),
                profile.getPrivacySettings().getSubscriptionsVisibility(),
                profile.getPrivacySettings().getLikedVideosVisibility(),
                profile.getCreatedAt()
        );
    }
}

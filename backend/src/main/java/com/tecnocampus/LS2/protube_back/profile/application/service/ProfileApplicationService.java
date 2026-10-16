package com.tecnocampus.LS2.protube_back.profile.application.service;

import com.tecnocampus.LS2.protube_back.profile.application.command.ChangeUserNameCommand;
import com.tecnocampus.LS2.protube_back.profile.application.command.UpdatePrivacySettingsCommand;
import com.tecnocampus.LS2.protube_back.profile.application.command.UpdateProfileCommand;
import com.tecnocampus.LS2.protube_back.profile.application.port.in.*;
import com.tecnocampus.LS2.protube_back.profile.application.port.out.ProfileRepository;
import com.tecnocampus.LS2.protube_back.profile.domain.exception.ProfileNotFoundException;
import com.tecnocampus.LS2.protube_back.profile.domain.exception.UserNameAlreadyExistException;
import com.tecnocampus.LS2.protube_back.profile.domain.model.ProfilePrivacySettings;
import com.tecnocampus.LS2.protube_back.profile.domain.model.UserProfile;
import com.tecnocampus.LS2.protube_back.profile.domain.service.ProfileValidator;
import org.springframework.stereotype.Service;

@Service
public class ProfileApplicationService implements GetMyProfileUseCase, GetPublicProfileUseCase, UpdateProfileUseCase, ChangeUserNameUseCase, UpdatePrivacySettingsUseCase {

    private final ProfileRepository profileRepository;
    private final ProfileValidator profileValidator;


    // TODO repository da error por que faltan clases de repo
    public ProfileApplicationService(ProfileRepository profileRepository, ProfileValidator profileValidator) {
        this.profileRepository = profileRepository;
        this.profileValidator = profileValidator;
    }

    @Override
    public UserProfile changeUserName(long userId, ChangeUserNameCommand command) {
        UserProfile profile = findUserProfileById(userId);
        String normalizeUsername = profileValidator.normalizeUsername(command.username());
        boolean exists = profileRepository.existsByUserNameNotID(normalizeUsername, userId);
        if(exists) throw new UserNameAlreadyExistException(normalizeUsername);
        profile.changeUserName(normalizeUsername);
        return profileRepository.save(profile);
    }

    @Override
    public UserProfile getMyProfile(Long userId) {
        return profileRepository.findById(userId).orElseThrow(() -> new ProfileNotFoundException(userId)) ;
    }

    @Override
    public UserProfile getPublicProfile(String username) {
        String normalizeUsername = profileValidator.normalizeUsername(username);
        return profileRepository.findByUsername(normalizeUsername).orElseThrow(() -> new ProfileNotFoundException(normalizeUsername));
    }

    @Override
    public UserProfile updatePrivacySettings(long userId, UpdatePrivacySettingsCommand command) {
        UserProfile profile = findUserProfileById(userId);
        ProfilePrivacySettings newSettings = new ProfilePrivacySettings(command.subscriptionsVisibility(),command.likedVideosVisibility());
        profile.updatePrivacySettings(newSettings);
        return profileRepository.save(profile);
    }

    @Override
    public UserProfile updateProfile(long userId, UpdateProfileCommand command) {
        UserProfile profile = findUserProfileById(userId);
        profile.updateProfile(command.displayName(), command.biography(), command.avatarUrl(), command.bannerUrl());
        return profileRepository.save(profile);
    }

    private UserProfile findUserProfileById(Long userId) {
        return profileRepository.findById(userId).orElseThrow(() -> new ProfileNotFoundException(userId));
    }
}

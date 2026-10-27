package com.tecnocampus.LS2.protube_back.profile.application;

import com.tecnocampus.LS2.protube_back.profile.application.command.ChangeUserNameCommand;
import com.tecnocampus.LS2.protube_back.profile.application.command.UpdatePrivacySettingsCommand;
import com.tecnocampus.LS2.protube_back.profile.application.command.UpdateProfileCommand;
import com.tecnocampus.LS2.protube_back.profile.application.port.out.ProfileRepository;
import com.tecnocampus.LS2.protube_back.profile.application.service.ProfileApplicationService;
import com.tecnocampus.LS2.protube_back.profile.domain.exception.ProfileNotFoundException;
import com.tecnocampus.LS2.protube_back.profile.domain.exception.UserNameAlreadyExistException;
import com.tecnocampus.LS2.protube_back.profile.domain.model.PrivacyVisibility;
import com.tecnocampus.LS2.protube_back.profile.domain.model.ProfilePrivacySettings;
import com.tecnocampus.LS2.protube_back.profile.domain.model.UserProfile;
import com.tecnocampus.LS2.protube_back.profile.domain.service.ProfileValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProfileApplicationServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProfileValidator profileValidator;

    private ProfileApplicationService service;
    private UserProfile profile;
    @BeforeEach
    void setUp(){
        service = new ProfileApplicationService(profileRepository,profileValidator);
        profile = new UserProfile((long) 1D,"maria_23","Maria","/avatar.png","Biography","/banner.png", ProfilePrivacySettings.defaultSettings(), OffsetDateTime.parse("2026-10-19T12:00:00Z"));
    }

    @Test
    public void getsMyProfile() {
        when(profileRepository.findById((long)1D)).thenReturn(Optional.of(profile));
        assertEquals(profile, service.getMyProfile((long)1D));
    }

    @Test
    public void throwsWhenProfileDoesNotExist(){
        when(profileRepository.findById((long)1D)).thenReturn(Optional.empty());
        assertThrows(ProfileNotFoundException.class,() -> service.getMyProfile((long)1D));
    }

    @Test
    public void getsPublicProfile() {
        when(profileValidator.normalizeUsername("Maria_23")).thenReturn("maria_23");
        when(profileRepository.findByUsername("maria_23")).thenReturn(Optional.of(profile));
        assertEquals(profile, service.getPublicProfile("Maria_23"));
    }

    @Test
    public void updateProfileAndSavesIt(){
        UpdateProfileCommand command = new UpdateProfileCommand("New name","New biography","/new-avatar.png", "/new-banner.png");
        when(profileRepository.findById((long)1D)).thenReturn(Optional.of(profile));
        when(profileRepository.save(profile)).thenReturn(profile);
        UserProfile result = service.updateProfile((long)1D, command);
        assertEquals("New name", result.getDisplayName());
        assertEquals("New biography", result.getBiography());
        assertEquals("/new-avatar.png", result.getAvatarUrl());
        assertEquals("/new-banner.png", result.getBannerUrl());
        verify(profileRepository).save(profile);
    }

    @Test
    public void changesUsernameWhenItsAvailable(){
        ChangeUserNameCommand command = new ChangeUserNameCommand("new_name");
        when(profileRepository.findById((long)1D)).thenReturn(Optional.of(profile));
        when(profileValidator.normalizeUsername("new_name")).thenReturn("new_name");
        when(profileRepository.existsByUsernameNotID("new_name", (long)1D)).thenReturn(false);
        when(profileRepository.save(profile)).thenReturn(profile);
        UserProfile result = service.changeUserName((long)1D, command);
        assertEquals("new_name", result.getUsername());
        verify(profileRepository).save(profile);
    }

    @Test
    public void rejectsDuplicateUsername(){
        ChangeUserNameCommand command = new ChangeUserNameCommand("existing_name");
        when(profileRepository.findById((long)1D)).thenReturn(Optional.of(profile));
        when(profileValidator.normalizeUsername("existing_name")).thenReturn("existing_name");
        when(profileRepository.existsByUsernameNotID("existing_name", (long)1D)).thenReturn(true);
        assertThrows(UserNameAlreadyExistException.class,() -> service.changeUserName((long)1D, command));
    }

    @Test
    public void updatePrivacySettings(){
        UpdatePrivacySettingsCommand command = new UpdatePrivacySettingsCommand(PrivacyVisibility.PUBLIC, PrivacyVisibility.PRIVATE);
        when(profileRepository.findById((long)1D)).thenReturn(Optional.of(profile));
        when(profileRepository.save(profile)).thenReturn(profile);
        UserProfile result = service.updatePrivacySettings((long)1D, command);
        assertTrue(result.getPrivacySettings().isSubscriptionsVisible());
        assertFalse(result.getPrivacySettings().isLikedVideosVisible());
    }
    @Test
    public void removesAvatar(){
        when(profileRepository.findById((long)1D)).thenReturn(Optional.of(profile));
        when(profileRepository.save(profile)).thenReturn(profile);
        UserProfile result = service.removeAvatar((long)1D);
        assertNull(result.getAvatarUrl());
        verify(profileRepository).save(profile);
    }
    @Test
    public void removesBanner(){
        when(profileRepository.findById((long)1D)).thenReturn(Optional.of(profile));
        when(profileRepository.save(profile)).thenReturn(profile);
        UserProfile result = service.removeBanner((long)1D);
        assertNull(result.getBannerUrl());
        verify(profileRepository).save(profile);
    }

    @Test
    public void throwsWhenPublicProfileDoesNotExist() {
        when(profileValidator.normalizeUsername("unknown_user")).thenReturn("unknown_user");
        when(profileRepository.findByUsername("unknown_user")).thenReturn(Optional.empty());
        ProfileNotFoundException exception = assertThrows(ProfileNotFoundException.class,() -> service.getPublicProfile("unknown_user"));
        assertEquals("Profile for user unknown_user not found",exception.getMessage());
    }

    @Test
    public void throwsWhenUpdatingProfileAndProfileDoesNotExist() {
        UpdateProfileCommand command = new UpdateProfileCommand("New name","New biography","/new-avatar.png", "/new-banner.png");
        when(profileRepository.findById((long) 1D)).thenReturn(Optional.empty());
        ProfileNotFoundException exception = assertThrows(ProfileNotFoundException.class,() -> service.updateProfile((long) 1D, command));
        assertEquals("Profile for user with id 1 not found",exception.getMessage());
    }

}

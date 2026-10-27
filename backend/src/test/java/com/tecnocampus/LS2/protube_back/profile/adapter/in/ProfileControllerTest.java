package com.tecnocampus.LS2.protube_back.profile.adapter.in;

import com.tecnocampus.LS2.protube_back.profile.adapter.in.web.MyProfileResponseDTO;
import com.tecnocampus.LS2.protube_back.profile.adapter.in.web.ProfileController;
import com.tecnocampus.LS2.protube_back.profile.adapter.in.web.ProfileResponseDTO;
import com.tecnocampus.LS2.protube_back.profile.adapter.in.web.ProfileWebMapper;
import com.tecnocampus.LS2.protube_back.profile.application.port.in.*;
import com.tecnocampus.LS2.protube_back.profile.domain.model.ProfilePrivacySettings;
import com.tecnocampus.LS2.protube_back.profile.domain.model.UserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class ProfileControllerTest {

    @Mock
    private GetMyProfileUseCase getMyProfileUseCase;
    @Mock
    private GetPublicProfileUseCase getPublicProfileUseCase;
    @Mock
    private UpdateProfileUseCase updateProfileUseCase;
    @Mock
    private ChangeUserNameUseCase changeUserNameUseCase;
    @Mock
    private UpdatePrivacySettingsUseCase updatePrivacySettingsUseCase;
    @Mock
    private RemoveBannerUseCase removeBannerUseCase;
    @Mock
    private RemoveAvatarUseCase removeAvatarUseCase;

    private ProfileController controller;
    private UserProfile profile;



    @BeforeEach
    void setUp(){
        controller = new ProfileController(getMyProfileUseCase,getPublicProfileUseCase,updateProfileUseCase,changeUserNameUseCase,updatePrivacySettingsUseCase,new ProfileWebMapper(),removeBannerUseCase,removeAvatarUseCase);
        profile = new UserProfile((long) 1D,"maria_23","Maria","/avatar.png","Biography","/banner.png", ProfilePrivacySettings.defaultSettings(), OffsetDateTime.parse("2026-10-19T12:00:00Z"));
    }

    @Test
    public void returnsMyProfile() {
        when(getMyProfileUseCase.getMyProfile((long) 1D)).thenReturn(profile);
        ResponseEntity<MyProfileResponseDTO> response = controller.getMyProfile((long) 1D);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("maria_23", response.getBody().username());
    }

    @Test
    public void returnsPublicProfileWithoutPrivacyFields(){
        when(getPublicProfileUseCase.getPublicProfile("maria_23")).thenReturn(profile);
        ResponseEntity<ProfileResponseDTO> response = controller.getPublicProfile("maria_23");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("maria_23", response.getBody().username());
        assertEquals("/avatar.png",response.getBody().avatarUrl());
    }

    @Test
    public void removesAvatar(){
        when(removeAvatarUseCase.removeAvatar((long) 1D)).thenReturn(profile);
        ResponseEntity<MyProfileResponseDTO> response = controller.removeAvatar((long) 1D);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
    @Test
    public void removesBanner(){
        when(removeBannerUseCase.removeBanner((long) 1D)).thenReturn(profile);
        ResponseEntity<MyProfileResponseDTO> response = controller.removeBanner((long) 1D);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}

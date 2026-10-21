package com.tecnocampus.LS2.protube_back.profile.adapter.out;

import com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence.*;
import com.tecnocampus.LS2.protube_back.profile.domain.exception.ProfileNotFoundException;
import com.tecnocampus.LS2.protube_back.profile.domain.model.ProfilePrivacySettings;
import com.tecnocampus.LS2.protube_back.profile.domain.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class ProfilePersistenceAdapterTest {
    @Mock
    private ProfilePersistenceMapper mapper;
    @Mock
    private SpringDataProfileRepository profileRepository;
    @Mock
    private SpringDataUserRepository userRepository;
    private ProfilePersistenceAdapter adapter;
    private UserJpaEntity user;
    private ProfileJpaEntity profileEntity;
    private UserProfile profile;

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-10T08:00:00Z");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-10-11T09:00:00Z");

    @BeforeEach
    void setUp(){
        adapter = new ProfilePersistenceAdapter(mapper, profileRepository, userRepository);
        user = new UserJpaEntity((long) 1D, "maria_23","maria@example.com","password", "LOCAL", null, CREATED_AT, UPDATED_AT);
        profile = new UserProfile((long) 1D,"maria_23","Maria","/avatar.png","Biography","/banner.png", ProfilePrivacySettings.defaultSettings(),CREATED_AT);
        profileEntity = new ProfileJpaEntity(user,"Maria","/avatar.png","Biography","/banner.png",UPDATED_AT,profile.getPrivacySettings().getSubscriptionsVisibility(), profile.getPrivacySettings().getLikedVideosVisibility());
    }

    @Test
    public void findsProfileByUsername(){
        when(profileRepository.findByUsername("maria_23")).thenReturn(Optional.of(profileEntity));
        when(mapper.toDomain(profileEntity)).thenReturn(profile);
        Optional<UserProfile> result = adapter.findByUsername("maria_23");
        assertEquals(Optional.of(profile), result);
        verify(profileRepository).findByUsername("maria_23");
        verify(mapper).toDomain(profileEntity);
    }

    @Test
    public void returnsEmptyWhenProfileNotExist(){
        when(profileRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        Optional<UserProfile> result = adapter.findByUsername("unknown");
        assertEquals(Optional.empty(), result);
    }

    @Test
    public void findsProfileById(){
        when(profileRepository.findById((long)1D)).thenReturn(Optional.of(profileEntity));
        when(mapper.toDomain(profileEntity)).thenReturn(profile);
        Optional<UserProfile> result = adapter.findById((long)1D);
        assertEquals(Optional.of(profile), result);
        verify(profileRepository).findById((long)1D);
        verify(mapper).toDomain(profileEntity);
    }

    @Test
    public void checkWhetherUsernameBelongsToAnotherUser(){
        when(profileRepository.existsByUsernameNotID("maria_23", (long)1D)).thenReturn(false);
        boolean result = adapter.existsByUsernameNotID("maria_23", (long)1D);
        assertEquals(false, result);
        verify(profileRepository).existsByUsernameNotID("maria_23", (long)1D);
    }

    @Test
    public void saveUserAndProfile(){
        ProfileJpaEntity savedEntity = profileEntity;
        when(userRepository.findById((long)1D)).thenReturn(Optional.of(user));
        when(mapper.toEntity(profile,user)).thenReturn(profileEntity);
        when(profileRepository.save(profileEntity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(profile);
        UserProfile result = adapter.save(profile);
        assertEquals(profile, result);
        verify(userRepository).findById((long)1D);
        verify(userRepository).save(user);
        verify(mapper).toEntity(profile,user);
        verify(profileRepository).save(profileEntity);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    public void updateUsernameBeforeSaving(){
        UserProfile profileWithNewUsername = new UserProfile((long)1D,"new_username","Maria","/avatar.png","Biography","/banner.png",ProfilePrivacySettings.defaultSettings(), UPDATED_AT);
        when(userRepository.findById((long)1D)).thenReturn(Optional.of(user));
        when(mapper.toEntity(profileWithNewUsername,user)).thenReturn(profileEntity);
        when(profileRepository.save(profileEntity)).thenReturn(profileEntity);
        when(mapper.toDomain(profileEntity)).thenReturn(profileWithNewUsername);
        adapter.save(profileWithNewUsername);
        assertEquals("new_username", user.getUsername());
        verify(userRepository).save(user);
    }

    @Test
    public void throwsExceptionWhenUserDoesNotExist(){
        when(userRepository.findById((long)1D)).thenReturn(Optional.empty());
        assertThrows(ProfileNotFoundException.class, () -> adapter.save(profile));
    }
}

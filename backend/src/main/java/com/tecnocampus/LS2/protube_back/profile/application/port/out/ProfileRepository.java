package com.tecnocampus.LS2.protube_back.profile.application.port.out;

import com.tecnocampus.LS2.protube_back.profile.domain.model.UserProfile;

import java.util.Optional;

public interface ProfileRepository {

    Optional<UserProfile> findByUsername(String username);
    Optional<UserProfile> findById(Long id);

    boolean existsByUserNameNotID(String username, Long id);
    UserProfile save(UserProfile userProfile);
}

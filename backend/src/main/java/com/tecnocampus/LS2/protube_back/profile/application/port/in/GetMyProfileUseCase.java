package com.tecnocampus.LS2.protube_back.profile.application.port.in;

import com.tecnocampus.LS2.protube_back.profile.domain.model.UserProfile;

public interface GetMyProfileUseCase {
    UserProfile getMyProfile(Long userId);
}

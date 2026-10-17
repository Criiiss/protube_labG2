package com.tecnocampus.LS2.protube_back.profile.application.port.in;

import com.tecnocampus.LS2.protube_back.profile.domain.model.UserProfile;

public interface RemoveBannerUseCase {
    UserProfile removeBanner(long userId);
}

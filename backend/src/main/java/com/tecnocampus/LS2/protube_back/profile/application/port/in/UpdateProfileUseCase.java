package com.tecnocampus.LS2.protube_back.profile.application.port.in;

import com.tecnocampus.LS2.protube_back.profile.application.command.UpdateProfileCommand;
import com.tecnocampus.LS2.protube_back.profile.domain.model.UserProfile;

public interface UpdateProfileUseCase {
    UserProfile updateProfile(long userId, UpdateProfileCommand command);
}

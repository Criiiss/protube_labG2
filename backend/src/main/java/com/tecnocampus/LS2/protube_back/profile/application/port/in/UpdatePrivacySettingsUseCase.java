package com.tecnocampus.LS2.protube_back.profile.application.port.in;

import com.tecnocampus.LS2.protube_back.profile.application.command.UpdatePrivacySettingsCommand;
import com.tecnocampus.LS2.protube_back.profile.domain.model.UserProfile;

public interface UpdatePrivacySettingsUseCase {
    UserProfile updatePrivacySettings(long userId, UpdatePrivacySettingsCommand command);
}

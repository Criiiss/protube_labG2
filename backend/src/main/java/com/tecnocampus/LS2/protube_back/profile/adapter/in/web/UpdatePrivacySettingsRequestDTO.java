package com.tecnocampus.LS2.protube_back.profile.adapter.in.web;

import com.tecnocampus.LS2.protube_back.profile.domain.model.PrivacyVisibility;

public record UpdatePrivacySettingsRequestDTO(PrivacyVisibility subscriptionsVisibility, PrivacyVisibility likedVideosVisibility) {
}

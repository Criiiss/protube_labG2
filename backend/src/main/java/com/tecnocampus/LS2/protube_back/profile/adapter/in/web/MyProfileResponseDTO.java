package com.tecnocampus.LS2.protube_back.profile.adapter.in.web;

import com.tecnocampus.LS2.protube_back.profile.domain.model.PrivacyVisibility;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record MyProfileResponseDTO(Long userId, String username, String displayName, String biography, String avatarUrl, String bannerUrl, PrivacyVisibility subscriptionsVisibility, PrivacyVisibility likedVideosVisibility, OffsetDateTime createdAt) {
}

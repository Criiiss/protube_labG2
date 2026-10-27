package com.tecnocampus.LS2.protube_back.profile.adapter.in.web;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record ProfileResponseDTO(Long userId, String username, String displayName, String biography, String avatarUrl, String bannerUrl, OffsetDateTime createdAt) {

}

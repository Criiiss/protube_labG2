package com.tecnocampus.LS2.protube_back.videos.adapter.in.web;

public record ChannelVideoResponseDTO(String id, String title, String channel, String views, String published, String duration, String categoryId, String thumbnail, long viewsCount, String createdAt, String status, String visibility) {
}

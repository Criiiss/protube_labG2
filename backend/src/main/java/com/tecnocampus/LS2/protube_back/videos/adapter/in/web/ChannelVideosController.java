package com.tecnocampus.LS2.protube_back.videos.adapter.in.web;

import com.tecnocampus.LS2.protube_back.auth.AuthController;
import com.tecnocampus.LS2.protube_back.auth.AuthService;
import com.tecnocampus.LS2.protube_back.videos.application.service.ChannelVideoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/profiles")
public class ChannelVideosController {
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "DRAFT",
            "SCHEDULED",
            "PUBLISHED",
            "ARCHIVED"
    );

    private final ChannelVideoService channelVideoService;
    private final AuthService authService;

    public ChannelVideosController( ChannelVideoService channelVideoService, AuthService authService) {
        this.channelVideoService = channelVideoService;
        this.authService = authService;
    }

    @GetMapping("/{userId}/videos")
    public ResponseEntity<List<ChannelVideoResponseDTO>> getPublicChannelVideos(@PathVariable Long userId) {
        return ResponseEntity.ok(channelVideoService.getPublicVideosByUserId(userId));
    }

    @GetMapping("/me/videos")
    public ResponseEntity<List<ChannelVideoResponseDTO>> getMyVideos(@RequestParam Long userId, @RequestParam String status, HttpServletRequest request) {
        Long authenticatedUserId = AuthController.requireUser(request, authService);
        if (!authenticatedUserId.equals(userId)) return ResponseEntity.status(403).build();

        String normalizedStatus = status.toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(channelVideoService.getOwnerVideosByStatus(userId, normalizedStatus));
    }
}

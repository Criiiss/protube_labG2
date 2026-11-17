package com.tecnocampus.LS2.protube_back.videos;

import com.tecnocampus.LS2.protube_back.auth.AuthController;
import com.tecnocampus.LS2.protube_back.auth.AuthService;
import com.tecnocampus.LS2.protube_back.videos.adapter.in.web.*;
import com.tecnocampus.LS2.protube_back.videos.application.service.ChannelVideoService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ChannelVideosControllerTest {
    @Test
    void getsPublicVideos() {
        var service = mock(ChannelVideoService.class);
        var controller = new ChannelVideosController(service, mock(AuthService.class));
        when(service.getPublicVideosByUserId(4L)).thenReturn(List.of());
        assertEquals(200, controller.getPublicChannelVideos(4L).getStatusCode().value());
        verify(service).getPublicVideosByUserId(4L);
    }

    @Test
    void rejectsOtherUsersAndInvalidStatuses() {
        var service = mock(ChannelVideoService.class);
        var auth = mock(AuthService.class);
        var controller = new ChannelVideosController(service, auth);
        var request = new MockHttpServletRequest();
        request.getSession(true).setAttribute("authUser", 1L);
        request.getSession().setAttribute("authDeadline", java.time.Instant.now().plusSeconds(60));
        request.getSession().setAttribute("authVersion", 0);
        when(auth.version(1L)).thenReturn(0);
        assertEquals(403, controller.getMyVideos(2L, "DRAFT", request).getStatusCode().value());
        assertEquals(400, controller.getMyVideos(1L, "unknown", request).getStatusCode().value());
    }

    @Test
    void acceptsCaseInsensitiveAllowedStatus() {
        var service = mock(ChannelVideoService.class);
        var auth = mock(AuthService.class);
        var controller = new ChannelVideosController(service, auth);
        var request = new MockHttpServletRequest();
        request.getSession(true).setAttribute("authUser", 1L);
        request.getSession().setAttribute("authDeadline", java.time.Instant.now().plusSeconds(60));
        request.getSession().setAttribute("authVersion", 0);
        when(auth.version(1L)).thenReturn(0);
        assertEquals(200, controller.getMyVideos(1L, "draft", request).getStatusCode().value());
        verify(service).getOwnerVideosByStatus(1L, "DRAFT");
    }
}

package com.tecnocampus.LS2.protube_back.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private final AuthService auth = mock(AuthService.class);
    private final AttemptLimiter limiter = mock(AttemptLimiter.class);
    private final GoogleAuth google = mock(GoogleAuth.class);
    private final AuthController controller = new AuthController(auth, limiter, google, false);

    private MockHttpServletRequest authenticatedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authUser", 1L);
        session.setAttribute("authDeadline",Instant.now().plusSeconds(300));
        session.setAttribute("authVersion", 0);
        request.setSession(session);
        return request;
    }


    private AuthService.Registration validRegistration(String captcha) {
        return new AuthService.Registration("Maria","maria_23","maria@example.com","ValidPass1","ValidPass1",LocalDate.of(2000, 1, 1),true,captcha);
    }


    @Test
    void sessionReturnsAuthenticatedUser() {
        MockHttpServletRequest request = authenticatedRequest();
        assertTrue(request.getSession(false).getAttribute("authUser") instanceof Long);
        AuthService.User user = new AuthService.User(1L, "maria_23", "Maria", null);
        when(auth.version(1L)).thenReturn(0);
        when(auth.user(1L)).thenReturn(user);
        AuthService.User result = controller.session(request);
        assertEquals(user, result);
        verify(auth).user(1L);
    }


    @Test
    void googleRedirectsToGoogleAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(google.start(request)).thenReturn("https://accounts.google.com/login");
        controller.google(request, response);
        assertEquals("https://accounts.google.com/login", response.getRedirectedUrl());
    }

    @Test
    void googleCallbackEstablishesSessionForExistingUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthService.User user = new AuthService.User((long)1D, "maria_23", "Maria", null);
        when(google.callback("code", "state", request)).thenReturn(user);
        when(google.frontend()).thenReturn("http://localhost:5173");
        when(auth.version((long)1D)).thenReturn(0);
        controller.callback("code","state",request,response);
        assertEquals("http://localhost:5173/#/", response.getRedirectedUrl());
        assertEquals((long)1D,request.getSession().getAttribute("authUser"));
    }

    @Test
    void googleCallbackRedirectsToRegistrationForNewUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(google.callback("code", "state", request)).thenReturn(null);
        when(google.frontend()).thenReturn("http://localhost:5173");
        controller.callback("code","state",request,response);
        assertEquals("http://localhost:5173/#/google-register",response.getRedirectedUrl());
        verify(auth, never()).version(any());
    }

    @Test
    void googleCallbackRedirectsToLoginWhenGoogleFails()throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(google.callback("code", "state", request)).thenThrow(new AuthFailure(400, "Google error"));
        when(google.frontend()).thenReturn("http://localhost:5173");
        controller.callback("code","state",request,response);
        assertEquals("http://localhost:5173/#/login?error=google", response.getRedirectedUrl());
    }

    @Test
    void completeGoogleRegistrationEstablishesSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthService.User user = new AuthService.User(1L, "maria_23", "Maria", null);
        AuthController.GoogleRegistration registration = new AuthController.GoogleRegistration("maria_23",LocalDate.of(2000, 1, 1), true);
        when(google.complete(registration, request)).thenReturn(user);
        when(auth.version(1L)).thenReturn(0);
        assertEquals(user,controller.complete(registration, request, response));
        assertEquals(1L,request.getSession().getAttribute("authUser"));
        verify(google).complete(registration, request);
    }

    @Test
    void establishInvalidatesExistingSession()throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.getSession(true);
        AuthService.User user = new AuthService.User(1L, "maria_23", "Maria", null);
        when(auth.version(1L)).thenReturn(0);
        controller.establish(user, false, request, response);
        assertEquals(1L,request.getSession().getAttribute("authUser"));
    }

    @Test
    void requireUserRejectsRequestWithoutSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AuthFailure failure = assertThrows(AuthFailure.class,() -> AuthController.requireUser(request, auth));
        assertEquals(401, failure.status);
    }

    @Test
    void requireUserRejectsSessionWithoutUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true);
        AuthFailure failure = assertThrows(AuthFailure.class, () -> AuthController.requireUser(request, auth));
        assertEquals(401, failure.status);
    }

    @Test
    void requireUserRejectsExpiredSession() {
        MockHttpServletRequest request = authenticatedRequest();
        request.getSession().setAttribute("authDeadline", Instant.now().minusSeconds(1));

        when(auth.version((long)1D)).thenReturn(0);
        AuthFailure failure = assertThrows(AuthFailure.class, () -> AuthController.requireUser(request, auth));
        assertEquals(401, failure.status);
        verify(auth, never()).version((long)1D);
    }

    @Test
    void requireUserRejectsSessionWithDifferentVersion() {
        MockHttpServletRequest request = authenticatedRequest();
        when(auth.version((long)1D)).thenReturn(2);
        AuthFailure failure = assertThrows(AuthFailure.class, () -> AuthController.requireUser(request, auth));
        assertEquals(401, failure.status);
    }

    @Test
    void registerRejectsRequestWithoutSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AuthService.Registration body = validRegistration("3");
        AuthFailure failure = assertThrows(AuthFailure.class, () -> controller.register(body, request));
        assertEquals(400, failure.status);
        verify(auth, never()).register(any());
    }
    @Test
    void registerRejectsRequestWhenCaptchaAnswerIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true);
        AuthService.Registration body = validRegistration("3");
        AuthFailure failure = assertThrows(AuthFailure.class,() -> controller.register(body, request));
        assertEquals(400, failure.status);
        verify(auth, never()).register(any());
    }

    @Test
    void registerRejectsExpiredCaptcha() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        var session = request.getSession(true);
        session.setAttribute("captcha", "3");
        session.setAttribute("captchaExpires",Instant.now().minusSeconds(1));
        AuthService.Registration body = validRegistration("3");
        AuthFailure failure = assertThrows(AuthFailure.class, () -> controller.register(body, request));
        assertEquals(400, failure.status);
        verify(auth, never()).register(any());
    }

    @Test
    void registerRejectsIncorrectCaptchaAnswer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        var session = request.getSession(true);
        session.setAttribute("captcha", "3");
        session.setAttribute("captchaExpires", Instant.now().plusSeconds(300));
        AuthService.Registration body = validRegistration("4");
        AuthFailure failure = assertThrows(AuthFailure.class, () -> controller.register(body, request));
        assertEquals(400, failure.status);
        verify(auth, never()).register(any());
    }

    @Test
    void registerAcceptsValidCaptchaAndRemovesCaptchaAttributes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        var session = request.getSession(true);
        session.setAttribute("captcha", "3");
        session.setAttribute("captchaExpires", Instant.now().plusSeconds(300));
        AuthService.Registration body = validRegistration("3");
        Map<String, String> response =controller.register(body, request);
        assertEquals("Cuenta creada. Revisa tu correo para verificarla antes de acceder.", response.get("message"));
        assertEquals(null, session.getAttribute("captcha"));
        assertEquals(null, session.getAttribute("captchaExpires"));
        verify(auth).register(body);
    }

    @Test
    void registerAcceptsValidCaptcha() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("captcha", "3");
        session.setAttribute("captchaExpires", Instant.now().plusSeconds(300));
        request.setSession(session);
        AuthService.Registration body = validRegistration("3");
        controller.register(body, request);
        verify(auth).register(body);
        assertNull(session.getAttribute("captcha"));
        assertNull(session.getAttribute("captchaExpires"));
    }

    @Test
    void registerRejectsWhenCaptchaAnswerIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("captchaExpires", Instant.now().plusSeconds(300));
        request.setSession(session);
        AuthFailure failure = assertThrows(AuthFailure.class,() -> controller.register(validRegistration("3"),request));
        assertEquals(400, failure.status);
    }

    @Test
    void registerRejectsWhenCaptchaExpirationIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("captcha", "3");
        request.setSession(session);
        AuthFailure failure = assertThrows(AuthFailure.class,() -> controller.register(validRegistration("3"),request));
        assertEquals(400, failure.status);
    }
    @Test
    void registerRejectsIncorrectCaptcha() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("captcha", "3");
        session.setAttribute("captchaExpires", Instant.now().plusSeconds(300));
        request.setSession(session);
        AuthFailure failure = assertThrows(AuthFailure.class,() -> controller.register(validRegistration("4"),request));
        assertEquals(400, failure.status);
    }

    @Test
    void establishCreatesSessionWhenThereIsNoPreviousSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthService.User user = new AuthService.User(1L,"maria_23","Maria",null);
        when(auth.version(1L)).thenReturn(0);
        controller.establish(user, false, request, response);
        assertEquals(1L, request.getSession(false).getAttribute("authUser"));
    }
    @Test
    void establishInvalidatesPreviousSession()throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession previousSession = new MockHttpSession();
        request.setSession(previousSession);
        AuthService.User user = new AuthService.User(1L, "maria_23", "Maria", null);
        when(auth.version(1L)).thenReturn(0);
        controller.establish(user, false, request, response);
        assertTrue(previousSession.isInvalid());
        MockHttpSession newSession =(MockHttpSession) request.getSession(true);
        assertEquals(Long.valueOf(1L),newSession.getAttribute("authUser"));
        assertEquals(Integer.valueOf(0), newSession.getAttribute("authVersion"));
    }

    @Test
    void requireUserRejectsSessionWithoutDeadline() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authUser", 1L);
        session.setAttribute("authVersion", 0);
        request.setSession(session);
        AuthFailure failure = assertThrows(AuthFailure.class, () -> AuthController.requireUser(request, auth));
        assertEquals(401, failure.status);
    }
    @Test
    void requireUserRejectsSessionWithInvalidDeadlineType() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authUser", 1L);
        session.setAttribute("authDeadline", "not-an-instant");
        session.setAttribute("authVersion", 0);
        request.setSession(session);
        AuthFailure failure = assertThrows(AuthFailure.class, () -> AuthController.requireUser(request, auth));
        assertEquals(401, failure.status);
    }

    @Test
    void requireUserAcceptsValidSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authUser", 1L);
        session.setAttribute("authDeadline",Instant.now().plusSeconds(300));
        session.setAttribute("authVersion", 0);
        request.setSession(session);
        when(auth.version(1L)).thenReturn(0);
        assertEquals(1L, AuthController.requireUser(request, auth));
    }





}

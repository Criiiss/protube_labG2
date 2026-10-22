package com.tecnocampus.LS2.protube_back.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence.SpringDataUserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = { "pro_tube.store.dir=target/test-media", "pro_tube.load_initial_data=false", "spring.datasource.url=jdbc:h2:mem:auth-tests;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false" })
@AutoConfigureMockMvc
class AuthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AuthService auth;
    @Autowired AuthAccounts accounts;
    @Autowired AuthTokens tokens;
    @Autowired SpringDataUserRepository users;
    @MockBean AuthMail mail;
    private final Map<String, String> links = new HashMap<>();
    private String username;
    @BeforeEach void init() {
        username = "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        doAnswer(invocation -> { links.put(invocation.getArgument(0), invocation.getArgument(2)); return null; }).when(mail).send(anyString(), anyString(), anyString());
    }
    private AuthService.Registration registration(String name) {
        return new AuthService.Registration("Test User", name, name + "@example.com", "ValidPass1", "ValidPass1", LocalDate.now().minusYears(20), true, "3");
    }
    private String token() { return links.get(username + "@example.com").split("token=")[1]; }
    private void activeAccount() { auth.register(registration(username)); auth.consume(token(), "verify", null, null); }
    private ResultActions postJson(String path, Object body, MockHttpSession session) throws Exception {
        return mvc.perform(post(path).session(session).header("X-Protube-Request", "1").contentType("application/json").content(json.writeValueAsString(body)));
    }
    @Test void registrationHashesPasswordAndRequiresSingleUseVerification() {
        auth.register(registration(username));
        assertTrue(users.findByUsername(username).orElseThrow().getPasswordHash().startsWith("$2a$"));
        assertThrows(AuthFailure.class, () -> auth.login(username, "ValidPass1"));
        String verification = token(); auth.consume(verification, "verify", null, null);
        assertEquals(username, auth.login(username, "ValidPass1").username());
        assertThrows(AuthFailure.class, () -> auth.consume(verification, "verify", null, null));
    }
    @Test void validationReportsFieldsAndRejectsDuplicate() {
        var error = assertThrows(AuthFailure.class, () -> auth.register(new AuthService.Registration("", "a", "bad", "weak", "different", LocalDate.now(), false, "")));
        assertTrue(error.fields.keySet().containsAll(List.of("name", "username", "email", "password", "confirmPassword", "birthDate", "acceptTerms")));
        auth.register(registration(username)); assertThrows(AuthFailure.class, () -> auth.register(registration(username)));
    }
    @Test void rejectsExpiredTokens() {
        auth.register(registration(username));
        var token = tokens.findAll().stream().filter(t -> t.userId.equals(users.findByUsername(username).orElseThrow().getId())).findFirst().orElseThrow();
        token.expiresAt = OffsetDateTime.now().minusSeconds(1); tokens.save(token);
        assertThrows(AuthFailure.class, () -> auth.consume(token(), "verify", null, null));
    }
    @Test void resetRevokesExistingSessionsAndCannotBeReused() throws Exception {
        activeAccount(); var browser = new MockHttpSession();
        var result = postJson("/api/auth/login", Map.of("identifier", username, "password", "ValidPass1", "remember", true), browser).andExpect(status().isOk()).andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.allOf(org.hamcrest.Matchers.containsString("HttpOnly"), org.hamcrest.Matchers.containsString("Max-Age=2592000")))).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession();
        auth.requestToken(username + "@example.com", "reset"); String reset = token();
        auth.consume(reset, "reset", "ChangedPass2", "ChangedPass2");
        mvc.perform(get("/api/auth/session").session(session)).andExpect(status().isUnauthorized());
        assertThrows(AuthFailure.class, () -> auth.login(username, "ValidPass1"));
        assertEquals(username, auth.login(username + "@example.com", "ChangedPass2").username());
        assertThrows(AuthFailure.class, () -> auth.consume(reset, "reset", "ChangedPass3", "ChangedPass3"));
    }
    @Test void sessionsProtectProfilesAndLogout() throws Exception {
        activeAccount();
        mvc.perform(get("/api/profiles/me").param("userId", "1")).andExpect(status().isUnauthorized());
        var result = postJson("/api/auth/login", Map.of("identifier", username, "password", "ValidPass1"), new MockHttpSession()).andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession();
        long id = users.findByUsername(username).orElseThrow().getId();
        mvc.perform(get("/api/profiles/me").param("userId", "" + id).session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/profiles/me").param("userId", "" + (id + 1000)).session(session)).andExpect(status().isForbidden());
        postJson("/api/auth/logout", Map.of(), session).andExpect(status().isOk()); assertTrue(session.isInvalid());
    }
    @Test void rejectsCsrfInvalidCaptchaAndLimitsFailedLogins() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{}" )).andExpect(status().isForbidden());
        postJson("/api/auth/register", registration(username), new MockHttpSession()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.captcha").exists());
        for (int i = 0; i < 5; i++) postJson("/api/auth/login", Map.of("identifier", username, "password", "invalid"), new MockHttpSession()).andExpect(status().isUnauthorized());
        postJson("/api/auth/login", Map.of("identifier", username, "password", "invalid"), new MockHttpSession()).andExpect(status().isTooManyRequests());
    }
    @Test void mailFailureRollsBackRegistration() {
        doThrow(new AuthFailure(503, "Mail unavailable")).when(mail).send(anyString(), anyString(), anyString());
        assertThrows(AuthFailure.class, () -> auth.register(registration(username)));
        assertFalse(users.existsByUsername(username));
    }
    @Test void browserCanRegisterVerifyResetAndEditOnlyOwnProfile() throws Exception {
        var browser = new MockHttpSession();
        mvc.perform(get("/api/auth/config")).andExpect(status().isOk()).andExpect(jsonPath("$.minimumAge").value(13));
        mvc.perform(get("/api/auth/captcha").session(browser)).andExpect(status().isOk());
        var body = new HashMap<String, Object>();
        body.put("name", "Test User"); body.put("username", username); body.put("email", username + "@example.com");
        body.put("password", "ValidPass1"); body.put("confirmPassword", "ValidPass1"); body.put("birthDate", "2000-01-01"); body.put("acceptTerms", true); body.put("captcha", browser.getAttribute("captcha"));
        postJson("/api/auth/register", body, browser).andExpect(status().isOk());
        postJson("/api/auth/resend", Map.of("email", username + "@example.com"), browser).andExpect(status().isOk());
        postJson("/api/auth/verify", Map.of("token", token()), browser).andExpect(status().isOk());
        postJson("/api/auth/forgot", Map.of("email", username + "@example.com"), browser).andExpect(status().isOk());
        postJson("/api/auth/reset", Map.of("token", token(), "password", "NewValidPass2", "confirmPassword", "NewValidPass2"), browser).andExpect(status().isOk());
        var result = postJson("/api/auth/login", Map.of("identifier", username, "password", "NewValidPass2"), browser).andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession();
        long id = users.findByUsername(username).orElseThrow().getId();
        mvc.perform(patch("/api/profiles/me").param("userId", "" + id).session(session).header("X-Protube-Request", "1").contentType("application/json").content("{\"displayName\":\"Updated\",\"biography\":\"My bio\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Updated"));
        mvc.perform(patch("/api/profiles/me/username").param("userId", "" + id).session(session).header("X-Protube-Request", "1").contentType("application/json").content("{\"username\":\"" + username + "_new\"}")).andExpect(status().isOk());
        mvc.perform(patch("/api/profiles/me/privacy").param("userId", "" + id).session(session).header("X-Protube-Request", "1").contentType("application/json").content("{\"subscriptionsVisibility\":\"PRIVATE\",\"likedVideosVisibility\":\"PRIVATE\"}")).andExpect(status().isOk());
    }
}

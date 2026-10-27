package com.tecnocampus.LS2.protube_back.auth;

import com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.mock.web.*;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GoogleAuthTest {
    private SpringDataUserRepository users;
    private AuthAccounts accounts;
    private AuthService auth;
    private GoogleAuth google;
    private MockRestServiceServer server;
    private jakarta.persistence.EntityManager em;
    @BeforeEach void init() {
        users = mock(SpringDataUserRepository.class); accounts = mock(AuthAccounts.class); auth = mock(AuthService.class); em = mock(jakarta.persistence.EntityManager.class);
        var builder = RestClient.builder(); server = MockRestServiceServer.bindTo(builder).build();
        google = new GoogleAuth("id", "secret", "http://localhost/callback", "http://localhost:5173", users, mock(SpringDataProfileRepository.class), accounts, auth, builder);
        ReflectionTestUtils.setField(google, "entityManager", em);
        when(auth.minimumAge()).thenReturn(13);
    }
    private MockHttpServletRequest request() {
        var request = new MockHttpServletRequest(); google.start(request); return request;
    }
    private void identity() {
        server.expect(requestTo("https://oauth2.googleapis.com/token")).andRespond(withSuccess("{\"access_token\":\"test-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://www.googleapis.com/oauth2/v3/userinfo")).andExpect(header("Authorization", "Bearer test-token")).andRespond(withSuccess("{\"sub\":\"google-subject\",\"email\":\"test@example.com\",\"email_verified\":true,\"name\":\"Google Test\"}", MediaType.APPLICATION_JSON));
    }
    @Test void createsGoogleAccountOnlyAfterAgeAndTermsValidation() {
        var request = request(); identity();
        assertNull(google.callback("code", (String) request.getSession().getAttribute("googleState"), request));
        var failure = assertThrows(AuthFailure.class, () -> google.complete(new AuthController.GoogleRegistration("bad", LocalDate.now(), false), request));
        assertTrue(failure.fields.containsKey("birthDate")); verify(users, never()).saveAndFlush(any());
        when(users.saveAndFlush(any())).thenAnswer(invocation -> { UserJpaEntity original = invocation.getArgument(0); return new UserJpaEntity(10L, original.getUsername(), original.getEmail(), null, "GOOGLE", original.getProviderId(), OffsetDateTime.now(), OffsetDateTime.now()); });
        when(auth.user(10L)).thenReturn(new AuthService.User(10L, "google_test", "Google Test", null));
        assertEquals(10L, google.complete(new AuthController.GoogleRegistration("google_test", LocalDate.of(2000,1,1), true), request).id());
        verify(em).persist(any(ProfileJpaEntity.class)); verify(accounts).save(argThat(account -> account.emailVerified));
        assertThrows(AuthFailure.class, () -> google.complete(new AuthController.GoogleRegistration("google_test", LocalDate.of(2000,1,1), true), request));
        server.verify();
    }
    @Test void existingGoogleIdentitySignsInWithoutLinkingLocalAccounts() {
        var request = request(); identity();
        when(users.findByAuthProviderAndProviderId("GOOGLE", "google-subject")).thenReturn(Optional.of(new UserJpaEntity(9L,"existing","test@example.com",null,"GOOGLE","google-subject",OffsetDateTime.now(),OffsetDateTime.now())));
        when(auth.user(9L)).thenReturn(new AuthService.User(9L,"existing","Existing",null));
        assertEquals(9L, google.callback("code", (String) request.getSession().getAttribute("googleState"), request).id());
        verify(users, never()).saveAndFlush(any()); server.verify();
    }
    @Test void rejectsForgedOrExpiredStateBeforeContactingGoogle() {
        var forged = request(); assertThrows(AuthFailure.class, () -> google.callback("code", "forged", forged));
        var expired = request(); expired.getSession().setAttribute("googleExpires", Instant.now().minusSeconds(1));
        assertThrows(AuthFailure.class, () -> google.callback("code", (String) expired.getSession().getAttribute("googleState"), expired));
        server.verify();
    }
}

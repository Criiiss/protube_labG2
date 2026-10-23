package com.tecnocampus.LS2.protube_back.auth;
import com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence.*;
import com.tecnocampus.LS2.protube_back.profile.domain.model.PrivacyVisibility;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.*;
import java.security.SecureRandom;
import java.util.*;

@Service
public class GoogleAuth {
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private record Identity(String sub, String email, String name, Instant expires) {}
    private final String clientId, secret, callback, frontend;
    private final SpringDataUserRepository users;
    private final SpringDataProfileRepository profiles;
    private final AuthAccounts accounts;
    private final AuthService auth;
    private final RestClient.Builder clientBuilder;
    public GoogleAuth(@Value("${ENV_PROTUBE_GOOGLE_CLIENT_ID:}") String clientId, @Value("${ENV_PROTUBE_GOOGLE_CLIENT_SECRET:}") String secret,
        @Value("${protube.auth.google-callback:http://localhost:8080/api/auth/google/callback}") String callback,
        @Value("${protube.auth.frontend-url:http://localhost:5173}") String frontend,
        SpringDataUserRepository users, SpringDataProfileRepository profiles, AuthAccounts accounts, AuthService auth, RestClient.Builder clientBuilder) {
        this.clientId = clientId; this.secret = secret; this.callback = callback; this.frontend = frontend;
        this.users = users; this.profiles = profiles; this.accounts = accounts; this.auth = auth;
        this.clientBuilder = clientBuilder;
    }
    public boolean enabled() { return !clientId.isBlank() && !secret.isBlank(); }
    public String frontend() { return frontend; }
    public String start(HttpServletRequest request) {
        if (!enabled()) throw new AuthFailure(503, "El acceso con Google no está configurado.");
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        request.getSession().setAttribute("googleState", state);
        request.getSession().setAttribute("googleExpires", Instant.now().plusSeconds(300));
        return UriComponentsBuilder.fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth").queryParam("client_id", clientId)
            .queryParam("redirect_uri", callback).queryParam("response_type", "code").queryParam("scope", "openid email profile").queryParam("state", state).build().encode().toUriString();
    }
    @Transactional
    public AuthService.User callback(String code, String state, HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null) throw new AuthFailure(400, "Sesión de Google no válida.");
        Object expected = session.getAttribute("googleState"); Object deadline = session.getAttribute("googleExpires");
        session.removeAttribute("googleState"); session.removeAttribute("googleExpires");
        if (code == null || state == null || !state.equals(expected) || !(deadline instanceof Instant expires) || expires.isBefore(Instant.now())) throw new AuthFailure(400, "Acceso con Google caducado.");
        var form = new LinkedMultiValueMap<String, String>(); form.add("code", code); form.add("client_id", clientId);
        form.add("client_secret", secret); form.add("redirect_uri", callback); form.add("grant_type", "authorization_code");
        var client = clientBuilder.build();
        var token = client.post().uri("https://oauth2.googleapis.com/token").contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(com.fasterxml.jackson.databind.JsonNode.class);
        if (token == null || !token.hasNonNull("access_token")) throw new AuthFailure(401, "Google no pudo validar el acceso.");
        var info = client.get().uri("https://www.googleapis.com/oauth2/v3/userinfo").headers(h -> h.setBearerAuth(token.get("access_token").asText())).retrieve().body(com.fasterxml.jackson.databind.JsonNode.class);
        if (info == null || !info.path("email_verified").asBoolean() || info.path("sub").asText().isBlank()) throw new AuthFailure(401, "Google no ha verificado el email.");
        String sub = info.get("sub").asText(), email = info.path("email").asText().toLowerCase(Locale.ROOT);
        var existing = users.findByAuthProviderAndProviderId("GOOGLE", sub);
        if (existing.isPresent()) return auth.user(existing.get().getId());
        if (users.existsByEmail(email)) throw new AuthFailure(409, "Ya existe una cuenta con este email. Accede con tu contraseña.");
        session.setAttribute("googleIdentity", new Identity(sub, email, info.path("name").asText("Usuario"), Instant.now().plusSeconds(600)));
        return null;
    }
    @Transactional
    public AuthService.User complete(AuthController.GoogleRegistration request, HttpServletRequest http) {
        var session = http.getSession(false);
        if (session == null || !(session.getAttribute("googleIdentity") instanceof Identity identity) || identity.expires.isBefore(Instant.now())) throw new AuthFailure(401, "Vuelve a iniciar el acceso con Google.");
        var errors = new LinkedHashMap<String, String>();
        String username = Objects.toString(request.username(), "").trim().toLowerCase(Locale.ROOT);
        if (!username.matches("[a-z0-9_]{3,30}") || users.existsByUsernameIgnoreCase(username)) errors.put("username", "Elige un usuario disponible de 3–30 letras, números o guion bajo.");
        if (request.birthDate() == null || request.birthDate().isAfter(LocalDate.now().minusYears(auth.minimumAge()))) errors.put("birthDate", "No cumples la edad mínima de " + auth.minimumAge() + " años.");
        if (!request.acceptTerms()) errors.put("acceptTerms", "Debes aceptar los términos.");
        if (!errors.isEmpty()) throw new AuthFailure(400, "Revisa los campos.", errors);
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var entity = users.saveAndFlush(new UserJpaEntity(null, username, identity.email, null, "GOOGLE", identity.sub, now, now));
        entityManager.persist(new ProfileJpaEntity(entity, identity.name.substring(0, Math.min(50, identity.name.length())), null, null, null, now, PrivacyVisibility.PRIVATE, PrivacyVisibility.PRIVATE));
        var account = new AuthAccount(); account.userId = entity.getId(); account.birthDate = request.birthDate(); account.emailVerified = true; account.termsAcceptedAt = now; accounts.save(account);
        session.removeAttribute("googleIdentity"); return auth.user(entity.getId());
    }
}

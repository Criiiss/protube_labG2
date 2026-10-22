package com.tecnocampus.LS2.protube_back.auth;

import com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence.*;
import com.tecnocampus.LS2.protube_back.profile.domain.model.PrivacyVisibility;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class AuthService {
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    public record Registration(String name, String username, String email, String password,
        String confirmPassword, LocalDate birthDate, boolean acceptTerms, String captcha) {}
    public record User(Long id, String username, String displayName, String avatarUrl) {}
    private final SpringDataUserRepository users;
    private final SpringDataProfileRepository profiles;
    private final AuthAccounts accounts;
    private final AuthTokens tokens;
    private final AuthMail mail;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    private final String dummyHash = encoder.encode("invalid-credential");
    private final String frontend;
    private final int minAge;
    public AuthService(SpringDataUserRepository users, SpringDataProfileRepository profiles, AuthAccounts accounts,
        AuthTokens tokens, AuthMail mail, @Value("${protube.auth.frontend-url:http://localhost:5173}") String frontend,
        @Value("${protube.auth.minimum-age:13}") int minAge) {
        this.users = users; this.profiles = profiles; this.accounts = accounts; this.tokens = tokens;
        this.mail = mail; this.frontend = frontend; this.minAge = minAge;
    }
    public int minimumAge() { return minAge; }
    public static void validatePassword(String password, Map<String, String> errors) {
        if (password == null || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72
            || !password.matches("(?s).*[A-Z].*") || !password.matches("(?s).*[0-9].*"))
            errors.put("password", "Usa entre 8 y 72 bytes, una mayúscula y un número.");
    }
    @Transactional
    public void register(Registration request) {
        var errors = new LinkedHashMap<String, String>();
        String username = request.username() == null ? "" : request.username().trim().toLowerCase(Locale.ROOT);
        String email = request.email() == null ? "" : request.email().trim().toLowerCase(Locale.ROOT);
        if (request.name() == null || request.name().isBlank() || request.name().trim().length() > 50) errors.put("name", "Introduce un nombre de hasta 50 caracteres.");
        if (!username.matches("[a-z0-9_]{3,30}")) errors.put("username", "Usa 3–30 letras, números o guion bajo.");
        else if (users.existsByUsernameIgnoreCase(username)) errors.put("username", "Este nombre de usuario ya existe.");
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length() > 255) errors.put("email", "Introduce un email válido.");
        else if (users.existsByEmail(email)) errors.put("email", "Este email ya está registrado.");
        validatePassword(request.password(), errors);
        if (!Objects.equals(request.password(), request.confirmPassword())) errors.put("confirmPassword", "Las contraseñas no coinciden.");
        if (request.birthDate() == null || request.birthDate().isAfter(LocalDate.now().minusYears(minAge))) errors.put("birthDate", "Debes tener al menos " + minAge + " años.");
        if (!request.acceptTerms()) errors.put("acceptTerms", "Debes aceptar los términos y condiciones.");
        if (!errors.isEmpty()) throw new AuthFailure(400, "Revisa los campos indicados.", errors);
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var entity = users.saveAndFlush(new UserJpaEntity(null, username, email, encoder.encode(request.password()), "LOCAL", null, now, now));
        entityManager.persist(new ProfileJpaEntity(entity, request.name().trim(), null, null, null, now, PrivacyVisibility.PRIVATE, PrivacyVisibility.PRIVATE));
        var account = new AuthAccount(); account.userId = entity.getId(); account.birthDate = request.birthDate(); account.termsAcceptedAt = now;
        accounts.save(account);
        sendToken(entity, "verify", 86400);
    }
    public User user(Long id) {
        var entity = users.findById(id).orElseThrow(() -> new AuthFailure(401, "Inicia sesión de nuevo."));
        var profile = profiles.findById(id);
        return new User(id, entity.getUsername(), profile.map(ProfileJpaEntity::getDisplayName).orElse(entity.getUsername()), profile.map(ProfileJpaEntity::getAvatarUrl).orElse(null));
    }
    public User login(String identifier, String password) {
        String key = identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
        var entity = key.contains("@") ? users.findByEmail(key) : users.findByUsernameIgnoreCase(key);
        String hash = entity.map(UserJpaEntity::getPasswordHash).orElse(dummyHash);
        boolean matches = encoder.matches(password == null ? "" : password, hash == null ? dummyHash : hash);
        if (!matches || entity.isEmpty() || !"LOCAL".equals(entity.get().getAuthProvider())) throw new AuthFailure(401, "Email, usuario o contraseña incorrectos.");
        var account = accounts.findById(entity.get().getId()).orElseThrow(() -> new AuthFailure(401, "Esta cuenta no está habilitada para iniciar sesión."));
        if (!account.emailVerified) throw new AuthFailure(403, "Verifica tu email antes de iniciar sesión.");
        return user(account.userId);
    }
    @Transactional
    public void requestToken(String email, String purpose) {
        if (email == null) return;
        users.findByEmail(email.trim().toLowerCase(Locale.ROOT)).filter(u -> "LOCAL".equals(u.getAuthProvider())).ifPresent(user -> {
            var account = accounts.findById(user.getId());
            if (account.isPresent() && (purpose.equals("reset") || !account.get().emailVerified)) sendToken(user, purpose, purpose.equals("reset") ? 1800 : 86400);
        });
    }
    private void sendToken(UserJpaEntity user, String purpose, int seconds) {
        byte[] random = new byte[32]; new SecureRandom().nextBytes(random);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        tokens.deleteByUserIdAndPurpose(user.getId(), purpose);
        var token = new AuthToken(); token.tokenHash = hash(raw); token.userId = user.getId(); token.purpose = purpose;
        token.expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(seconds); tokens.save(token);
        mail.send(user.getEmail(), purpose.equals("verify") ? "Verifica tu cuenta de protube" : "Restablece tu contraseña de protube", frontend + "/#/" + purpose + "?token=" + raw);
    }
    @Transactional
    public void consume(String raw, String purpose, String password, String confirmation) {
        var token = tokens.lock(hash(raw == null ? "" : raw)).orElseThrow(() -> new AuthFailure(400, "El enlace no es válido o ya se ha utilizado."));
        if (!token.purpose.equals(purpose) || token.expiresAt.isBefore(OffsetDateTime.now(ZoneOffset.UTC))) throw new AuthFailure(400, "El enlace ha caducado. Solicita uno nuevo.");
        var account = accounts.findById(token.userId).orElseThrow(() -> new AuthFailure(400, "Cuenta no disponible."));
        if (purpose.equals("reset")) {
            var errors = new LinkedHashMap<String, String>(); validatePassword(password, errors);
            if (!Objects.equals(password, confirmation)) errors.put("confirmPassword", "Las contraseñas no coinciden.");
            if (!errors.isEmpty()) throw new AuthFailure(400, "Revisa la contraseña.", errors);
            var user = users.findById(token.userId).orElseThrow(); user.changePasswordHash(encoder.encode(password)); users.save(user);
            account.sessionVersion++;
        } else account.emailVerified = true;
        accounts.save(account); tokens.delete(token);
    }
    public int version(Long id) { return accounts.findById(id).map(a -> a.sessionVersion).orElse(-1); }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}

package com.tecnocampus.LS2.protube_back.auth;

import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final AttemptLimiter limiter;
    private final GoogleAuth google;
    private final boolean secure;
    public AuthController(AuthService auth, AttemptLimiter limiter, GoogleAuth google,
        @Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
        this.auth = auth; this.limiter = limiter; this.google = google; this.secure = secure;
    }
    public record Credentials(String identifier, String password, boolean remember) {}
    public record Email(String email) {}
    public record TokenRequest(String token, String password, String confirmPassword) {}
    public record GoogleRegistration(String username, LocalDate birthDate, boolean acceptTerms) {}
    @GetMapping("/config") public Map<String, Object> config() { return Map.of("minimumAge", auth.minimumAge(), "googleEnabled", google.enabled()); }
    @GetMapping("/captcha") public Map<String, String> captcha(HttpServletRequest request) {
        limiter.check("captcha:" + request.getRemoteAddr(), 60);
        var random = new SecureRandom(); int a = random.nextInt(9) + 1, b = random.nextInt(9) + 1;
        var session = request.getSession(); session.setAttribute("captcha", Integer.toString(a + b));
        session.setAttribute("captchaExpires", Instant.now().plusSeconds(300));
        return Map.of("question", "¿Cuánto es " + a + " + " + b + "?");
    }
    @PostMapping("/register") public Map<String, String> register(@RequestBody AuthService.Registration body, HttpServletRequest request) {
        limiter.check("register:" + request.getRemoteAddr(), 10);
        var session = request.getSession(false);
        Object answer = session == null ? null : session.getAttribute("captcha");
        Instant expires = session == null ? null : (Instant) session.getAttribute("captchaExpires");
        if (session != null) { session.removeAttribute("captcha"); session.removeAttribute("captchaExpires"); }
        if (answer == null || expires == null || expires.isBefore(Instant.now()) || !answer.equals(body.captcha()))
            throw new AuthFailure(400, "Resuelve de nuevo la comprobación.", Map.of("captcha", "Respuesta incorrecta o caducada."));
        auth.register(body); return Map.of("message", "Cuenta creada. Revisa tu correo para verificarla antes de acceder.");
    }
    @PostMapping("/login") public AuthService.User login(@RequestBody Credentials body, HttpServletRequest request, HttpServletResponse response) {
        limiter.check("login-ip:" + request.getRemoteAddr(), 30);
        String key = "login-user:" + Objects.toString(body.identifier(), "").trim().toLowerCase(Locale.ROOT);
        limiter.check(key, 5);
        var user = auth.login(body.identifier(), body.password()); limiter.clear(key);
        establish(user, body.remember(), request, response); return user;
    }
    void establish(AuthService.User user, boolean remember, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        var session = request.getSession(true); session.setAttribute("authUser", user.id());
        session.setAttribute("authVersion", auth.version(user.id()));
        int seconds = remember ? 30 * 86400 : 1800;
        session.setMaxInactiveInterval(seconds); session.setAttribute("authDeadline", Instant.now().plusSeconds(seconds));
        var cookie = ResponseCookie.from("JSESSIONID", session.getId()).httpOnly(true).secure(secure).sameSite("Lax").path("/");
        if (remember) cookie.maxAge(Duration.ofSeconds(seconds));
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.build().toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }
    @GetMapping("/session") public AuthService.User session(HttpServletRequest request) { return auth.user(requireUser(request, auth)); }
    @PostMapping("/logout") public Map<String, String> logout(HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("JSESSIONID", "").path("/").httpOnly(true).secure(secure).sameSite("Lax").maxAge(0).build().toString());
        return Map.of("message", "Sesión cerrada.");
    }
    @PostMapping("/forgot") public Map<String, String> forgot(@RequestBody Email body, HttpServletRequest request) { return tokenEmail(body, request, "reset"); }
    @PostMapping("/resend") public Map<String, String> resend(@RequestBody Email body, HttpServletRequest request) { return tokenEmail(body, request, "verify"); }
    private Map<String, String> tokenEmail(Email body, HttpServletRequest request, String purpose) {
        limiter.check("mail:" + request.getRemoteAddr(), 5);
        auth.requestToken(body.email(), purpose); return Map.of("message", "Si la cuenta corresponde, recibirás un enlace por correo.");
    }
    @PostMapping("/verify") public Map<String, String> verify(@RequestBody TokenRequest body) { auth.consume(body.token(), "verify", null, null); return Map.of("message", "Email verificado. Ya puedes iniciar sesión."); }
    @PostMapping("/reset") public Map<String, String> reset(@RequestBody TokenRequest body) { auth.consume(body.token(), "reset", body.password(), body.confirmPassword()); return Map.of("message", "Contraseña actualizada. Inicia sesión de nuevo."); }
    @GetMapping("/google") public void google(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException { response.sendRedirect(google.start(request)); }
    @GetMapping("/google/callback") public void callback(@RequestParam(required = false) String code, @RequestParam(required = false) String state,
        HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        try {
            var result = google.callback(code, state, request);
            if (result != null) establish(result, false, request, response);
            response.sendRedirect(google.frontend() + (result == null ? "/#/google-register" : "/#/"));
        } catch (RuntimeException ex) { response.sendRedirect(google.frontend() + "/#/login?error=google"); }
    }
    @PostMapping("/google/complete") public AuthService.User complete(@RequestBody GoogleRegistration body, HttpServletRequest request, HttpServletResponse response) {
        var user = google.complete(body, request); establish(user, false, request, response); return user;
    }
    public static Long requireUser(HttpServletRequest request, AuthService auth) {
        var session = request.getSession(false);
        if (session == null || !(session.getAttribute("authUser") instanceof Long id)) throw new AuthFailure(401, "Inicia sesión para continuar.");
        if (!(session.getAttribute("authDeadline") instanceof Instant deadline) || deadline.isBefore(Instant.now()) || !Objects.equals(session.getAttribute("authVersion"), auth.version(id))) {
            session.invalidate(); throw new AuthFailure(401, "Tu sesión ha caducado. Inicia sesión de nuevo.");
        }
        return id;
    }
}

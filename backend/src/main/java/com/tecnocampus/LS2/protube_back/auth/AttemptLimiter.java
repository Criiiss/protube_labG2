package com.tecnocampus.LS2.protube_back.auth;
import java.time.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
@Component
public class AttemptLimiter {
    private record Window(int attempts, Instant expires) {}
    private final ConcurrentHashMap<String, Window> attempts = new ConcurrentHashMap<>();
    public void check(String key, int limit) {
        var now = Instant.now();
        attempts.entrySet().removeIf(entry -> entry.getValue().expires.isBefore(now));
        if (attempts.size() >= 10000 && !attempts.containsKey(key)) throw new AuthFailure(429, "Demasiadas solicitudes. Espera unos minutos.");
        var value = attempts.compute(key, (ignored, previous) -> previous == null
            ? new Window(1, now.plusSeconds(900)) : new Window(previous.attempts + 1, previous.expires));
        if (value.attempts > limit) throw new AuthFailure(429, "Demasiados intentos. Vuelve a intentarlo en 15 minutos.");
    }
    public void clear(String key) { attempts.remove(key); }
}

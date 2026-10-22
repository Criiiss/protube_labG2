package com.tecnocampus.LS2.protube_back.auth;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Cookie sessions require a non-simple header on API mutations, including login (CSRF). */
@Component
public class AuthGuard extends OncePerRequestFilter {
    private final AuthService auth;
    public AuthGuard(AuthService auth) { this.auth = auth; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        try {
            if (path.startsWith("/api/auth") || path.startsWith("/api/profiles/me")) {
                response.setHeader("Cache-Control", "no-store");
                if (!java.util.Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod()) && !"1".equals(request.getHeader("X-Protube-Request")))
                    throw new AuthFailure(403, "Solicitud no permitida. Recarga la página.");
            }
            if (path.startsWith("/api/profiles/me") && !request.getMethod().equals("OPTIONS")) {
                Long id = AuthController.requireUser(request, auth);
                if (!id.toString().equals(request.getParameter("userId"))) throw new AuthFailure(403, "Solo puedes acceder a tu propio perfil.");
            }
            chain.doFilter(request, response);
        } catch (AuthFailure ex) {
            response.setStatus(ex.status); response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("message", ex.getMessage())));
        }
    }
}

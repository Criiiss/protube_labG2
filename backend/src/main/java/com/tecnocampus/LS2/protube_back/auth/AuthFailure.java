package com.tecnocampus.LS2.protube_back.auth;
import java.util.Map;
public class AuthFailure extends RuntimeException {
    public final int status;
    public final Map<String, String> fields;
    public AuthFailure(int status, String message) { this(status, message, Map.of()); }
    public AuthFailure(int status, String message, Map<String, String> fields) {
        super(message); this.status = status; this.fields = fields;
    }
}

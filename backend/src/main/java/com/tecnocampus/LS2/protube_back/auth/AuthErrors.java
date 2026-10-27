package com.tecnocampus.LS2.protube_back.auth;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.annotation.Order;
@RestControllerAdvice(assignableTypes = AuthController.class)
@Order(0)
public class AuthErrors {
    @ExceptionHandler(AuthFailure.class) public ResponseEntity<?> failure(AuthFailure ex) {
        return ResponseEntity.status(ex.status).body(Map.of("message", ex.getMessage(), "fields", ex.fields));
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<?> duplicate() { return ResponseEntity.status(409).body(Map.of("message", "El usuario o email ya existe. Revisa los datos.")); }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<?> invalid() { return ResponseEntity.badRequest().body(Map.of("message", "Los datos enviados no son válidos. Revisa la fecha y los campos.")); }
}

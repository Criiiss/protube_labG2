package com.tecnocampus.LS2.protube_back.auth;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
@Entity
@Table(name = "auth_tokens")
public class AuthToken {
    @Id @Column(length = 64) public String tokenHash;
    @Column(nullable = false) public Long userId;
    @Column(nullable = false, length = 20) public String purpose;
    @Column(nullable = false) public OffsetDateTime expiresAt;
}

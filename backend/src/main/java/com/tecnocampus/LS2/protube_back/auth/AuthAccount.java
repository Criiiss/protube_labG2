package com.tecnocampus.LS2.protube_back.auth;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "auth_accounts")
public class AuthAccount {
    @Id public Long userId;
    public LocalDate birthDate;
    @Column(nullable = false) public OffsetDateTime termsAcceptedAt;
    @Column(nullable = false) public boolean emailVerified;
    @Column(nullable = false) public int sessionVersion;
}

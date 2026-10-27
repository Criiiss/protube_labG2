package com.tecnocampus.LS2.protube_back.auth;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuthAccounts extends JpaRepository<AuthAccount, Long> {}

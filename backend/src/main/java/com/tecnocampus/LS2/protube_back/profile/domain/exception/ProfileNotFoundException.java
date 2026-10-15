package com.tecnocampus.LS2.protube_back.profile.domain.exception;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException(String userName) {
        super("Profile for user " + userName + " not found");
    }

    public ProfileNotFoundException(Long id) {
        super("Profile for user with id " + id + " not found");
    }
}

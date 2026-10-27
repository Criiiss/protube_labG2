package com.tecnocampus.LS2.protube_back.profile.domain.exception;

public class InvalidUsernameException extends RuntimeException {
    public InvalidUsernameException(String username, String msg) {
        super("Invalid username: " + username + " - " + msg);
    }
}

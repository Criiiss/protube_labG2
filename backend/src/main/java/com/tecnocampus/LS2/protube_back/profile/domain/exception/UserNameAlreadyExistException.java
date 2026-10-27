package com.tecnocampus.LS2.protube_back.profile.domain.exception;

public class UserNameAlreadyExistException extends RuntimeException{
    public UserNameAlreadyExistException(String userName) {
        super("Username " + userName + " already exists");
    }
}

package com.tecnocampus.LS2.protube_back.profile.adapter.in.web;

import com.tecnocampus.LS2.protube_back.profile.domain.exception.InvalidUsernameException;
import com.tecnocampus.LS2.protube_back.profile.domain.exception.ProfileNotFoundException;
import com.tecnocampus.LS2.protube_back.profile.domain.exception.UserNameAlreadyExistException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    public record ErrorResponse(String code, String message) {}

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProfileNotFound(ProfileNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("PROFILE_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(UserNameAlreadyExistException.class)
    public ResponseEntity<ErrorResponse> handleUserNameAlreadyExist(UserNameAlreadyExistException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("USERNAME_ALREADY_EXIST", e.getMessage()));
    }

    @ExceptionHandler(InvalidUsernameException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUsername(InvalidUsernameException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("INVALID_USERNAME", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
    }
}

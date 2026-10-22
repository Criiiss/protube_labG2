package com.tecnocampus.LS2.protube_back.profile.adapter.in;

import com.tecnocampus.LS2.protube_back.profile.adapter.in.web.GlobalExceptionHandler;
import com.tecnocampus.LS2.protube_back.profile.domain.exception.ProfileNotFoundException;
import com.tecnocampus.LS2.protube_back.profile.domain.exception.UserNameAlreadyExistException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    public void convertsProfileNotFoundTo404(){
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler.handleProfileNotFound(new ProfileNotFoundException((long)1D));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("PROFILE_NOT_FOUND", response.getBody().code());
    }

    @Test
    public void convertsDuplicatedUsernameTo409(){
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler.handleUserNameAlreadyExist(new UserNameAlreadyExistException("maria_23"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("USERNAME_ALREADY_EXIST", response.getBody().code());
    }
}

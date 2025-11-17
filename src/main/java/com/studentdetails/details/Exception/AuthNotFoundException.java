package com.studentdetails.details.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED) //
public class AuthNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AuthNotFoundException(String message) {
        super(message);
    }

    public AuthNotFoundException() {
        super("Authentication credentials not found or invalid.");
    }
}

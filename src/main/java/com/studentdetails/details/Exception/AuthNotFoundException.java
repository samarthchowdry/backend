package com.studentdetails.details.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

/**
 * Custom exception thrown when authentication fails or user is not authorized.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AuthNotFoundException(String message) {
        super(message);
    }

}

package com.studentdetails.details.Service.ServiceImpl;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when a Student with a given ID is not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class StudentNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public StudentNotFoundException(Long id) {
        super("Student not found with id: " + id);
    }

    public StudentNotFoundException(String message) {
        super(message);
    }

    public StudentNotFoundException(Long id, Throwable cause) {
        super("Student not found with id: " + id, cause);
    }
}

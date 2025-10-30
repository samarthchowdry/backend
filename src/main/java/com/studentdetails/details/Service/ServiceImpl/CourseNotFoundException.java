package com.studentdetails.details.Service.ServiceImpl;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when a Course with a given ID is not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class CourseNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CourseNotFoundException(Long id) {
        super("Course not found with id: " + id);
    }

    public CourseNotFoundException(String message) {
        super(message);
    }

    public CourseNotFoundException(Long id, Throwable cause) {
        super("Course not found with id: " + id, cause);
    }
}

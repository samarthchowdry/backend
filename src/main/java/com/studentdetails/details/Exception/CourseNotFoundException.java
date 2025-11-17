package com.studentdetails.details.Exception;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.NOT_FOUND)
public class CourseNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CourseNotFoundException(Long id) {
        super("Course not found with id: " + id);
    }

}

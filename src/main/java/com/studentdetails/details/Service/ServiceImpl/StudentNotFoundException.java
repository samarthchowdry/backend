package com.studentdetails.details.Service.ServiceImpl;
public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(Long id) {
        super("Student not found with id: " + id);

    }
}

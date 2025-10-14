package com.studentdetails.details.Domain;

import java.time.LocalDate;

public class Student {
    private Long id;
    private String name;
    private LocalDate dob;

    public Student() {}

    public Student(String name, LocalDate dob) {
        this.name = name;
        this.dob = dob;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
}

package com.studentdetails.details.DTO;
//package com.studentdetails.details.DTO;
//
//public class StudentDTO {
//    private Long id;
//    private String name;
//    private String dob; // You can use String or LocalDate
//
//    // Constructor
//    public StudentDTO(Long id, String name, String dob) {
//        this.id = id;
//        this.name = name;
//        this.dob = dob;
//    }
//
//    // Getters and Setters
//    public Long getId() { return id; }
//    public void setId(Long id) { this.id = id; }
//
//    public String getName() { return name; }
//    public void setName(String name) { this.name = name; }
//
//    public String getDob() { return dob; }
//    public void setDob(String dob) { this.dob = dob; }
//}

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO{
    private Long id;
    private String name;
    private LocalDate dob;
    private String email;
    private String address;
    private String branch;
}

package com.studentdetails.details.Repository;

import com.studentdetails.details.Domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("SELECT s FROM Student s WHERE " +
            "(:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:email IS NULL OR LOWER(s.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "(:dob IS NULL OR s.dob = :dob) AND " +
            "(:branch IS NULL OR LOWER(s.branch) LIKE LOWER(CONCAT('%', :branch, '%')))")
    List<Student> findByFilters(@Param("name") String name,
                                @Param("dob") LocalDate dob,
                                @Param("email") String email,
                                @Param("branch") String branch);

}

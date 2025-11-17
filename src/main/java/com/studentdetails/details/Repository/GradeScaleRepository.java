package com.studentdetails.details.Repository;

import com.studentdetails.details.Domain.GradeScale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GradeScaleRepository extends JpaRepository<GradeScale, Long> {
    List<GradeScale> findAllByOrderByMinPercentageDesc();
}


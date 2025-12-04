package com.studentdetails.details.Repository;

import com.studentdetails.details.Domain.ReportScheduleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportScheduleConfigRepository extends JpaRepository<ReportScheduleConfig, Long> {
}



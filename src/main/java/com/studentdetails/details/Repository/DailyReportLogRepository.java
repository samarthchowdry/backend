package com.studentdetails.details.Repository;

import com.studentdetails.details.Domain.DailyReportLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyReportLogRepository extends JpaRepository<DailyReportLog, Long> {
    List<DailyReportLog> findAllByOrderByReportDateDesc();

    boolean existsByReportDate(LocalDate reportDate);
}



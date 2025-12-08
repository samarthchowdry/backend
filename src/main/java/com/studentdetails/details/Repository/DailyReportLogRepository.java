package com.studentdetails.details.Repository;

import com.studentdetails.details.Domain.DailyReportLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyReportLogRepository extends JpaRepository<DailyReportLog, Long> {

}



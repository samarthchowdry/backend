package com.studentdetails.details.Repository;

import com.studentdetails.details.Domain.EmailNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {
    List<EmailNotification> findAllByOrderBySentTimeDesc();

    List<EmailNotification> findTop100ByStatusInOrderByIdAsc(List<EmailNotification.EmailStatus> statuses);
}


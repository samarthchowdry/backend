package com.studentdetails.details.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that scans for pending/failed emails and sends them asynchronously.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailScheduler {

    private final EmailService emailService;

    /**
     * Runs once a day at 06:00 AM server time.
     * Cron format: second minute hour day-of-month month day-of-week
     */
    // for  a day
    // @Scheduled(cron = "0 0 6 * * *")
    //for  1 minute testing only
    @Scheduled(cron = "0 * * * * *") // every minute

    public void processPendingEmailsDaily() {
        log.info("Starting scheduled job: processPendingEmailsDaily");
        emailService.processPendingEmails();
        log.info("Finished scheduled job: processPendingEmailsDaily");
    }
}



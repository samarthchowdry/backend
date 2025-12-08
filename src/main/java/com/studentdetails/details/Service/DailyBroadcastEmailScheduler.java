package com.studentdetails.details.Service;

import com.studentdetails.details.Domain.BroadcastEmailTemplate;
import com.studentdetails.details.Repository.BroadcastEmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


// Scheduler that sends a broadcast email to all students every day at 11:00 PM,
// using the latest subject and message stored in the sendbroadcastemailtable.

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyBroadcastEmailScheduler {

    private final BroadcastEmailTemplateRepository broadcastEmailTemplateRepository;
    private final AdminCommunicationService adminCommunicationService;
// Runs once a day at 11:00 PM, using the application's configured time zone.
// Cron format: second minute hour day-of-month month day-of-week

    @Scheduled(cron = "0 0 23 * * *")
    public void sendDailyBroadcastEmail() {
        BroadcastEmailTemplate latestTemplate = broadcastEmailTemplateRepository.findTopByOrderByIdDesc();

        if (latestTemplate == null) {
            log.info("No broadcast email template found. Skipping daily broadcast email.");
            return;
        }

        log.info("Sending daily broadcast email using template id={}, subject='{}'",
                latestTemplate.getId(), latestTemplate.getSubject());

        int recipients = adminCommunicationService.sendBroadcastEmail(
                latestTemplate.getSubject(),
                latestTemplate.getMessage()
        );

        log.info("Daily broadcast email sent to {} students.", recipients);
    }
}



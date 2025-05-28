package com.sholas.demo.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import java.util.List;
@Component
public class EmailPollScheduler {

    private final EmailReaderService emailReaderService;
    private final CamundaEmailProcessService camundaService;

    @Autowired
    public EmailPollScheduler(EmailReaderService emailReaderService,
                              CamundaEmailProcessService camundaService) {
        this.emailReaderService = emailReaderService;
        this.camundaService = camundaService;
    }

    @Scheduled(fixedRate = 120000)
    public void pollEmails() {
        System.out.println("[INFO] Starting scheduled email poll...");

        try {
            List<Message> messages = emailReaderService.fetchAndProcessUnreadEmails();
            System.out.println("[INFO] Found " + messages.size() + " unread emails.");

            for (Message msg : messages) {
                try {
                    camundaService.startEmailProcess(msg);
                    emailReaderService.markAsRead(msg);
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to start Camunda process or mark email as read: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Failed during email polling: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

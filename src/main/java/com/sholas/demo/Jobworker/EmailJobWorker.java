package com.sholas.demo.Jobworker;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.api.worker.JobWorker;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import jakarta.annotation.PostConstruct;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class EmailJobWorker {

    @Autowired
    private ZeebeClient zeebeClient;

    @PostConstruct
    public void startJobWorker() {
        zeebeClient.newWorker()
                .jobType("extract-email-metadata")
                .handler(new JobHandler() {
                    @Override
                    public void handle(JobClient client, ActivatedJob job) {
                        Map<String, Object> variables = job.getVariablesAsMap();

                        String from = (String) variables.get("from");
                        String subject = (String) variables.get("subject");
                        String bodyRaw = (String) variables.get("body");
                        String receivedDate = (String) variables.get("receivedDate");

                        // Convert raw body to plain text
                        String body = extractPlainText(bodyRaw);

                        if (body != null && !body.trim().isEmpty()) {
                            System.out.println("📨 Email Received:");
                            System.out.println("📅 Received: " + (receivedDate != null ? receivedDate : "unknown"));
                            System.out.println("👤 From   : " + (from != null ? from : "unknown"));
                            System.out.println("📧 Subject: " + (subject != null ? subject : "no subject"));
                            System.out.println("📝 Body   :\n" + body);
                            System.out.println("--------------------------------------");
                        } else {
                            System.out.println("⚠️ Skipped email from " + (from != null ? from : "unknown") + " — no plain text body.");
                            System.out.println("--------------------------------------");
                        }

                        client.newCompleteCommand(job.getKey())
                                .send()
                                .join();
                    }
                })
                .name("email-job-worker")
                .open();

        System.out.println("========== ALL EMAILS PROCESSED ==========");
    }

    // Helper method to clean HTML/mixed content
    private String extractPlainText(String htmlOrText) {
        if (htmlOrText == null) return "";
        return Jsoup.parse(htmlOrText).text(); // Strips all tags and returns clean text
    }
}
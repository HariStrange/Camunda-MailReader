package com.sholas.demo.Service;

import io.camunda.zeebe.client.ZeebeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@Service
public class CamundaEmailProcessService {

    private final ZeebeClient zeebeClient;

    @Value("${camunda.client.process-id:Process_1l26zol}")
    private String processId;

    public CamundaEmailProcessService(ZeebeClient zeebeClient) {
        this.zeebeClient = zeebeClient;
    }

    public void startEmailProcess(Message email) throws Exception {
        String subject = email.getSubject();
        String body = extractText(email);
        String from = extractSender(email);

        Map<String, Object> variables = new HashMap<>();
        variables.put("subject", subject != null ? subject : "No Subject");
        variables.put("body", body != null ? body : "No Body");
        variables.put("from", from != null ? from : "unknown");
        variables.put("receivedDate", formatDateToIST(email.getReceivedDate()));

        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId(processId)
                .latestVersion()
                .variables(variables)
                .send()
                .join();

        System.out.println("[INFO] Started Camunda process for email from: " + from + ", subject: " + subject);
    }

    private String formatDateToIST(Date date) {
        if (date == null) return "Unknown";
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        return sdf.format(date);
    }

    private String extractText(Message message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof MimeMultipart) {
            MimeMultipart multipart = (MimeMultipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                if (part.isMimeType("text/plain")) {
                    return part.getContent().toString();
                }
            }
        }
        return "";
    }

    private String extractSender(Message message) throws Exception {
        Address[] fromAddresses = message.getFrom();
        if (fromAddresses != null && fromAddresses.length > 0) {
            return fromAddresses[0].toString();
        }
        return null;
    }
}
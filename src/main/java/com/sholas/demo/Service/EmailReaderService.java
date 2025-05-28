package com.sholas.demo.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.search.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class EmailReaderService {

    @Value("${mail.host}")
    private String host;

    @Value("${mail.port}")
    private String port;

    @Value("${mail.username}")
    private String username;

    @Value("${mail.password}")
    private String password;

    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    public List<Message> fetchAndProcessUnreadEmails() {
        if (isProcessing.get()) {
            System.out.println("[INFO] Email fetch is already running. Skipping...");
            return Collections.emptyList();
        }

        isProcessing.set(true);
        Store store = null;
        Folder inbox = null;

        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", host);
            props.put("mail.imaps.port", port);
            props.put("mail.imaps.ssl.enable", "true");
            props.put("mail.imaps.ssl.trust", "*");
            props.put("mail.imaps.connectiontimeout", "20000");
            props.put("mail.imaps.timeout", "30000");

            Session session = Session.getInstance(props);
            store = session.getStore("imaps");

            if (!tryConnectWithRetry(store, 3)) {
                throw new MessagingException("Failed to connect after 3 attempts.");
            }

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            SearchTerm unreadTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.MONTH, Calendar.JANUARY);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            SearchTerm dateTerm = new ReceivedDateTerm(ComparisonTerm.GE, calendar.getTime());

            SearchTerm combined = new AndTerm(unreadTerm, dateTerm);

            Message[] messages = inbox.search(combined);
            return Arrays.asList(messages);


        } catch (Exception e) {
            System.err.println("[ERROR] Failed to fetch emails: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            isProcessing.set(false);
        }
    }

    public void markAsRead(Message message) throws MessagingException {
        message.setFlag(Flags.Flag.SEEN, true);
    }

    private boolean tryConnectWithRetry(Store store, int maxRetries) {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                store.connect(host, Integer.parseInt(port), username, password);
                return true;
            } catch (MessagingException e) {
                attempts++;
                System.err.println("[WARN] Connection attempt " + attempts + " failed: " + e.getMessage());
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {}
            }
        }
        return false;
    }
}

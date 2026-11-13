package com.vietblog.support;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * In-memory JavaMailSender for integration tests that captures rendered HTML emails
 * without requiring an external SMTP server.
 */
public class CapturingJavaMailSender extends JavaMailSenderImpl {

    private static final Pattern OTP_PATTERN = Pattern.compile("font-family:monospace;\">(\\d{6})</div>");

    public record CapturedEmail(List<String> to, String subject, String htmlBody) {}

    private final List<CapturedEmail> sentEmails = new CopyOnWriteArrayList<>();

    public CapturingJavaMailSender() {
        setHost("smtp.test.local");
        setPort(587);
        setUsername("test@vietblog.local");
    }

    @Override
    public void send(MimeMessage mimeMessage) throws MailException {
        try {
            Address[] recipients = mimeMessage.getAllRecipients();
            List<String> toList = recipients == null
                    ? List.of()
                    : Arrays.stream(recipients).map(Address::toString).toList();
            String subject = mimeMessage.getSubject();
            String body = extractBody(mimeMessage);
            sentEmails.add(new CapturedEmail(toList, subject != null ? subject : "", body));
        } catch (MessagingException | IOException ex) {
            throw new MailSendException("Failed to capture MimeMessage in test", ex);
        }
    }

    @Override
    public void send(MimeMessage... mimeMessages) throws MailException {
        for (MimeMessage message : mimeMessages) {
            send(message);
        }
    }

    public void clear() {
        sentEmails.clear();
    }

    public List<CapturedEmail> all() {
        return List.copyOf(sentEmails);
    }

    public List<CapturedEmail> forRecipient(String email) {
        String target = email.trim().toLowerCase();
        return sentEmails.stream()
                .filter(mail -> mail.to().stream().anyMatch(addr -> addr.toLowerCase().contains(target)))
                .toList();
    }

    public Optional<String> latestOtpFor(String email) {
        List<CapturedEmail> matches = forRecipient(email);
        for (int i = matches.size() - 1; i >= 0; i--) {
            Matcher matcher = OTP_PATTERN.matcher(matches.get(i).htmlBody());
            if (matcher.find()) {
                return Optional.of(matcher.group(1));
            }
        }
        return Optional.empty();
    }

    private static String extractBody(Part part) throws MessagingException, IOException {
        Object content = part.getContent();
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof Multipart multipart) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                sb.append(extractBody(bodyPart));
            }
            return sb.toString();
        }
        return content != null ? content.toString() : "";
    }

    @TestConfiguration
    public static class Config {
        @Bean
        @Primary
        public CapturingJavaMailSender capturingJavaMailSender() {
            return new CapturingJavaMailSender();
        }
    }
}


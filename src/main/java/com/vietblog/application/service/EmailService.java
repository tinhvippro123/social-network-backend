package com.vietblog.application.service;

import com.vietblog.domain.entity.User;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ErrorCode;
import jakarta.annotation.PreDestroy;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String mailUsername;
    private final String configuredFrom;
    private final String fromName;
    private final String frontendUrl;
    private final ExecutorService notificationExecutor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "vietblog-mail-notify");
        thread.setDaemon(true);
        return thread;
    });

    public EmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${app.mail.from:}") String configuredFrom,
            @Value("${app.mail.from-name:VietBlog}") String fromName,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailUsername = mailUsername != null ? mailUsername.trim() : "";
        this.configuredFrom = configuredFrom != null ? configuredFrom.trim() : "";
        this.fromName = StringUtils.hasText(fromName) ? fromName.trim() : "VietBlog";
        this.frontendUrl = StringUtils.hasText(frontendUrl) ? frontendUrl.trim().replaceAll("/+$", "") : "http://localhost:5173";
    }

    @PreDestroy
    public void shutdown() {
        notificationExecutor.shutdownNow();
    }

    public boolean isConfigured() {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            return false;
        }
        if (sender instanceof JavaMailSenderImpl impl) {
            return StringUtils.hasText(impl.getHost());
        }
        return true;
    }

    public void sendVerificationOtp(User user, String otp, int expiresMinutes) {
        String safeName = HtmlUtils.htmlEscape(user.getName());
        String safeOtp = HtmlUtils.htmlEscape(otp);
        String body = "<p>Xin chào <strong>" + safeName + "</strong>,</p>"
                + "<p>Cảm ơn bạn đã đăng ký tài khoản trên <strong>VietBlog</strong>. "
                + "Vui lòng sử dụng mã OTP dưới đây để xác thực địa chỉ email của bạn:</p>"
                + renderOtpBox(safeOtp, expiresMinutes)
                + "<p style=\"color:#6b7280;font-size:13px;\">Nếu bạn không thực hiện đăng ký tài khoản này, vui lòng bỏ qua email này.</p>";

        sendHtmlStrict(
                user.getEmail(),
                "[VietBlog] Mã OTP xác thực email tài khoản",
                buildLayout("Xác thực email tài khoản", body)
        );
    }

    public void sendPasswordResetOtp(User user, String otp, int expiresMinutes) {
        String safeName = HtmlUtils.htmlEscape(user.getName());
        String safeOtp = HtmlUtils.htmlEscape(otp);
        String body = "<p>Xin chào <strong>" + safeName + "</strong>,</p>"
                + "<p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản VietBlog của bạn. "
                + "Nhập mã OTP gồm 6 chữ số sau để tiếp tục:</p>"
                + renderOtpBox(safeOtp, expiresMinutes)
                + "<p style=\"color:#6b7280;font-size:13px;\">Không chia sẻ mã này cho bất kỳ ai. "
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.</p>";

        sendHtmlStrict(
                user.getEmail(),
                "[VietBlog] Mã OTP đặt lại mật khẩu",
                buildLayout("Đặt lại mật khẩu", body)
        );
    }

    /**
     * Gửi email thông báo hoạt động ở chế độ nền (best-effort), không làm gián đoạn thao tác chính.
     */
    public void sendNotificationEmailAsync(
            String toEmail,
            String recipientName,
            String subject,
            String heading,
            String messageText,
            String actionPath) {
        if (!StringUtils.hasText(toEmail) || !isConfigured()) {
            return;
        }
        notificationExecutor.submit(() -> {
            try {
                String safeName = HtmlUtils.htmlEscape(StringUtils.hasText(recipientName) ? recipientName : "bạn");
                String safeMessage = HtmlUtils.htmlEscape(messageText);
                String safeLink = StringUtils.hasText(actionPath)
                        ? frontendUrl + (actionPath.startsWith("/") ? actionPath : "/" + actionPath)
                        : frontendUrl;
                String body = "<p>Xin chào <strong>" + safeName + "</strong>,</p>"
                        + "<p>" + safeMessage + "</p>"
                        + "<p style=\"margin:24px 0;\">"
                        + "<a href=\"" + HtmlUtils.htmlEscape(safeLink) + "\" "
                        + "style=\"display:inline-block;padding:10px 20px;background:#2563eb;color:#ffffff;"
                        + "text-decoration:none;border-radius:8px;font-weight:600;\">Xem trên VietBlog</a>"
                        + "</p>"
                        + "<p style=\"color:#6b7280;font-size:12px;\">Bạn có thể thay đổi tùy chọn nhận email trong mục Cài đặt &rarr; Thông báo.</p>";
                sendHtmlInternal(toEmail, subject, buildLayout(heading, body));
            } catch (Exception ex) {
                log.warn("Không thể gửi email thông báo tới {}: {}", toEmail, ex.getMessage());
            }
        });
    }

    private void sendHtmlStrict(String toEmail, String subject, String htmlContent) {
        if (!isConfigured()) {
            throw new BusinessRuleException(ErrorCode.EMAIL_NOT_CONFIGURED);
        }
        try {
            sendHtmlInternal(toEmail, subject, htmlContent);
        } catch (MessagingException | MailException | UnsupportedEncodingException ex) {
            log.error("Gửi email tới {} thất bại: {}", toEmail, ex.getMessage(), ex);
            throw new BusinessRuleException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private void sendHtmlInternal(String toEmail, String subject, String htmlContent)
            throws MessagingException, UnsupportedEncodingException {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            throw new BusinessRuleException(ErrorCode.EMAIL_NOT_CONFIGURED);
        }
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        String fromAddress = resolveFromAddress();
        if (StringUtils.hasText(fromAddress)) {
            helper.setFrom(fromAddress, fromName);
        }
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        sender.send(message);
    }

    private String resolveFromAddress() {
        if (StringUtils.hasText(configuredFrom)) {
            return configuredFrom;
        }
        if (StringUtils.hasText(mailUsername)) {
            return mailUsername;
        }
        return "no-reply@vietblog.local";
    }

    private static String renderOtpBox(String safeOtp, int expiresMinutes) {
        return "<div style=\"margin:20px 0;padding:16px 24px;background:#eff6ff;border:1px solid #bfdbfe;"
                + "border-radius:10px;text-align:center;\">"
                + "<div style=\"font-size:30px;font-weight:700;letter-spacing:8px;color:#1d4ed8;"
                + "font-family:monospace;\">" + safeOtp + "</div>"
                + "<div style=\"margin-top:8px;font-size:12px;color:#4b5563;\">Mã có hiệu lực trong "
                + expiresMinutes + " phút</div>"
                + "</div>";
    }

    private static String buildLayout(String heading, String innerHtml) {
        return "<!DOCTYPE html><html><body style=\"margin:0;padding:24px;background:#f3f4f6;"
                + "font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#1f2937;\">"
                + "<div style=\"max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;"
                + "border:1px solid #e5e7eb;overflow:hidden;\">"
                + "<div style=\"padding:20px 24px;background:#1d4ed8;color:#ffffff;\">"
                + "<h2 style=\"margin:0;font-size:18px;\">VietBlog — " + HtmlUtils.htmlEscape(heading) + "</h2>"
                + "</div>"
                + "<div style=\"padding:24px;font-size:15px;line-height:1.6;\">" + innerHtml + "</div>"
                + "<div style=\"padding:14px 24px;background:#f9fafb;border-top:1px solid #e5e7eb;"
                + "font-size:12px;color:#9ca3af;text-align:center;\">&copy; VietBlog</div>"
                + "</div></body></html>";
    }
}


package net.engineerAnsh.BankApplication.Email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Event.UserLoginEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService emailTemplateService;

    @Override
    public void sendSimpleEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("Simple email sent to {}", to);
    }

    @Override
    public void sendEmailWithAttachment(String to,
                                        String subject,
                                        String body,
                                        byte[] attachement,
                                        String fileName
    ) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setTo(to);
        helper.setSubject(subject);

        // body as plain text
        helper.setText(body, false);

        // attach pdf
        helper.addAttachment(fileName, new ByteArrayResource(attachement));

        mailSender.send(mimeMessage);

        log.info("Email with attachment sent to {}", to);
    }

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendVerificationEmail(String toEmail, String token) {
        String verificationLink = baseUrl + "/auth/verify?token=" + token;
        String subject = "Verify Your Email - Bank of Ansh";
        String body = emailTemplateService.buildVerificationEmail(verificationLink);
        sendHtmlEmail(toEmail, subject, body);
    }

    public void sendLoginAlertEmail(UserLoginEvent loginEvent) {
        String subject = "New Login Detected";
        String body = emailTemplateService.buildLoginAlertEmail(loginEvent);
        sendHtmlEmail(loginEvent.getEmail(), subject, body);
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML

            mailSender.send(message);
            log.info("HTML email is sent to {}, reason: {}", to, subject);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to send HTML email", ex);
        }
    }
}

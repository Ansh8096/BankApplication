package net.engineerAnsh.BankApplication.Email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceimpl implements EmailService{

    private final JavaMailSender mailSender;

    @Override
    public void sendSimpleEmail(String to, String subject, String body) {
            SimpleMailMessage message = new SimpleMailMessage();
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
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
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage,true);
        helper.setTo(to);
        helper.setSubject(subject);

        // body as plain text
        helper.setText(body,false);

        // attach pdf
        helper.addAttachment(fileName,new ByteArrayResource(attachement));

        mailSender.send(mimeMessage);

        log.info("Email with attachment sent to {}", to);
    }
}

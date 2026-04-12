package net.engineerAnsh.BankApplication.services.notification.email;


import jakarta.mail.MessagingException;

public interface EmailService {

    void sendSimpleEmail(String to,String subject, String body);

    void sendEmailWithAttachment(
            String to,
            String subject,
            String body,
            byte[] attachement,
            String fileName
    ) throws MessagingException;
}

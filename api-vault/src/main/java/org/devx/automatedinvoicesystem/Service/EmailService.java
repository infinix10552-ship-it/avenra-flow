package org.devx.automatedinvoicesystem.Service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Reset Request - Automated Invoice System");
        message.setText("Hello,\n\n" +
                "You have requested to reset your password. Please click the link below to set a new password:\n\n" +
                resetLink + "\n\n" +
                "This link will expire in 15 minutes.\n\n" +
                "If you did not request this, please ignore this email.");

        mailSender.send(message);
        System.out.println("✅ [EMAIL] Password reset link sent to: " + toEmail);
    }
}

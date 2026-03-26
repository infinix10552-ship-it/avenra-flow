package org.devx.automatedinvoicesystem.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String brevoUrl = "https://api.brevo.com/v3/smtp/email";

    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> sender = new HashMap<>();
        sender.put("name", "Avenra FLOW Security");
        sender.put("email", "no-reply@avenra.com"); // Adjust to your verified Brevo sender email

        Map<String, String> to = new HashMap<>();
        to.put("email", toEmail);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", sender);
        payload.put("to", List.of(to));
        payload.put("subject", "Avenra FLOW - Password Reset Request");
        payload.put("htmlContent", "<html><body style='font-family: Arial, sans-serif; color: #333;'>"
                + "<h2>Password Reset Request</h2>"
                + "<p>To reset your secure vault password, please click the button below:</p>"
                + "<a href='" + resetUrl + "' style='display: inline-block; padding: 10px 20px; background-color: #3b82f6; color: #fff; text-decoration: none; border-radius: 5px;'>Reset Password</a>"
                + "<p>If you did not request this, please ignore this email.</p>"
                + "</body></html>");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForEntity(brevoUrl, request, String.class);
            System.out.println("✅ [EMAIL] Password reset sent to " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ [EMAIL ERROR] Failed to send password reset: " + e.getMessage());
        }
    }

    // --- NEW: Dedicated Team Invitation Template ---
    public void sendTeamInvitationEmail(String toEmail, String inviteUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> sender = new HashMap<>();
        sender.put("name", "Avenra FLOW Team");
        sender.put("email", "no-reply@avenra.com"); // Adjust to your verified Brevo sender email

        Map<String, String> to = new HashMap<>();
        to.put("email", toEmail);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", sender);
        payload.put("to", List.of(to));
        payload.put("subject", "You've been invited to an Avenra FLOW Workspace");
        payload.put("htmlContent", "<html><body style='font-family: Arial, sans-serif; color: #333;'>"
                + "<h2>Workspace Invitation</h2>"
                + "<p>You have been invited to join a secure financial workspace on Avenra FLOW.</p>"
                + "<p>Click the button below to set up your account and access the vault. You will be automatically routed to your team's tenant upon registration.</p>"
                + "<br/>"
                + "<a href='" + inviteUrl + "' style='display: inline-block; padding: 12px 24px; background-color: #10b981; color: #fff; text-decoration: none; font-weight: bold; border-radius: 6px;'>Accept Invitation</a>"
                + "<br/><br/>"
                + "<p style='font-size: 12px; color: #888;'>Welcome to the next generation of financial operations.</p>"
                + "</body></html>");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForEntity(brevoUrl, request, String.class);
            System.out.println("✅ [EMAIL] Workspace invitation sent to " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ [EMAIL ERROR] Failed to send workspace invitation: " + e.getMessage());
        }
    }
}
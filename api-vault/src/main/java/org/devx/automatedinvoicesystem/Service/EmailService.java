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

    // Pulls the new API key from Render Environment Variables
    @Value("${brevo.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        // We use the HTTP endpoint (Port 443) which easily slices through Render's firewall
        String url = "https://api.brevo.com/v3/smtp/email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        // Build the payload
        Map<String, Object> sender = new HashMap<>();
        sender.put("name", "Avenra Security");
        sender.put("email", "security@avenra.com");

        Map<String, Object> recipient = new HashMap<>();
        recipient.put("email", toEmail);

        Map<String, Object> body = new HashMap<>();
        body.put("sender", sender);
        body.put("to", List.of(recipient));
        body.put("subject", "Reset Your Avenra FLOW Password");
        body.put("htmlContent", "<div style='font-family: sans-serif; max-width: 600px; margin: auto;'>" +
                "<h2>Password Reset Request</h2>" +
                "<p>You recently requested to reset your password for your Avenra FLOW workspace.</p>" +
                "<a href='" + resetLink + "' style='display: inline-block; padding: 10px 20px; color: #fff; background-color: #0f172a; text-decoration: none; border-radius: 5px; margin-top: 15px;'>Reset Password</a>" +
                "<p style='margin-top: 30px; font-size: 12px; color: #64748b;'>If you did not request this, please ignore this email. This link will expire in 24 hours.</p>" +
                "</div>");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(url, request, String.class);
            System.out.println("[✅] Password reset email sent successfully via HTTP API!");
        } catch (Exception e) {
            System.err.println("\n[❌] HTTP EMAIL CRASH:");
            e.printStackTrace();
        }
    }
}
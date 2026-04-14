package org.devx.automatedinvoicesystem.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.sender.email:noreply@avenraflow.com}")
    private String senderEmail;

    @Value("${brevo.sender.name:Avenra FLOW}")
    private String senderName;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String brevoUrl = "https://api.brevo.com/v3/smtp/email";

    @PostConstruct
    public void init() {
        if ("none".equals(apiKey)) {
            System.err.println("\n⚠️ [EMAIL SERVICE] Brevo API Key not configured! Emails will ONLY be printed to the console (Local Dev Mode).\n");
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        System.out.println("📧 [EMAIL] Dispatching password reset email to: " + toEmail);

        Map<String, Object> sender = new HashMap<>();
        sender.put("name", senderName + " Security");
        sender.put("email", senderEmail);

        Map<String, String> to = new HashMap<>();
        to.put("email", toEmail);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", sender);
        payload.put("to", List.of(to));
        payload.put("subject", "Avenra FLOW - Password Reset Request");
        payload.put("htmlContent", "<html><body style='font-family: Arial, sans-serif; color: #333;'>"
                + "<div style='max-width: 600px; margin: 0 auto; padding: 32px; border: 1px solid #e2e8f0; border-radius: 12px;'>"
                + "<h2 style='color: #1e293b;'>Password Reset Request</h2>"
                + "<p>To reset your secure vault password, please click the button below:</p>"
                + "<a href='" + resetUrl + "' style='display: inline-block; padding: 12px 24px; background-color: #3b82f6; color: #fff; text-decoration: none; border-radius: 8px; font-weight: bold; margin: 16px 0;'>Reset Password</a>"
                + "<p style='color: #64748b; font-size: 13px;'>If you did not request this, please ignore this email. This link expires in 1 hour.</p>"
                + "<hr style='border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;'/>"
                + "<p style='font-size: 11px; color: #94a3b8;'>Avenra FLOW — Intelligent Financial Operations</p>"
                + "</div>"
                + "</body></html>");

        sendEmail(payload, "password reset", toEmail);
    }

    @Async
    public void sendTeamInvitationEmail(String toEmail, String inviteUrl) {
        System.out.println("📧 [EMAIL] Dispatching team invitation email to: " + toEmail);

        Map<String, Object> sender = new HashMap<>();
        sender.put("name", senderName + " Team");
        sender.put("email", senderEmail);

        Map<String, String> to = new HashMap<>();
        to.put("email", toEmail);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", sender);
        payload.put("to", List.of(to));
        payload.put("subject", "You've been invited to an Avenra FLOW Workspace");
        payload.put("htmlContent", "<html><body style='font-family: Arial, sans-serif; color: #333;'>"
                + "<div style='max-width: 600px; margin: 0 auto; padding: 32px; border: 1px solid #e2e8f0; border-radius: 12px;'>"
                + "<h2 style='color: #1e293b;'>Workspace Invitation</h2>"
                + "<p>You have been invited to join a secure financial workspace on Avenra FLOW.</p>"
                + "<p>Click the button below to set up your account and access the vault. You will be automatically routed to your team's tenant upon registration.</p>"
                + "<br/>"
                + "<a href='" + inviteUrl + "' style='display: inline-block; padding: 12px 24px; background-color: #10b981; color: #fff; text-decoration: none; font-weight: bold; border-radius: 8px;'>Accept Invitation</a>"
                + "<br/><br/>"
                + "<hr style='border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;'/>"
                + "<p style='font-size: 11px; color: #94a3b8;'>Welcome to the next generation of financial operations.</p>"
                + "</div>"
                + "</body></html>");

        sendEmail(payload, "workspace invitation", toEmail);
    }

    /**
     * Centralized email dispatch with FULL error transparency.
     * If Brevo rejects the request (invalid API key, unverified sender, etc.),
     * the EXACT HTTP response body is logged instead of being silently swallowed.
     */
    private void sendEmail(Map<String, Object> payload, String emailType, String toEmail) {
        if ("none".equals(apiKey)) {
            System.out.println("\n====== [LOCAL DEV MODE: MOCKED EMAIL] ======");
            System.out.println("Email Type: " + emailType.toUpperCase());
            System.out.println("To:         " + toEmail);
            System.out.println("Subject:    " + payload.get("subject"));
            System.out.println("Content:    " + payload.get("htmlContent"));
            System.out.println("==========================================\n");
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(brevoUrl, request, String.class);
            System.out.println("✅ [EMAIL] " + emailType + " sent to " + toEmail
                    + " | Status: " + response.getStatusCode());
        } catch (HttpClientErrorException e) {
            System.err.println("❌ [EMAIL ERROR] " + emailType + " to " + toEmail + " REJECTED by Brevo.");
            System.err.println("   HTTP Status: " + e.getStatusCode());
            System.err.println("   Response Body: " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            System.err.println("❌ [EMAIL ERROR] Brevo server error for " + emailType + " to " + toEmail);
            System.err.println("   HTTP Status: " + e.getStatusCode());
            System.err.println("   Response Body: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("❌ [EMAIL ERROR] Failed to send " + emailType + " to " + toEmail);
            System.err.println("   Exception: " + e.getClass().getSimpleName() + " — " + e.getMessage());
        }
    }
}
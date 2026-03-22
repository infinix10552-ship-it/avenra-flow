package org.devx.automatedinvoicesystem.Config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwilioConfig {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @PostConstruct
    public void initTwilio() {
        // This physically connects Spring Boot server to Twilio's cloud on startup
        Twilio.init(accountSid, authToken);
        System.out.println("✅ [TWILIO] WhatsApp Engine Initialized.");
    }
}
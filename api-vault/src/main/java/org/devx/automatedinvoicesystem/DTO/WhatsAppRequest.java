package org.devx.automatedinvoicesystem.DTO;

import lombok.Data;
import java.util.UUID;

@Data
public class WhatsAppRequest {
    private UUID invoiceId;
    private String targetPhoneNumber; // +xx1234567890
}

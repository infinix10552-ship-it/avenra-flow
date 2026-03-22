package org.devx.automatedinvoicesystem.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPayload {

    private UUID invoiceId;
    private UUID organizationId;
    private String vendorName;
    private Double totalAmount;
    private LocalDate invoiceDate;
    private String category;
}

package org.devx.automatedinvoicesystem.DTO;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class InvoiceSearchFilter {

    // Mandatory security anchor
    private UUID organizationId;

    // Optional client scope
    private UUID clientId;

    // Optional user filters
    private String supplierName;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
}

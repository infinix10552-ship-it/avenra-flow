package org.devx.automatedinvoicesystem.DTO;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class InvoiceSearchFilter {

    // The mandatory security anchor
    private UUID organizationId;

    // The optional user filters
    private String vendorName;
    private String category;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;

}

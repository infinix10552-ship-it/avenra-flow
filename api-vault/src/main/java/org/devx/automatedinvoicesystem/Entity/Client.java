package org.devx.automatedinvoicesystem.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clients", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"organization_id", "client_gstin", "financial_year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Client extends Base {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @NotBlank(message = "Client name is required")
    @Column(name = "client_name", nullable = false, length = 200)
    private String clientName;

    @NotBlank(message = "Client GSTIN is required")
    @Size(min = 15, max = 15, message = "GSTIN must be exactly 15 characters")
    @Pattern(
            regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
            message = "Invalid GSTIN format"
    )
    @Column(name = "client_gstin", nullable = false, length = 15)
    private String clientGstin;

    @NotBlank(message = "Financial year is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Financial year must be in format YYYY-YY (e.g., 2025-26)")
    @Column(name = "financial_year", nullable = false, length = 7)
    private String financialYear;

    // ── CHART OF ACCOUNTS (PRD §2.2 & §6.2) ──────────────────────────
    // Persisted per client. AI must select EXACT match from this list.
    // If no match → ledgerAccountName = null → REQUIRES_MANUAL_REVIEW.

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ClientLedger> clientLedgers = new ArrayList<>();
}

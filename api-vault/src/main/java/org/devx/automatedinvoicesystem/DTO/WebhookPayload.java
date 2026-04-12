package org.devx.automatedinvoicesystem.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Webhook payload from the AI worker.
 * Maps EXACTLY to the AI output contract defined in the PRD.
 * Any deviation from this schema = HARD FAIL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPayload {

    // ── ROUTING ───────────────────────────────────────────────────────
    private UUID invoiceId;
    private UUID organizationId;
    private UUID clientId;

    // ── AI EXTRACTED FIELDS (STRICT SCHEMA) ───────────────────────────
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String supplierName;
    private String supplierGstin;
    private String buyerGstin;
    private String hsnSac;

    // ── FINANCIAL DATA (BigDecimal — NO FLOATING POINT) ───────────────
    private BigDecimal baseAmount;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal igst;
    private BigDecimal totalAmount;

    // ── LEDGER MAPPING (PRD §2.1 — must be exact match from clientLedgers or null)
    private String ledgerAccountName;

    // ── AI METADATA ───────────────────────────────────────────────────
    private Double confidenceScore;
}

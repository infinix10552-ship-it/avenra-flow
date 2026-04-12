package org.devx.automatedinvoicesystem.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoices",
        indexes = {
                @Index(name = "idx_invoice_client", columnList = "client_id"),
                @Index(name = "idx_invoice_org", columnList = "organization_id"),
                @Index(name = "idx_invoice_status", columnList = "status"),
                @Index(name = "idx_invoice_duplicate_check",
                        columnList = "supplier_gstin, invoice_number, invoice_date, client_id")
        },
        uniqueConstraints = {
                // PRD §3.3: Duplicate Detection — unique invoice identity per client
                @UniqueConstraint(
                        name = "uk_invoice_identity",
                        columnNames = {"supplier_gstin", "invoice_number", "invoice_date", "client_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Invoice extends Base {

    // ── TENANT HIERARCHY ──────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    // ── FILE METADATA ─────────────────────────────────────────────────

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "s3_file_url", nullable = false, length = 500)
    private String s3FileUrl;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    // ── INVOICE IDENTITY ──────────────────────────────────────────────

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    // ── SUPPLIER / BUYER INFO ─────────────────────────────────────────

    @Column(name = "supplier_name", length = 200)
    private String supplierName;

    @Column(name = "supplier_gstin", length = 15)
    private String supplierGstin;

    @Column(name = "buyer_gstin", length = 15)
    private String buyerGstin;

    // ── CLASSIFICATION ────────────────────────────────────────────────

    @Column(name = "hsn_sac_code", length = 20)
    private String hsnSacCode;

    @Column(name = "ledger_account_name", length = 200)
    private String ledgerAccountName;

    // ── FINANCIAL DATA (BigDecimal — NO FLOATING POINT) ───────────────

    @Column(name = "base_taxable_amount", precision = 15, scale = 2)
    private BigDecimal baseTaxableAmount;

    @Column(name = "cgst", precision = 15, scale = 2)
    private BigDecimal cgst;

    @Column(name = "sgst", precision = 15, scale = 2)
    private BigDecimal sgst;

    @Column(name = "igst", precision = 15, scale = 2)
    private BigDecimal igst;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "original_currency", length = 3, nullable = false)
    private String originalCurrency = "INR";

    @Column(name = "exchange_rate", precision = 15, scale = 6)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "converted_amount_inr", precision = 15, scale = 2)
    private BigDecimal convertedAmountInr;

    // ── AI METADATA ───────────────────────────────────────────────────

    @Column(name = "ai_confidence_score")
    private Double aiConfidenceScore;

    // ── PROCESSING STATUS ─────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProcessingStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    // PRD §3.3: Store reason for FAILED status (e.g., DUPLICATE_INVOICE)
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    // ── AUDIT FIELDS (PRD §5.1 — Maker-Checker System) ───────────────

    // UUID of the user who last manually modified this invoice
    @Column(name = "modified_by")
    private UUID modifiedBy;

    // Timestamp of the last manual modification (NOT auto-managed by Hibernate)
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    // ── STATE MACHINE (PRD §1.2) ──────────────────────────────────────
    //
    // PENDING → PROCESSING                  (RabbitMQ pickup)
    // PROCESSING → COMPLETED                (all validations pass)
    // PROCESSING → REQUIRES_MANUAL_REVIEW   (validation failure)
    // PROCESSING → FAILED                   (crash or duplicate)
    // REQUIRES_MANUAL_REVIEW → COMPLETED    (after manual approval)
    //
    // HARD RULE: Only COMPLETED invoices are exportable.

    public enum ProcessingStatus {
        PENDING,                 // Uploaded, waiting for RabbitMQ pickup
        PROCESSING,              // AI worker is extracting data
        COMPLETED,               // Extraction + validation passed
        REQUIRES_MANUAL_REVIEW,  // Validation failed (math mismatch, low confidence, missing ledger, invalid GSTIN)
        FAILED                   // Processing crashed (corrupt file, AI error, duplicate invoice)
    }
}

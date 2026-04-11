package org.devx.automatedinvoicesystem.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoices", indexes = {
        @Index(name = "idx_invoice_client", columnList = "client_id"),
        @Index(name = "idx_invoice_org", columnList = "organization_id"),
        @Index(name = "idx_invoice_status", columnList = "status")
})
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

    // ── AI METADATA ───────────────────────────────────────────────────

    @Column(name = "ai_confidence_score")
    private Double aiConfidenceScore;

    // ── PROCESSING STATUS ─────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProcessingStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    public enum ProcessingStatus {
        PENDING,                 // Uploaded, waiting for RabbitMQ pickup
        PROCESSING,              // AI worker is extracting data
        COMPLETED,               // Extraction + validation passed
        REQUIRES_MANUAL_REVIEW,  // Validation failed (math mismatch, low confidence)
        FAILED                   // Processing crashed (corrupt file, AI error)
    }
}

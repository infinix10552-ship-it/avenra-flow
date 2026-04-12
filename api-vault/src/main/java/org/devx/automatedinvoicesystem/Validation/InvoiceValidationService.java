package org.devx.automatedinvoicesystem.Validation;

import org.devx.automatedinvoicesystem.DTO.WebhookPayload;
import org.devx.automatedinvoicesystem.Repository.ClientLedgerRepo;
import org.devx.automatedinvoicesystem.Repository.InvoiceRepo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Centralized validation pipeline for invoice data.
 * PRD §3: Every invoice MUST pass through this service before status assignment.
 *
 * <p>Validation order (deterministic):
 * <ol>
 *   <li>GSTIN format + state code validation (supplier + buyer)</li>
 *   <li>Tax math: base + cgst + sgst + igst == total</li>
 *   <li>AI confidence threshold (>= 85)</li>
 *   <li>Ledger mapping enforcement (must exist in client's Chart of Accounts)</li>
 *   <li>Duplicate detection (supplierGstin + invoiceNumber + invoiceDate + clientId)</li>
 * </ol>
 *
 * <p><b>Hard Rule:</b> When in doubt → block, not guess.
 * Invalid data is NEVER auto-corrected.</p>
 */
@Service
public class InvoiceValidationService {

    private final ClientLedgerRepo clientLedgerRepo;
    private final InvoiceRepo invoiceRepo;

    public InvoiceValidationService(ClientLedgerRepo clientLedgerRepo, InvoiceRepo invoiceRepo) {
        this.clientLedgerRepo = clientLedgerRepo;
        this.invoiceRepo = invoiceRepo;
    }

    /**
     * Result of the validation pipeline.
     * Contains the determined status and a list of all validation failures.
     */
    public record ValidationResult(
            Status status,
            List<String> failures,
            String failureReason
    ) {
        public boolean isValid() {
            return status == Status.COMPLETED;
        }

        public enum Status {
            COMPLETED,
            REQUIRES_MANUAL_REVIEW,
            FAILED
        }
    }

    /**
     * Executes the full validation pipeline against the AI-extracted payload.
     * PRD §3.1: Executed BEFORE saving invoice data.
     *
     * @param payload   The AI-extracted data
     * @param clientId  The client this invoice belongs to (nullable for legacy uploads)
     * @return ValidationResult with status and failure details
     */
    public ValidationResult validate(WebhookPayload payload, UUID clientId) {
        List<String> failures = new ArrayList<>();
        String failureReason = null;

        // ── 1. GSTIN VALIDATION (PRD §3.2) ────────────────────────────────
        validateGstin(payload.getSupplierGstin(), "Supplier", failures);
        validateGstin(payload.getBuyerGstin(), "Buyer", failures);

        // ── 2. TAX MATH VALIDATION (PRD §3.1) ─────────────────────────────
        boolean taxMathValid = TaxCalculationValidator.isValid(
                payload.getBaseAmount(),
                payload.getCgst(),
                payload.getSgst(),
                payload.getIgst(),
                payload.getTotalAmount()
        );

        if (!taxMathValid) {
            BigDecimal mismatch = TaxCalculationValidator.getMismatchAmount(
                    payload.getBaseAmount(), payload.getCgst(),
                    payload.getSgst(), payload.getIgst(), payload.getTotalAmount()
            );
            failures.add("Tax math mismatch: base + cgst + sgst + igst != total. Diff: " + mismatch);
        }

        // ── 3. CONFIDENCE THRESHOLD (PRD §3.1) ────────────────────────────
        if (payload.getConfidenceScore() == null || payload.getConfidenceScore() < 85.0) {
            failures.add("AI confidence below threshold: "
                    + (payload.getConfidenceScore() != null ? payload.getConfidenceScore() : "null")
                    + "% (min: 85%)");
        }

        // ── 4. LEDGER MAPPING ENFORCEMENT (PRD §2.2) ──────────────────────
        if (clientId != null) {
            validateLedgerMapping(payload.getLedgerAccountName(), clientId, failures);
        } else if (payload.getLedgerAccountName() == null) {
            failures.add("Ledger account name is null (no client ledgers to validate against)");
        }

        // ── 5. DUPLICATE DETECTION (PRD §3.3) ─────────────────────────────
        if (clientId != null && payload.getSupplierGstin() != null
                && payload.getInvoiceNumber() != null && payload.getInvoiceDate() != null) {

            boolean isDuplicate = invoiceRepo.existsBySupplierGstinAndInvoiceNumberAndInvoiceDateAndClientId(
                    payload.getSupplierGstin(),
                    payload.getInvoiceNumber(),
                    payload.getInvoiceDate(),
                    clientId
            );

            if (isDuplicate) {
                failureReason = "DUPLICATE_INVOICE";
                return new ValidationResult(
                        ValidationResult.Status.FAILED,
                        List.of("Duplicate invoice detected: supplierGstin="
                                + payload.getSupplierGstin() + ", invoiceNumber="
                                + payload.getInvoiceNumber() + ", invoiceDate="
                                + payload.getInvoiceDate()),
                        failureReason
                );
            }
        }

        // ── DETERMINE STATUS ──────────────────────────────────────────────
        if (!failures.isEmpty()) {
            return new ValidationResult(
                    ValidationResult.Status.REQUIRES_MANUAL_REVIEW,
                    failures,
                    null
            );
        }

        return new ValidationResult(
                ValidationResult.Status.COMPLETED,
                List.of(),
                null
        );
    }

    // ── PRIVATE VALIDATORS ────────────────────────────────────────────

    private void validateGstin(String gstin, String label, List<String> failures) {
        if (gstin == null || gstin.isBlank()) {
            return; // null GSTIN is not a format error — it's a missing field (handled by confidence)
        }
        if (!GstinValidator.isValid(gstin)) {
            failures.add("Invalid " + label + " GSTIN format: " + gstin);
        } else if (!GstinValidator.hasValidStateCode(gstin)) {
            failures.add("Invalid state code in " + label + " GSTIN: " + gstin.substring(0, 2));
        }
    }

    private void validateLedgerMapping(String ledgerAccountName, UUID clientId, List<String> failures) {
        if (ledgerAccountName == null || ledgerAccountName.isBlank()) {
            failures.add("Ledger account name is null — AI could not map to Chart of Accounts");
            return;
        }

        // PRD §2.2: Must be a match from clientLedgers (now case-insensitive)
        boolean ledgerExists = clientLedgerRepo.existsByClientIdAndLedgerNameIgnoreCase(clientId, ledgerAccountName);
        if (!ledgerExists) {
            failures.add("Ledger '" + ledgerAccountName
                    + "' not found in client's Chart of Accounts. "
                    + "Available options: " + clientLedgerRepo.findLedgerNamesByClientId(clientId));
        }
    }
}

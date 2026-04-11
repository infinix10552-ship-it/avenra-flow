package org.devx.automatedinvoicesystem.Service;

import org.devx.automatedinvoicesystem.Entity.Client;
import org.devx.automatedinvoicesystem.Entity.Invoice;
import org.devx.automatedinvoicesystem.Repository.ClientRepo;
import org.devx.automatedinvoicesystem.Repository.InvoiceRepo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Generates GST-compliant CSV for ClearTax / Zoho Books import.
 * BLOCKS export if ANY invoice requires manual review.
 * Exact header compliance — no deviations.
 */
@Service
public class CsvExportService {

    private final InvoiceRepo invoiceRepo;
    private final ClientRepo clientRepo;

    // ClearTax / Zoho compatible header
    private static final String CSV_HEADER =
            "GSTIN of Supplier,Invoice Number,Invoice Date,Invoice Value," +
            "Place of Supply,Taxable Value,CGST Amount,SGST Amount,IGST Amount," +
            "HSN/SAC,Buyer GSTIN";

    public CsvExportService(InvoiceRepo invoiceRepo, ClientRepo clientRepo) {
        this.invoiceRepo = invoiceRepo;
        this.clientRepo = clientRepo;
    }

    /**
     * Generates a ClearTax/Zoho-compatible GST CSV for all COMPLETED invoices.
     *
     * @throws IllegalStateException if any invoice requires manual review
     */
    public String generateGstCsv(UUID clientId) {
        Client client = clientRepo.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        // BLOCK EXPORT if review pending
        boolean hasUnreviewedInvoices = invoiceRepo.existsByClientIdAndStatus(
                clientId, Invoice.ProcessingStatus.REQUIRES_MANUAL_REVIEW);

        if (hasUnreviewedInvoices) {
            long count = invoiceRepo.countByClientIdAndStatus(
                    clientId, Invoice.ProcessingStatus.REQUIRES_MANUAL_REVIEW);
            throw new IllegalStateException(
                    "Export blocked: " + count + " invoice(s) require manual review before export.");
        }

        List<Invoice> invoices = invoiceRepo.findByClientIdAndStatus(
                clientId, Invoice.ProcessingStatus.COMPLETED);

        if (invoices.isEmpty()) {
            throw new IllegalArgumentException("No completed invoices found for client: " + client.getClientName());
        }

        return buildCsv(invoices);
    }

    private String buildCsv(List<Invoice> invoices) {
        StringBuilder csv = new StringBuilder();
        csv.append(CSV_HEADER).append("\n");

        for (Invoice inv : invoices) {
            csv.append(escapeCsv(inv.getSupplierGstin())).append(",");
            csv.append(escapeCsv(inv.getInvoiceNumber())).append(",");
            csv.append(inv.getInvoiceDate() != null ? inv.getInvoiceDate().toString() : "").append(",");
            csv.append(formatAmount(inv.getTotalAmount())).append(",");
            csv.append(deriveStateFromGstin(inv.getSupplierGstin())).append(",");
            csv.append(formatAmount(inv.getBaseTaxableAmount())).append(",");
            csv.append(formatAmount(inv.getCgst())).append(",");
            csv.append(formatAmount(inv.getSgst())).append(",");
            csv.append(formatAmount(inv.getIgst())).append(",");
            csv.append(escapeCsv(inv.getHsnSacCode())).append(",");
            csv.append(escapeCsv(inv.getBuyerGstin()));
            csv.append("\n");
        }

        return csv.toString();
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.toPlainString();
    }

    /**
     * Derives the state code from the first 2 digits of GSTIN.
     */
    private String deriveStateFromGstin(String gstin) {
        if (gstin == null || gstin.length() < 2) return "";
        return gstin.substring(0, 2);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

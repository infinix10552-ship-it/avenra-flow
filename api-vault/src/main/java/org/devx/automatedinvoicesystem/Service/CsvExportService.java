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
 * PRD §4.1: Partial export — only COMPLETED invoices are exported.
 * PRD §4.2: Returns exportedCount and skippedCount.
 */
@Service
public class CsvExportService {

    private final InvoiceRepo invoiceRepo;
    private final ClientRepo clientRepo;

    private static final String CSV_HEADER =
            "GSTIN of Supplier,Invoice Number,Invoice Date,Invoice Value," +
            "Place of Supply,Taxable Value,CGST Amount,SGST Amount,IGST Amount," +
            "HSN/SAC,Buyer GSTIN,Ledger Account";

    public CsvExportService(InvoiceRepo invoiceRepo, ClientRepo clientRepo) {
        this.invoiceRepo = invoiceRepo;
        this.clientRepo = clientRepo;
    }

    /**
     * PRD §4.2: Export result with counts.
     */
    public record ExportResult(String content, int exportedCount, int skippedCount) {}

    /**
     * Generates a ClearTax/Zoho-compatible GST CSV for all COMPLETED invoices.
     * PRD §4.1: Partial export — skips non-COMPLETED invoices.
     */
    public ExportResult generateGstCsv(UUID clientId) {
        Client client = clientRepo.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        List<Invoice> completedInvoices = invoiceRepo.findByClientIdAndStatus(
                clientId, Invoice.ProcessingStatus.COMPLETED);

        long totalForClient = invoiceRepo.findByClientIdOrderByCreatedAtDesc(clientId).size();
        int skippedCount = (int) (totalForClient - completedInvoices.size());

        if (completedInvoices.isEmpty()) {
            throw new IllegalArgumentException("No completed invoices found for client: " + client.getClientName()
                    + ". " + skippedCount + " invoice(s) require review or are still processing.");
        }

        String csv = buildCsv(completedInvoices);
        return new ExportResult(csv, completedInvoices.size(), skippedCount);
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
            csv.append(escapeCsv(inv.getBuyerGstin())).append(",");
            csv.append(escapeCsv(inv.getLedgerAccountName()));
            csv.append("\n");
        }

        return csv.toString();
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.toPlainString();
    }

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

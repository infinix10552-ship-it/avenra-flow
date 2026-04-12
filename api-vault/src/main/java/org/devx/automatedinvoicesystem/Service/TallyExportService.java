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
 * Generates valid Tally XML Voucher documents from COMPLETED invoices.
 * PRD §4.1: Partial export allowed — only COMPLETED invoices are exported.
 * PRD §4.4: If XML generation fails → entire export fails (no partial file corruption).
 */
@Service
public class TallyExportService {

    private final InvoiceRepo invoiceRepo;
    private final ClientRepo clientRepo;

    public TallyExportService(InvoiceRepo invoiceRepo, ClientRepo clientRepo) {
        this.invoiceRepo = invoiceRepo;
        this.clientRepo = clientRepo;
    }

    /**
     * PRD §4.2: Export result with counts.
     */
    public record ExportResult(String content, int exportedCount, int skippedCount) {}

    /**
     * Generates Tally-compatible XML for all COMPLETED invoices of a client.
     * PRD §4.1: Partial export — only COMPLETED invoices, skip others.
     * PRD §4.2: Returns exportedCount and skippedCount.
     */
    public ExportResult generateTallyXml(UUID clientId) {
        Client client = clientRepo.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        // PRD §4.1: Export ONLY COMPLETED invoices
        List<Invoice> completedInvoices = invoiceRepo.findByClientIdAndStatus(
                clientId, Invoice.ProcessingStatus.COMPLETED);

        // Count skipped (non-COMPLETED)
        long totalForClient = invoiceRepo.findByClientIdOrderByCreatedAtDesc(clientId).size();
        int skippedCount = (int) (totalForClient - completedInvoices.size());

        if (completedInvoices.isEmpty()) {
            throw new IllegalArgumentException("No completed invoices found for client: " + client.getClientName()
                    + ". " + skippedCount + " invoice(s) require review or are still processing.");
        }

        // PRD §4.4: If XML generation fails → entire export fails
        String xml;
        try {
            xml = buildTallyXml(completedInvoices, client);
        } catch (Exception e) {
            throw new RuntimeException("Tally XML generation failed. Entire export aborted to prevent partial file corruption: "
                    + e.getMessage(), e);
        }

        return new ExportResult(xml, completedInvoices.size(), skippedCount);
    }

    private String buildTallyXml(List<Invoice> invoices, Client client) {
        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<ENVELOPE>\n");
        xml.append("  <HEADER>\n");
        xml.append("    <TALLYREQUEST>Import Data</TALLYREQUEST>\n");
        xml.append("  </HEADER>\n");
        xml.append("  <BODY>\n");
        xml.append("    <IMPORTDATA>\n");
        xml.append("      <REQUESTDESC>\n");
        xml.append("        <REPORTNAME>Vouchers</REPORTNAME>\n");
        xml.append("        <STATICVARIABLES>\n");
        xml.append("          <SVCURRENTCOMPANY>").append(escapeXml(client.getClientName())).append("</SVCURRENTCOMPANY>\n");
        xml.append("        </STATICVARIABLES>\n");
        xml.append("      </REQUESTDESC>\n");
        xml.append("      <REQUESTDATA>\n");

        for (Invoice inv : invoices) {
            xml.append(buildVoucherXml(inv));
        }

        xml.append("      </REQUESTDATA>\n");
        xml.append("    </IMPORTDATA>\n");
        xml.append("  </BODY>\n");
        xml.append("</ENVELOPE>\n");

        return xml.toString();
    }

    private String buildVoucherXml(Invoice inv) {
        StringBuilder v = new StringBuilder();
        String supplierName = inv.getSupplierName() != null ? inv.getSupplierName() : "Unknown Supplier";
        BigDecimal total = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal cgst = inv.getCgst() != null ? inv.getCgst() : BigDecimal.ZERO;
        BigDecimal sgst = inv.getSgst() != null ? inv.getSgst() : BigDecimal.ZERO;
        BigDecimal igst = inv.getIgst() != null ? inv.getIgst() : BigDecimal.ZERO;
        BigDecimal base = inv.getBaseTaxableAmount() != null ? inv.getBaseTaxableAmount() : BigDecimal.ZERO;

        v.append("        <TALLYMESSAGE xmlns:UDF=\"TallyUDF\">\n");
        v.append("          <VOUCHER VCHTYPE=\"Purchase\" ACTION=\"Create\">\n");
        v.append("            <DATE>").append(formatTallyDate(inv)).append("</DATE>\n");
        v.append("            <NARRATION>Invoice: ").append(escapeXml(inv.getInvoiceNumber())).append("</NARRATION>\n");
        v.append("            <VOUCHERTYPENAME>Purchase</VOUCHERTYPENAME>\n");
        v.append("            <VOUCHERNUMBER>").append(escapeXml(inv.getInvoiceNumber())).append("</VOUCHERNUMBER>\n");

        if (inv.getSupplierGstin() != null) {
            v.append("            <PARTYGSTIN>").append(escapeXml(inv.getSupplierGstin())).append("</PARTYGSTIN>\n");
        }

        // Party Ledger (Credit — supplierName)
        v.append("            <ALLLEDGERENTRIES.LIST>\n");
        v.append("              <LEDGERNAME>").append(escapeXml(supplierName)).append("</LEDGERNAME>\n");
        v.append("              <ISDEEMEDPOSITIVE>No</ISDEEMEDPOSITIVE>\n");
        v.append("              <AMOUNT>").append(total.toPlainString()).append("</AMOUNT>\n");
        v.append("            </ALLLEDGERENTRIES.LIST>\n");

        // Purchase Ledger (Debit — base amount)
        String ledgerName = inv.getLedgerAccountName() != null ? inv.getLedgerAccountName() : "Purchase Accounts";
        v.append("            <ALLLEDGERENTRIES.LIST>\n");
        v.append("              <LEDGERNAME>").append(escapeXml(ledgerName)).append("</LEDGERNAME>\n");
        v.append("              <ISDEEMEDPOSITIVE>Yes</ISDEEMEDPOSITIVE>\n");
        v.append("              <AMOUNT>-").append(base.toPlainString()).append("</AMOUNT>\n");
        v.append("            </ALLLEDGERENTRIES.LIST>\n");

        // CGST Ledger (Debit)
        if (cgst.compareTo(BigDecimal.ZERO) > 0) {
            v.append("            <ALLLEDGERENTRIES.LIST>\n");
            v.append("              <LEDGERNAME>Input CGST</LEDGERNAME>\n");
            v.append("              <ISDEEMEDPOSITIVE>Yes</ISDEEMEDPOSITIVE>\n");
            v.append("              <AMOUNT>-").append(cgst.toPlainString()).append("</AMOUNT>\n");
            v.append("            </ALLLEDGERENTRIES.LIST>\n");
        }

        // SGST Ledger (Debit)
        if (sgst.compareTo(BigDecimal.ZERO) > 0) {
            v.append("            <ALLLEDGERENTRIES.LIST>\n");
            v.append("              <LEDGERNAME>Input SGST</LEDGERNAME>\n");
            v.append("              <ISDEEMEDPOSITIVE>Yes</ISDEEMEDPOSITIVE>\n");
            v.append("              <AMOUNT>-").append(sgst.toPlainString()).append("</AMOUNT>\n");
            v.append("            </ALLLEDGERENTRIES.LIST>\n");
        }

        // IGST Ledger (Debit)
        if (igst.compareTo(BigDecimal.ZERO) > 0) {
            v.append("            <ALLLEDGERENTRIES.LIST>\n");
            v.append("              <LEDGERNAME>Input IGST</LEDGERNAME>\n");
            v.append("              <ISDEEMEDPOSITIVE>Yes</ISDEEMEDPOSITIVE>\n");
            v.append("              <AMOUNT>-").append(igst.toPlainString()).append("</AMOUNT>\n");
            v.append("            </ALLLEDGERENTRIES.LIST>\n");
        }

        v.append("          </VOUCHER>\n");
        v.append("        </TALLYMESSAGE>\n");

        return v.toString();
    }

    private String formatTallyDate(Invoice inv) {
        if (inv.getInvoiceDate() == null) return "";
        return inv.getInvoiceDate().toString().replace("-", "");
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

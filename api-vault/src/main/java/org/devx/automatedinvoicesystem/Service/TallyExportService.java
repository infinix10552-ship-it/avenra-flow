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
 * Generates valid Tally XML Voucher documents from approved invoices.
 * BLOCKS export if ANY invoice for the client has REQUIRES_MANUAL_REVIEW status.
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
     * Generates Tally-compatible XML for all COMPLETED invoices of a client.
     *
     * @throws IllegalStateException if any invoice requires manual review
     */
    public String generateTallyXml(UUID clientId) {
        Client client = clientRepo.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        // BLOCK EXPORT if any invoices need review
        boolean hasUnreviewedInvoices = invoiceRepo.existsByClientIdAndStatus(
                clientId, Invoice.ProcessingStatus.REQUIRES_MANUAL_REVIEW);

        if (hasUnreviewedInvoices) {
            long count = invoiceRepo.countByClientIdAndStatus(
                    clientId, Invoice.ProcessingStatus.REQUIRES_MANUAL_REVIEW);
            throw new IllegalStateException(
                    "Export blocked: " + count + " invoice(s) require manual review before export. "
                            + "Review and approve all flagged invoices first.");
        }

        List<Invoice> invoices = invoiceRepo.findByClientIdAndStatus(
                clientId, Invoice.ProcessingStatus.COMPLETED);

        if (invoices.isEmpty()) {
            throw new IllegalArgumentException("No completed invoices found for client: " + client.getClientName());
        }

        return buildTallyXml(invoices, client);
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
        // Tally expects YYYYMMDD format
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

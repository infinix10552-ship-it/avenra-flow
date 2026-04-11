package org.devx.automatedinvoicesystem.Service;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.devx.automatedinvoicesystem.Entity.Invoice;
import org.devx.automatedinvoicesystem.Repository.InvoiceRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Service
public class WhatsAppService {

    private final InvoiceRepo invoiceRepository;

    @Value("${twilio.whatsapp.from}")
    private String fromWhatsAppNumber;

    public WhatsAppService(InvoiceRepo invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public String sendInvoiceViaWhatsApp(UUID invoiceId, UUID organizationId, String targetPhoneNumber) {
        Invoice invoice = invoiceRepository.findByIdAndOrganizationId(invoiceId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found or access denied."));

        PhoneNumber to = new PhoneNumber("whatsapp:" + targetPhoneNumber);
        PhoneNumber from = new PhoneNumber("whatsapp:" + fromWhatsAppNumber);

        String textBody = String.format("Hello! Here is your processed invoice for *%s*. Total Amount: ₹%.2f. Ledger: %s.",
                invoice.getSupplierName() != null ? invoice.getSupplierName() : "Unknown supplier",
                invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO,
                invoice.getLedgerAccountName() != null ? invoice.getLedgerAccountName() : "N/A"
        );

//        try {
//            String originalUrl = invoice.getS3FileUrl();
//
//            // --- THE NGROK BYPASS ---
//            String ngrokUrl = "https://overt-pretheological-randi.ngrok-free.dev";
//
//            // Dynamically upgrade localhost to the public internet
//            String publicPdfUrl = originalUrl.replace("http://localhost:8081", ngrokUrl);
//
//            System.out.println("[*] Handing public URL to Twilio: " + publicPdfUrl);
//
//            Message message = Message.creator(to, from, textBody)
//                    .setMediaUrl(List.of(URI.create(publicPdfUrl)))
//                    .create();
//
//            System.out.println("🚀 [WHATSAPP] Sent successfully. Twilio SID: " + message.getSid());
//            return message.getSid();
//
//        } catch (Exception e) {
//            System.err.println("❌ [WHATSAPP] Failed to send: " + e.getMessage());
//            throw new RuntimeException("Failed to send WhatsApp message: " + e.getMessage());
//        }
        try {
            // The S3FileUrl is now a live Cloudflare R2 link
            String publicPdfUrl = invoice.getS3FileUrl();

            System.out.println("[*] Handing public R2 URL to Twilio: " + publicPdfUrl);

            Message message = Message.creator(to, from, textBody)
                    .setMediaUrl(List.of(URI.create(publicPdfUrl)))
                    .create();

            System.out.println("🚀 [WHATSAPP] Sent successfully. Twilio SID: " + message.getSid());
            return message.getSid();

        } catch (Exception e) {
            System.err.println("❌ [WHATSAPP] Failed to send: " + e.getMessage());
            throw new RuntimeException("Failed to send WhatsApp message: " + e.getMessage());
        }
    }
}
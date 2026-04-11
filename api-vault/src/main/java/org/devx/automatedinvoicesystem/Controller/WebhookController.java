package org.devx.automatedinvoicesystem.Controller;

import org.devx.automatedinvoicesystem.DTO.WebhookPayload;
import org.devx.automatedinvoicesystem.Service.InvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhook")
public class WebhookController {

    private final InvoiceService invoiceService;

    public WebhookController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    // Python will send a structured JSON POST request here
    @PostMapping("/invoice-complete")
    public ResponseEntity<?> handlePythonCompletion(@RequestBody WebhookPayload payload) {
        try {
            System.out.println("📥 Receiving webhook for invoice: " + payload.getInvoiceId());
            invoiceService.completeInvoiceProcessing(payload);
            return ResponseEntity.ok(Map.of("message", "ACK: Financial metadata saved and WebSocket fired."));
        } catch (IllegalArgumentException e) {
            System.err.println("❌ WEBHOOK ERROR (Validation): " + e.getMessage());
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ WEBHOOK CRASH: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error: " + e.getMessage()));
        }
    }
    
}
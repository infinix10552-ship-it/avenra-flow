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
    public ResponseEntity<String> handlePythonCompletion(@RequestBody WebhookPayload payload) {
        // Payload contains: invoiceId, organizationId, financialMetadata (as a Map<String, Object>)
        invoiceService.completeInvoiceProcessing(payload);

        return ResponseEntity.ok("ACK: Financial metadata saved and WebSocket fired.");
    }
    
}
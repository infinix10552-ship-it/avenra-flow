package org.devx.automatedinvoicesystem.Controller;

import org.devx.automatedinvoicesystem.DTO.WhatsAppRequest;
import org.devx.automatedinvoicesystem.Service.WhatsAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/whatsapp")
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    public WhatsAppController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @PostMapping("/send")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN')")
    public ResponseEntity<?> sendWhatsApp(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @RequestBody WhatsAppRequest request) {

        try {
            String twilioSid = whatsAppService.sendInvoiceViaWhatsApp(
                    request.getInvoiceId(),
                    organizationId,
                    request.getTargetPhoneNumber()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "WhatsApp message queued successfully!",
                    "sid", twilioSid
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

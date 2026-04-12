package org.devx.automatedinvoicesystem.Controller;

import org.devx.automatedinvoicesystem.Entity.InvoiceAuditLog;
import org.devx.automatedinvoicesystem.Repository.InvoiceAuditLogRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PRD §5.3: Audit log controller — exposes field-level change history
 * for any invoice. Read-only. Used by the review queue UI.
 */
@RestController
@RequestMapping("/api/v1/invoices")
public class AuditController {

    private final InvoiceAuditLogRepo auditLogRepo;

    public AuditController(InvoiceAuditLogRepo auditLogRepo) {
        this.auditLogRepo = auditLogRepo;
    }

    /**
     * GET /api/v1/invoices/{invoiceId}/audit-log
     * Returns field-level audit trail for a specific invoice.
     */
    @GetMapping("/{invoiceId}/audit-log")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN')")
    public ResponseEntity<?> getAuditLog(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @PathVariable UUID invoiceId) {

        List<InvoiceAuditLog> logs = auditLogRepo.findByInvoiceIdOrderByTimestampDesc(invoiceId);
        return ResponseEntity.ok(Map.of(
                "invoiceId", invoiceId,
                "auditEntries", logs.size(),
                "logs", logs
        ));
    }
}

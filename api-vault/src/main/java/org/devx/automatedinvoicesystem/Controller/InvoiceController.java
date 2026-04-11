package org.devx.automatedinvoicesystem.Controller;

import org.devx.automatedinvoicesystem.DTO.InvoiceSearchFilter;
import org.devx.automatedinvoicesystem.DTO.WebhookPayload;
import org.devx.automatedinvoicesystem.Entity.Invoice;
import org.devx.automatedinvoicesystem.Service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    // ── UPLOAD ENDPOINTS ──────────────────────────────────────────────

    @PostMapping("/upload")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN')")
    public ResponseEntity<?> uploadInvoice(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @RequestParam(value = "clientId", required = false) UUID clientId) {

        try {
            Invoice savedInvoice = invoiceService.processInvoiceUpload(file, organizationId, clientId);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Invoice uploaded successfully",
                    "invoiceId", savedInvoice.getId(),
                    "status", savedInvoice.getStatus()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("\n❌ FATAL UPLOAD CRASH:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred during upload."));
        }
    }

    @PostMapping("/upload/bulk")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN')")
    public ResponseEntity<?> uploadBulkInvoices(
            @RequestParam("file") MultipartFile zipFile,
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @RequestParam(value = "clientId", required = false) UUID clientId) {

        try {
            Map<String, Integer> reportCard = invoiceService.processBulkUpload(zipFile, organizationId, clientId);

            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "message", "Bulk processing complete",
                    "report", reportCard
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("\n❌ FATAL BULK UPLOAD CRASH:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred during bulk upload."));
        }
    }

    // ── VIEW ENDPOINTS ────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN', 'MEMBER')")
    public ResponseEntity<List<Invoice>> getAllInvoices(
            @RequestHeader("X-Organization-Id") UUID organizationId) {
        List<Invoice> invoices = invoiceService.getAllInvoices(organizationId);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN', 'MEMBER')")
    public ResponseEntity<List<Invoice>> getInvoicesByClient(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @PathVariable UUID clientId) {
        return ResponseEntity.ok(invoiceService.getInvoicesByClient(clientId));
    }

    // ── SEARCH ENDPOINT ───────────────────────────────────────────────

    @GetMapping("/search")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN', 'MEMBER')")
    public ResponseEntity<List<Invoice>> searchInvoices(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate
    ) {
        InvoiceSearchFilter filter = new InvoiceSearchFilter();
        filter.setOrganizationId(organizationId);
        filter.setClientId(clientId);
        filter.setSupplierName(supplierName);
        filter.setStatus(status);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);

        return ResponseEntity.ok(invoiceService.searchInvoices(filter));
    }

    // ── REVIEW QUEUE ENDPOINTS ────────────────────────────────────────

    @PostMapping("/{invoiceId}/approve")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN')")
    public ResponseEntity<?> approveInvoice(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @PathVariable UUID invoiceId) {
        try {
            Invoice approved = invoiceService.approveInvoice(invoiceId);
            return ResponseEntity.ok(Map.of(
                    "message", "Invoice approved",
                    "invoiceId", approved.getId(),
                    "status", approved.getStatus()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{invoiceId}/correct")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN')")
    public ResponseEntity<?> correctAndApproveInvoice(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @PathVariable UUID invoiceId,
            @RequestBody WebhookPayload correctedData) {
        try {
            Invoice corrected = invoiceService.updateAndApproveInvoice(invoiceId, correctedData);
            return ResponseEntity.ok(Map.of(
                    "message", "Invoice corrected and approved",
                    "invoiceId", corrected.getId(),
                    "status", corrected.getStatus()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
package org.devx.automatedinvoicesystem.Controller;

import org.devx.automatedinvoicesystem.Entity.Invoice;
import org.devx.automatedinvoicesystem.Service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // The Door Lock
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

    // UPLOAD ENDPOINT
    @PostMapping("/upload")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN')")
    public ResponseEntity<?> uploadInvoice(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-Organization-Id") UUID organizationId) {

        try {
            Invoice savedInvoice = invoiceService.processInvoiceUpload(file, organizationId);

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
            @RequestHeader("X-Organization-Id") UUID organizationId) {

        try {
            // Hand the ZIP file to the Conveyor Belt
            Map<String, Integer> reportCard = invoiceService.processBulkUpload(zipFile, organizationId);

            // Return the report card to React (ex."40 Success, 5 Duplicates Skipped")
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

    // VIEW ENDPOINT
    @GetMapping
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN', 'MEMBER')")
    public ResponseEntity<List<Invoice>> getAllInvoices(
            @RequestHeader("X-Organization-Id") UUID organizationId // <-- ADDED: We must know WHICH org they want to view
    ) {
        List<Invoice> invoices = invoiceService.getAllInvoices();
        return ResponseEntity.ok(invoices);
    }

    // DYNAMIC SEARCH ENDPOINT
    @GetMapping("/search")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN', 'MEMBER')")
    public ResponseEntity<List<Invoice>> searchInvoices(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @RequestParam(required = false) String vendorName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            // Spring will automatically parse "2026-03-01" into a Java LocalDate
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate
    ) {
        //Packing the shipping container
        org.devx.automatedinvoicesystem.DTO.InvoiceSearchFilter filter = new org.devx.automatedinvoicesystem.DTO.InvoiceSearchFilter();
        filter.setOrganizationId(organizationId);
        filter.setVendorName(vendorName);
        filter.setCategory(category);
        filter.setStatus(status);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);

        // Fire the engine
        List<Invoice> results = invoiceService.searchInvoices(filter);

        return ResponseEntity.ok(results);
    }
}
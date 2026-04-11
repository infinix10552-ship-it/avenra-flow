package org.devx.automatedinvoicesystem.Controller;

import org.devx.automatedinvoicesystem.Service.CsvExportService;
import org.devx.automatedinvoicesystem.Service.TallyExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices/export")
public class ExportController {

    private final TallyExportService tallyExportService;
    private final CsvExportService csvExportService;

    public ExportController(TallyExportService tallyExportService, CsvExportService csvExportService) {
        this.tallyExportService = tallyExportService;
        this.csvExportService = csvExportService;
    }

    /**
     * GET /api/v1/invoices/export/tally/{clientId}
     * Returns valid Tally XML Voucher format.
     * BLOCKS if any invoice requires manual review.
     */
    @GetMapping("/tally/{clientId}")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN')")
    public ResponseEntity<?> exportTallyXml(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @PathVariable UUID clientId) {

        try {
            String xml = tallyExportService.generateTallyXml(clientId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"tally_import_" + clientId + ".xml\"");

            return new ResponseEntity<>(xml, headers, HttpStatus.OK);

        } catch (IllegalStateException e) {
            // Export blocked — invoices need review
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/v1/invoices/export/csv/{clientId}
     * Returns ClearTax/Zoho-compatible GST CSV.
     * BLOCKS if any invoice requires manual review.
     */
    @GetMapping("/csv/{clientId}")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN')")
    public ResponseEntity<?> exportGstCsv(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @PathVariable UUID clientId) {

        try {
            String csv = csvExportService.generateGstCsv(clientId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"gst_export_" + clientId + ".csv\"");

            return new ResponseEntity<>(csv, headers, HttpStatus.OK);

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

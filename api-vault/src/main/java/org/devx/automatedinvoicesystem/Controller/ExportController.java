package org.devx.automatedinvoicesystem.Controller;

import org.devx.automatedinvoicesystem.Service.CsvExportService;
import org.devx.automatedinvoicesystem.Service.TallyExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * PRD §4: Export Engine — Non-Blocking but Safe.
 * Exports ONLY COMPLETED invoices. Returns exportedCount and skippedCount.
 * PRD §4.3: UI shows "Exported X invoices. Y require review."
 */
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
     * PRD §4.2: Returns file + exportedCount + skippedCount.
     */
    @GetMapping("/tally/{clientId}")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN')")
    public ResponseEntity<?> exportTallyXml(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @PathVariable UUID clientId) {

        try {
            TallyExportService.ExportResult result = tallyExportService.generateTallyXml(clientId);

            // PRD §4.2: Return file as base64 + counts
            String base64File = Base64.getEncoder().encodeToString(result.content().getBytes());

            return ResponseEntity.ok(Map.of(
                    "file", base64File,
                    "exportedCount", result.exportedCount(),
                    "skippedCount", result.skippedCount(),
                    "format", "tally_xml",
                    "filename", "tally_import_" + clientId + ".xml"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            // PRD §4.4: If XML generation fails → entire export fails
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Export failed: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v1/invoices/export/tally/{clientId}/download
     * Direct XML file download (alternative endpoint).
     */
    @GetMapping("/tally/{clientId}/download")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN')")
    public ResponseEntity<?> downloadTallyXml(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @PathVariable UUID clientId) {

        try {
            TallyExportService.ExportResult result = tallyExportService.generateTallyXml(clientId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"tally_import_" + clientId + ".xml\"");
            headers.set("X-Exported-Count", String.valueOf(result.exportedCount()));
            headers.set("X-Skipped-Count", String.valueOf(result.skippedCount()));

            return new ResponseEntity<>(result.content(), headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/v1/invoices/export/csv/{clientId}
     * Returns ClearTax/Zoho-compatible GST CSV.
     * PRD §4.2: Returns file + exportedCount + skippedCount.
     */
    @GetMapping("/csv/{clientId}")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN')")
    public ResponseEntity<?> exportGstCsv(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @PathVariable UUID clientId) {

        try {
            CsvExportService.ExportResult result = csvExportService.generateGstCsv(clientId);

            String base64File = Base64.getEncoder().encodeToString(result.content().getBytes());

            return ResponseEntity.ok(Map.of(
                    "file", base64File,
                    "exportedCount", result.exportedCount(),
                    "skippedCount", result.skippedCount(),
                    "format", "csv",
                    "filename", "gst_export_" + clientId + ".csv"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/v1/invoices/export/csv/{clientId}/download
     * Direct CSV file download (alternative endpoint).
     */
    @GetMapping("/csv/{clientId}/download")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN')")
    public ResponseEntity<?> downloadGstCsv(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @PathVariable UUID clientId) {

        try {
            CsvExportService.ExportResult result = csvExportService.generateGstCsv(clientId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"gst_export_" + clientId + ".csv\"");
            headers.set("X-Exported-Count", String.valueOf(result.exportedCount()));
            headers.set("X-Skipped-Count", String.valueOf(result.skippedCount()));

            return new ResponseEntity<>(result.content(), headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

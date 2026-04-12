package org.devx.automatedinvoicesystem.Controller;

import org.devx.automatedinvoicesystem.Entity.ClientLedger;
import org.devx.automatedinvoicesystem.Service.ClientLedgerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PRD §6.2: Ledger Mapping UI endpoints.
 * Upload Chart of Accounts per client. Used by frontend Ledger Mapping page.
 */
@RestController
@RequestMapping("/api/v1/clients/{clientId}/ledgers")
public class LedgerController {

    private final ClientLedgerService clientLedgerService;

    public LedgerController(ClientLedgerService clientLedgerService) {
        this.clientLedgerService = clientLedgerService;
    }

    /**
     * GET /api/v1/clients/{clientId}/ledgers
     * Returns all ledger names for a client.
     */
    @GetMapping
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<?> getLedgers(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @PathVariable UUID clientId) {

        List<ClientLedger> ledgers = clientLedgerService.getLedgers(clientId);
        return ResponseEntity.ok(Map.of(
                "clientId", clientId,
                "count", ledgers.size(),
                "ledgers", ledgers
        ));
    }

    /**
     * POST /api/v1/clients/{clientId}/ledgers
     * Adds a single ledger name.
     */
    @PostMapping
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN')")
    public ResponseEntity<?> addLedger(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @PathVariable UUID clientId,
            @RequestBody Map<String, String> payload) {

        try {
            String ledgerName = payload.get("ledgerName");
            if (ledgerName == null || ledgerName.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "ledgerName is required"));
            }

            ClientLedger created = clientLedgerService.addLedger(clientId, ledgerName);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Ledger added successfully",
                    "ledgerId", created.getId(),
                    "ledgerName", created.getLedgerName()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/v1/clients/{clientId}/ledgers
     * Bulk replace — deletes all existing ledgers and uploads new list.
     */
    @PutMapping
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN')")
    public ResponseEntity<?> replaceAllLedgers(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @PathVariable UUID clientId,
            @RequestBody Map<String, List<String>> payload) {

        try {
            List<String> ledgerNames = payload.get("ledgerNames");
            if (ledgerNames == null || ledgerNames.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "ledgerNames list is required"));
            }

            List<ClientLedger> saved = clientLedgerService.replaceAllLedgers(clientId, ledgerNames);
            return ResponseEntity.ok(Map.of(
                    "message", "Ledgers replaced successfully",
                    "count", saved.size()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/v1/clients/{clientId}/ledgers/{ledgerId}
     * Deletes a single ledger.
     */
    @DeleteMapping("/{ledgerId}")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN')")
    public ResponseEntity<?> deleteLedger(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @PathVariable UUID clientId,
            @PathVariable UUID ledgerId) {

        try {
            clientLedgerService.deleteLedger(ledgerId);
            return ResponseEntity.ok(Map.of("message", "Ledger deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

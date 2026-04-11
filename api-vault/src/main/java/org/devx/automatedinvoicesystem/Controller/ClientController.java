package org.devx.automatedinvoicesystem.Controller;

import org.devx.automatedinvoicesystem.Entity.Client;
import org.devx.automatedinvoicesystem.Service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN')")
    public ResponseEntity<?> createClient(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @RequestBody Map<String, String> payload) {

        try {
            String clientName = payload.get("clientName");
            String clientGstin = payload.get("clientGstin");
            String financialYear = payload.get("financialYear");

            if (clientName == null || clientName.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Client name is required."));
            }
            if (clientGstin == null || clientGstin.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Client GSTIN is required."));
            }
            if (financialYear == null || financialYear.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Financial year is required."));
            }

            Client created = clientService.createClient(organizationId, clientName, clientGstin, financialYear);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Client created successfully",
                    "clientId", created.getId(),
                    "clientName", created.getClientName(),
                    "clientGstin", created.getClientGstin()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN', 'MEMBER')")
    public ResponseEntity<List<Client>> getClients(
            @RequestHeader("X-Organization-Id") UUID organizationId) {
        return ResponseEntity.ok(clientService.getClientsByOrganization(organizationId));
    }

    @GetMapping("/{clientId}")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN', 'MEMBER')")
    public ResponseEntity<?> getClient(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @PathVariable UUID clientId) {
        try {
            return ResponseEntity.ok(clientService.getClientById(clientId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{clientId}")
    @PreAuthorize("@tenantSecurity.hasRole(#organizationId, 'OWNER', 'ADMIN')")
    public ResponseEntity<?> updateClient(
            @RequestHeader("X-Organization-Id") UUID organizationId,
            @PathVariable UUID clientId,
            @RequestBody Map<String, String> payload) {

        try {
            Client updated = clientService.updateClient(
                    clientId,
                    payload.get("clientName"),
                    payload.get("financialYear")
            );
            return ResponseEntity.ok(Map.of(
                    "message", "Client updated successfully",
                    "clientId", updated.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

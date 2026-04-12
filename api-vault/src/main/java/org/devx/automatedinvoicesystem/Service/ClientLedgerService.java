package org.devx.automatedinvoicesystem.Service;

import org.devx.automatedinvoicesystem.Entity.Client;
import org.devx.automatedinvoicesystem.Entity.ClientLedger;
import org.devx.automatedinvoicesystem.Repository.ClientLedgerRepo;
import org.devx.automatedinvoicesystem.Repository.ClientRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * PRD §6.2: Ledger Mapping Service.
 * Upload Chart of Accounts per client. Persist per client.
 * Used dynamically in AI processing.
 */
@Service
public class ClientLedgerService {

    private final ClientLedgerRepo clientLedgerRepo;
    private final ClientRepo clientRepo;

    public ClientLedgerService(ClientLedgerRepo clientLedgerRepo, ClientRepo clientRepo) {
        this.clientLedgerRepo = clientLedgerRepo;
        this.clientRepo = clientRepo;
    }

    /**
     * Returns all ledger names for a client.
     */
    public List<String> getLedgerNames(UUID clientId) {
        return clientLedgerRepo.findLedgerNamesByClientId(clientId);
    }

    /**
     * Returns all ledger entities for a client.
     */
    public List<ClientLedger> getLedgers(UUID clientId) {
        return clientLedgerRepo.findByClientId(clientId);
    }

    /**
     * Adds a single ledger name to a client's Chart of Accounts.
     * Rejects duplicates.
     */
    @Transactional
    public ClientLedger addLedger(UUID clientId, String ledgerName) {
        Client client = clientRepo.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        String trimmedName = ledgerName.trim();
        if (trimmedName.isBlank()) {
            throw new IllegalArgumentException("Ledger name cannot be blank.");
        }

        if (clientLedgerRepo.existsByClientIdAndLedgerName(clientId, trimmedName)) {
            throw new IllegalArgumentException("Ledger '" + trimmedName + "' already exists for this client.");
        }

        ClientLedger ledger = new ClientLedger();
        ledger.setClient(client);
        ledger.setLedgerName(trimmedName);
        return clientLedgerRepo.save(ledger);
    }

    /**
     * Bulk upload (replace) all ledgers for a client.
     * Deletes existing ledgers and replaces with the new list.
     */
    @Transactional
    public List<ClientLedger> replaceAllLedgers(UUID clientId, List<String> ledgerNames) {
        Client client = clientRepo.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        // Delete existing
        clientLedgerRepo.deleteByClientId(clientId);

        // Insert new
        List<ClientLedger> newLedgers = new ArrayList<>();
        for (String name : ledgerNames) {
            String trimmed = name.trim();
            if (!trimmed.isBlank()) {
                ClientLedger ledger = new ClientLedger();
                ledger.setClient(client);
                ledger.setLedgerName(trimmed);
                newLedgers.add(ledger);
            }
        }

        return clientLedgerRepo.saveAll(newLedgers);
    }

    /**
     * Deletes a specific ledger by ID.
     */
    @Transactional
    public void deleteLedger(UUID ledgerId) {
        if (!clientLedgerRepo.existsById(ledgerId)) {
            throw new IllegalArgumentException("Ledger not found: " + ledgerId);
        }
        clientLedgerRepo.deleteById(ledgerId);
    }
}

package org.devx.automatedinvoicesystem.Repository;

import org.devx.automatedinvoicesystem.Entity.ClientLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for per-client Chart of Accounts (ledger mappings).
 * PRD §2.2: AI must select EXACT match from clientLedgers.
 * PRD §6.2: Upload Chart of Accounts per client, persist per client.
 */
@Repository
public interface ClientLedgerRepo extends JpaRepository<ClientLedger, UUID> {

    List<ClientLedger> findByClientId(UUID clientId);

    boolean existsByClientIdAndLedgerName(UUID clientId, String ledgerName);

    void deleteByClientId(UUID clientId);

    /**
     * Returns just the ledger name strings for a client — used to build
     * the allowlist passed to the AI prompt and the validation pipeline.
     */
    @Query("SELECT cl.ledgerName FROM ClientLedger cl WHERE cl.client.id = :clientId")
    List<String> findLedgerNamesByClientId(@Param("clientId") UUID clientId);
}

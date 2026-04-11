package org.devx.automatedinvoicesystem.Repository;

import org.devx.automatedinvoicesystem.Entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepo extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<Invoice> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<Invoice> findByOrganizationIdAndStatus(UUID organizationId, Invoice.ProcessingStatus status);

    // Client-scoped queries (v2.0)
    List<Invoice> findByClientIdOrderByCreatedAtDesc(UUID clientId);

    List<Invoice> findByClientIdAndStatus(UUID clientId, Invoice.ProcessingStatus status);

    boolean existsByClientIdAndStatus(UUID clientId, Invoice.ProcessingStatus status);

    long countByClientIdAndStatus(UUID clientId, Invoice.ProcessingStatus status);

    // Fast boolean check for duplicates within a tenant
    boolean existsByFileHashAndOrganizationId(String fileHash, UUID organizationId);

    // Retry query: find failed invoices with retries remaining
    List<Invoice> findByStatusAndRetryCountLessThan(Invoice.ProcessingStatus status, int maxRetries);
}
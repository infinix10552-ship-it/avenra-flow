package org.devx.automatedinvoicesystem.Repository;

import org.devx.automatedinvoicesystem.Entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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

    // Fast boolean check for file-level duplicates within a tenant
    boolean existsByFileHashAndOrganizationId(String fileHash, UUID organizationId);

    // PRD §3.3: Duplicate detection — invoice identity check within a client
    // Composite key: (supplierGstin + invoiceNumber + invoiceDate + clientId)
    boolean existsBySupplierGstinAndInvoiceNumberAndInvoiceDateAndClientIdAndIdNot(
            String supplierGstin, String invoiceNumber, LocalDate invoiceDate, UUID clientId, UUID id);

    // Aggregation queries for Analytics (Commercial Readiness)
    long countByOrganizationId(UUID organizationId);
    
    long countByOrganizationIdAndStatus(UUID organizationId, Invoice.ProcessingStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(COALESCE(i.convertedAmountInr, i.totalAmount)) FROM Invoice i WHERE i.organization.id = :orgId AND i.status = :status")
    java.math.BigDecimal sumTotalAmountByOrganizationId(UUID orgId, Invoice.ProcessingStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(COALESCE(i.cgst, 0) + COALESCE(i.sgst, 0) + COALESCE(i.igst, 0)) FROM Invoice i WHERE i.organization.id = :orgId AND i.status = :status")
    java.math.BigDecimal sumTotalTaxByOrganizationId(UUID orgId, Invoice.ProcessingStatus status);

    // Retry query: find failed invoices with retries remaining
    List<Invoice> findByStatusAndRetryCountLessThan(Invoice.ProcessingStatus status, int maxRetries);
}
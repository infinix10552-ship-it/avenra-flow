package org.devx.automatedinvoicesystem.Repository;

import org.devx.automatedinvoicesystem.Entity.InvoiceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for field-level invoice audit logs.
 * PRD §5.3: Every manual edit must be recorded with old/new values.
 */
@Repository
public interface InvoiceAuditLogRepo extends JpaRepository<InvoiceAuditLog, UUID> {

    /**
     * Returns the audit trail for a specific invoice, most recent edits first.
     */
    List<InvoiceAuditLog> findByInvoiceIdOrderByTimestampDesc(UUID invoiceId);
}

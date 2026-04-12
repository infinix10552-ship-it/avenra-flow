package org.devx.automatedinvoicesystem.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Field-level audit trail for invoice modifications.
 * PRD §5.3: Captures every manual edit with old/new values,
 * the user who made the change, and the exact timestamp.
 *
 * <p>No silent overwrites allowed. Every change is recorded.</p>
 */
@Entity
@Table(name = "invoice_audit_logs", indexes = {
        @Index(name = "idx_audit_invoice", columnList = "invoice_id"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceAuditLog extends Base {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "field_changed", nullable = false, length = 100)
    private String fieldChanged;

    @Column(name = "old_value", length = 500)
    private String oldValue;

    @Column(name = "new_value", length = 500)
    private String newValue;

    @Column(name = "modified_by", nullable = false)
    private UUID modifiedBy;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * Factory method for concise audit log creation.
     */
    public static InvoiceAuditLog create(Invoice invoice, String fieldChanged,
                                          String oldValue, String newValue,
                                          UUID modifiedBy) {
        InvoiceAuditLog log = new InvoiceAuditLog();
        log.setInvoice(invoice);
        log.setFieldChanged(fieldChanged);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setModifiedBy(modifiedBy);
        log.setTimestamp(LocalDateTime.now());
        return log;
    }
}

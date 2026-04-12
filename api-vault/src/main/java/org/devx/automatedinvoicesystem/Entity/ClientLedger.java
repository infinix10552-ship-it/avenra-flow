package org.devx.automatedinvoicesystem.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Represents a single ledger account name from a client's Chart of Accounts.
 * PRD §2.2 & §6.2: AI must select EXACT match from clientLedgers.
 * If no match → returns null → invoice goes to REQUIRES_MANUAL_REVIEW.
 *
 * <p>Persisted per client. Uploaded via the Ledger Mapping UI.
 * Used dynamically during AI processing and backend validation.</p>
 */
@Entity
@Table(name = "client_ledgers", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_client_ledger_name",
                columnNames = {"client_id", "ledger_name"}
        )
}, indexes = {
        @Index(name = "idx_client_ledger_client", columnList = "client_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientLedger extends Base {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnore
    private Client client;

    @NotBlank(message = "Ledger name is required")
    @Column(name = "ledger_name", nullable = false, length = 200)
    private String ledgerName;
}

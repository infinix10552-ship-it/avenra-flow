package org.devx.automatedinvoicesystem.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "organization_members", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "organization_id"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMember extends Base {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    /**
     * PRD §9.2 — Role Definitions:
     * - OWNER:      Full control (firm owner, super-admin)
     * - ADMIN:      Full control (can invite users, manage billing, export)
     * - ACCOUNTANT: Can process + edit invoices (upload, review, approve)
     * - REVIEWER:   Can approve flagged invoices only (read-only + approve)
     * - MEMBER:     DEPRECATED — legacy role, maps to limited read access
     */
    public enum MemberRole {

        OWNER,       // Full control — firm owner, super-admin
        ADMIN,       // Full control — invite users, manage billing, export
        ACCOUNTANT,  // Process + edit — upload, review, approve invoices
        @Deprecated  // Legacy role — do not assign to new members. Use ACCOUNTANT or REVIEWER.
        MEMBER,
        REVIEWER     // Approve only — review and approve flagged invoices

    }

}

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

    public enum MemberRole {

        OWNER,       // Admin (firm owner) — full access
        ADMIN,       // Can invite users, manage billing, export
        ACCOUNTANT,  // Can upload, review, approve invoices
        MEMBER,      // Legacy — can only view dashboards
        REVIEWER     // Can review and approve flagged invoices only

    }

}

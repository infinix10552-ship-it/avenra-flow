package org.devx.automatedinvoicesystem.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "organization_invites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationInvite extends Base {

    @Column(nullable = false, unique = true)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String assignedRole = "MEMBER";

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public OrganizationInvite(String email, Organization organization) {
        this.email = email;
        this.organization = organization;
        this.expiresAt = LocalDateTime.now().plusDays(7); // Invites expire in 7 days
    }
}
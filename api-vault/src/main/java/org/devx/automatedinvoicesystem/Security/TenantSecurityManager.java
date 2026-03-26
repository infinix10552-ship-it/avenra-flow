package org.devx.automatedinvoicesystem.Security;

import org.devx.automatedinvoicesystem.Entity.Organization;
import org.devx.automatedinvoicesystem.Entity.OrganizationMember;
import org.devx.automatedinvoicesystem.Entity.User;
import org.devx.automatedinvoicesystem.Repository.OrganizationMemberRepo;
import org.devx.automatedinvoicesystem.Repository.OrganizationRepo;
import org.devx.automatedinvoicesystem.Repository.UserRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component("tenantSecurity")
public class TenantSecurityManager {

    private final OrganizationRepo orgRepo;
    private final UserRepo userRepo;
    private final OrganizationMemberRepo memberRepo;

    public TenantSecurityManager(OrganizationRepo orgRepo, UserRepo userRepo, OrganizationMemberRepo memberRepo) {
        this.orgRepo = orgRepo;
        this.userRepo = userRepo;
        this.memberRepo = memberRepo;
    }

    public void verifyOwnerAccess() {
        String email = getCurrentUserId(); // Returns email
        User user = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        UUID orgId = getCurrentOrganizationId();

        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        if (org.getOwnerId() == null || !org.getOwnerId().equals(user.getId())) {
            throw new RuntimeException("Security Error: Only organization owners can invite members");
        }
    }

    public UUID getCurrentOrganizationId() {
        String email = getCurrentUserId();
        User user = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        List<OrganizationMember> memberships = memberRepo.findByUser(user);
        if (memberships.isEmpty()) {
            throw new RuntimeException("User does not belong to any organization");
        }

        return memberships.get(0).getOrganization().getId();
    }

    public String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) auth.getPrincipal()).getUsername(); // This is the user's email
        }

        throw new RuntimeException("Unable to determine current user");
    }
}
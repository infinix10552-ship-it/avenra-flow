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
        String email = getCurrentUserId();
        User user = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        UUID orgId = getCurrentOrganizationId();

        List<OrganizationMember> memberships = memberRepo.findByUser(user);
        OrganizationMember currentMember = memberships.stream()
                .filter(m -> m.getOrganization().getId().equals(orgId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User is not a member of this organization"));

        if (!currentMember.getRole().toString().equalsIgnoreCase("OWNER")) {
            throw new RuntimeException("Security Error: Only organization owners can invite members");
        }

        Organization org = currentMember.getOrganization();
        if (org.getOwnerId() == null) {
            org.setOwnerId(user.getId());
            orgRepo.save(org);
            System.out.println("🔧 [AUTO-HEAL] Fixed missing owner_id for organization: " + org.getName());
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
            return ((UserDetails) auth.getPrincipal()).getUsername();
        }

        throw new RuntimeException("Unable to determine current user");
    }

    // --- EXPLICIT METHOD OVERLOADS FOR SpEL ---
    // These methods exactly match the signatures your controllers are asking for.

    public boolean hasRole(UUID organizationId, String role1) {
        return checkRoleLogic(organizationId, role1);
    }

    public boolean hasRole(UUID organizationId, String role1, String role2) {
        return checkRoleLogic(organizationId, role1, role2);
    }

    public boolean hasRole(UUID organizationId, String role1, String role2, String role3) {
        return checkRoleLogic(organizationId, role1, role2, role3);
    }

    private boolean checkRoleLogic(UUID organizationId, String... roles) {
        String email = getCurrentUserId();
        User user = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        List<OrganizationMember> memberships = memberRepo.findByUser(user);

        for (OrganizationMember member : memberships) {
            // We handle cases where the controller passes a null orgId by defaulting to their primary org
            UUID checkOrgId = (organizationId != null) ? organizationId : getCurrentOrganizationId();

            if (member.getOrganization().getId().equals(checkOrgId)) {
                for (String role : roles) {
                    if (member.getRole().toString().equalsIgnoreCase(role)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
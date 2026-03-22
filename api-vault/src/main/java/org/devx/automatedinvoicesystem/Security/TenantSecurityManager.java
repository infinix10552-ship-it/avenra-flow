package org.devx.automatedinvoicesystem.Security;

import org.devx.automatedinvoicesystem.Entity.OrganizationMember;
import org.devx.automatedinvoicesystem.Entity.User;
import org.devx.automatedinvoicesystem.Repository.OrganizationMemberRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component("tenantSecurity")
public class TenantSecurityManager {

    private final OrganizationMemberRepo memberRepo;

    public TenantSecurityManager(OrganizationMemberRepo memberRepo) {
        this.memberRepo = memberRepo;
    }

    // CHANGED: We now accept standard Strings to avoid SpEL Enum resolution bugs
    public boolean hasRole(UUID orgId, String... allowedRoles) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            System.out.println("SECURITY DENIED: No authenticated user found.");
            return false;
        }

        User currentUser = (User) authentication.getPrincipal();

        Optional<OrganizationMember> membershipOpt = memberRepo.findByUserIdAndOrganizationId(
                currentUser.getId(),
                orgId
        );

        if (membershipOpt.isEmpty()) {
            System.out.println("SECURITY DENIED: User " + currentUser.getId() + " is NOT a member of Org " + orgId);
            return false;
        }

        // Get the role from the DB and convert it to a String
        String userRole = membershipOpt.get().getRole().name();

        for (String allowedRole : allowedRoles) {
            if (userRole.equals(allowedRole)) {
                System.out.println("SECURITY GRANTED: User is " + userRole);
                return true;
            }
        }

        System.out.println("SECURITY DENIED: User is " + userRole + " but endpoint requires " + String.join(", ", allowedRoles));
        return false;
    }
}
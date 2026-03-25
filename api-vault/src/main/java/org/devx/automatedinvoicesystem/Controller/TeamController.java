package org.devx.automatedinvoicesystem.Controller;

import org.devx.automatedinvoicesystem.Entity.Organization;
import org.devx.automatedinvoicesystem.Entity.OrganizationInvite;
import org.devx.automatedinvoicesystem.Repository.OrganizationInviteRepo;
import org.devx.automatedinvoicesystem.Repository.OrganizationRepo;
import org.devx.automatedinvoicesystem.Security.TenantSecurityManager;
import org.devx.automatedinvoicesystem.Service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/team")
public class TeamController {

    private final OrganizationInviteRepo inviteRepo;
    private final OrganizationRepo orgRepo;
    private final EmailService emailService;
    private final TenantSecurityManager tenantSecurityManager;

    public TeamController(OrganizationInviteRepo inviteRepo, OrganizationRepo orgRepo, EmailService emailService, TenantSecurityManager tenantSecurityManager) {
        this.inviteRepo = inviteRepo;
        this.orgRepo = orgRepo;
        this.emailService = emailService;
        this.tenantSecurityManager = tenantSecurityManager;
    }

    @PostMapping("/invite")
    public ResponseEntity<?> inviteMember(@RequestBody Map<String, String> payload) {
        // Enforce Owner security logic
        tenantSecurityManager.verifyOwnerAccess();

        UUID orgId = tenantSecurityManager.getCurrentOrganizationId();
        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        String emailToInvite = payload.get("email");

        if (inviteRepo.findByEmail(emailToInvite).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "An invitation for this email already exists."));
        }

        OrganizationInvite invite = new OrganizationInvite(emailToInvite, org);
        inviteRepo.save(invite);

        // Send them directly to your register page
        String registerUrl = "https://avenra-flow.vercel.app/register";

        // We reuse the existing Brevo email setup here for speed
        emailService.sendPasswordResetEmail(emailToInvite, registerUrl);

        return ResponseEntity.ok(Map.of("message", "Invitation sent successfully"));
    }
}
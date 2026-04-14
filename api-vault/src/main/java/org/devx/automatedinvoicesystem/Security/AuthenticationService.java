package org.devx.automatedinvoicesystem.Security;

import jakarta.transaction.Transactional;
import org.devx.automatedinvoicesystem.DTO.AuthenticationRequest;
import org.devx.automatedinvoicesystem.DTO.AuthenticationResponse;
import org.devx.automatedinvoicesystem.DTO.RegisterRequest;
import org.devx.automatedinvoicesystem.Entity.*;
import org.devx.automatedinvoicesystem.Repository.*;
import org.devx.automatedinvoicesystem.Service.EmailService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuthenticationService {

    private final UserRepo userRepo;
    private final OrganizationRepo organizationRepository;
    private final OrganizationMemberRepo orgMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepo tokenRepository;
    private final EmailService emailService;
    private final OrganizationInviteRepo inviteRepo;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url}")
    private String frontendUrl;

    public AuthenticationService(UserRepo userRepo, OrganizationRepo organizationRepository, OrganizationMemberRepo orgMemberRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager, PasswordResetTokenRepo tokenRepository, EmailService emailService, OrganizationInviteRepo inviteRepo) {
        this.userRepo = userRepo;
        this.organizationRepository = organizationRepository;
        this.orgMemberRepository = orgMemberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.inviteRepo = inviteRepo;
    }

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user = userRepo.save(user);

        // --- PRE-AUTH INVITATION INTERCEPTOR ---
        Optional<OrganizationInvite> pendingInvite = inviteRepo.findByEmail(request.getEmail());
        Organization targetOrg;
        OrganizationMember.MemberRole userRole;

        if (pendingInvite.isPresent() && pendingInvite.get().getExpiresAt().isAfter(LocalDateTime.now())) {
            targetOrg = pendingInvite.get().getOrganization();
            try {
                userRole = OrganizationMember.MemberRole.valueOf(pendingInvite.get().getAssignedRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                userRole = OrganizationMember.MemberRole.MEMBER;
            }
            inviteRepo.delete(pendingInvite.get());
        } else {
            if (pendingInvite.isPresent()) inviteRepo.delete(pendingInvite.get());

            targetOrg = new Organization();
            String orgName = request.getOrganizationName();
            if (orgName == null || orgName.trim().isEmpty()) {
                orgName = user.getFirstName() + "'s Personal Workspace";
            }
            targetOrg.setName(orgName);
            targetOrg.setOwnerId(user.getId()); // Set the Owner ID
            targetOrg = organizationRepository.save(targetOrg);
            userRole = OrganizationMember.MemberRole.OWNER;
        }

        OrganizationMember member = new OrganizationMember();
        member.setUser(user);
        member.setOrganization(targetOrg);
        member.setRole(userRole);
        orgMemberRepository.save(member);

        String jwtToken = jwtService.generateToken(user, targetOrg.getId().toString());
        return new AuthenticationResponse(jwtToken);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        User user = userRepo.findByEmail(request.getEmail()).orElseThrow();

        String targetOrgId = null;
        List<OrganizationMember> memberships = orgMemberRepository.findByUser(user);
        if (!memberships.isEmpty()) {
            targetOrgId = memberships.get(0).getOrganization().getId().toString();
        }

        String jwtToken = jwtService.generateToken(user, targetOrgId);
        return new AuthenticationResponse(jwtToken);
    }

    @Transactional
    public void processForgotPassword(String email) {
        User user = userRepo.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("No account found with that email."));
        tokenRepository.deleteByUser(user);
        String token = java.util.UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        tokenRepository.save(resetToken);
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        System.out.println("🔐 [AUTH] Attempting password reset for token: " + token);
        
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or missing token. The link may have been already used or is incorrect."));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new IllegalArgumentException("This reset link has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        if (user == null) {
            throw new IllegalStateException("Critical Error: Token exists but is not linked to any user.");
        }

        // Update password with security-hardened encoding
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.saveAndFlush(user); 

        // CRITICAL: Clean up the token so it cannot be reused
        tokenRepository.delete(resetToken);
        tokenRepository.flush();
        
        System.out.println("✅ [AUTH] Password reset successful for user: " + user.getEmail());
    }
}
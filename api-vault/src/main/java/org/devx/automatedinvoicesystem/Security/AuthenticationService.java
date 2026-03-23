package org.devx.automatedinvoicesystem.Security;

import jakarta.transaction.Transactional;
import org.devx.automatedinvoicesystem.DTO.AuthenticationRequest;
import org.devx.automatedinvoicesystem.DTO.AuthenticationResponse;
import org.devx.automatedinvoicesystem.DTO.RegisterRequest;
import org.devx.automatedinvoicesystem.Entity.Organization;
import org.devx.automatedinvoicesystem.Entity.OrganizationMember;
import org.devx.automatedinvoicesystem.Entity.PasswordResetToken;
import org.devx.automatedinvoicesystem.Entity.User;
import org.devx.automatedinvoicesystem.Repository.OrganizationMemberRepo;
import org.devx.automatedinvoicesystem.Repository.OrganizationRepo;
import org.devx.automatedinvoicesystem.Repository.PasswordResetTokenRepo;
import org.devx.automatedinvoicesystem.Repository.UserRepo;
import org.devx.automatedinvoicesystem.Service.EmailService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

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
    @org.springframework.beans.factory.annotation.Value("${app.frontend.url}")
    private String frontendUrl;

    // --- NEW: Injected Organization Repositories ---
    public AuthenticationService(UserRepo userRepo,
                                 OrganizationRepo organizationRepository,
                                 OrganizationMemberRepo orgMemberRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtService jwtService,
                                 AuthenticationManager authenticationManager,
                                 PasswordResetTokenRepo tokenRepository,
                                 EmailService emailService) {
        this.userRepo = userRepo;
        this.organizationRepository = organizationRepository;
        this.orgMemberRepository = orgMemberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    @Transactional // Ensures User, Org, and Member are all saved together or rollback
    public AuthenticationResponse register(RegisterRequest request) {
        // 1. Safety Check: Does the email already exist?
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        // 2. Build the new User entity
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user = userRepo.save(user);

        // 3. Create their Workspace using the provided name, or fallback to default
        Organization org = new Organization();
        String orgName = request.getOrganizationName();
        
        if (orgName == null || orgName.trim().isEmpty()) {
            orgName = user.getFirstName() + "'s Personal Workspace";
        }
        
        org.setName(orgName);
        org = organizationRepository.save(org);

        // 4. NEW: Link the user as the OWNER of this Workspace
        OrganizationMember member = new OrganizationMember();
        member.setUser(user);
        member.setOrganization(org);
        member.setRole(OrganizationMember.MemberRole.OWNER);
        orgMemberRepository.save(member);

        // 5. NEW: Mint the JWT with the specific Organization ID embedded
        String targetOrgId = org.getId().toString();
        String jwtToken = jwtService.generateToken(user, targetOrgId);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // 1. The Vault Check
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. Fetch User
        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow();

        // 3. NEW: Fetch their Workspace ID to embed in the token
        String targetOrgId = null;
        List<OrganizationMember> memberships = orgMemberRepository.findByUser(user);
        if (!memberships.isEmpty()) {
            targetOrgId = memberships.get(0).getOrganization().getId().toString();
        }

        // 4. NEW: Mint the JWT using the overloaded method
        String jwtToken = jwtService.generateToken(user, targetOrgId);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    // PASSWORD RESET LOGIC STARTS HERE

    @Transactional
    public void processForgotPassword(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No account found with that email."));

        // (This prevents the unique constraint crash)
        tokenRepository.deleteByUser(user);

        String token = java.util.UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        tokenRepository.save(resetToken);

//        String resetUrl = "http://localhost:5173/reset-password?token=" + token;
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or missing token."));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new IllegalArgumentException("This reset link has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        tokenRepository.delete(resetToken);

        System.out.println("🔒 [AUTH] Password successfully reset for user: " + user.getEmail());
    }
}
package org.devx.automatedinvoicesystem.Security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.devx.automatedinvoicesystem.Entity.Organization;
import org.devx.automatedinvoicesystem.Entity.OrganizationInvite;
import org.devx.automatedinvoicesystem.Entity.OrganizationMember;
import org.devx.automatedinvoicesystem.Entity.User;
import org.devx.automatedinvoicesystem.Repository.OrganizationInviteRepo;
import org.devx.automatedinvoicesystem.Repository.OrganizationMemberRepo;
import org.devx.automatedinvoicesystem.Repository.OrganizationRepo;
import org.devx.automatedinvoicesystem.Repository.UserRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepo userRepository;
    private final OrganizationRepo organizationRepository;
    private final OrganizationMemberRepo orgMemberRepository;
    private final JwtService jwtService;
    private final OrganizationInviteRepo inviteRepo;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(UserRepo userRepository, OrganizationRepo organizationRepository, OrganizationMemberRepo orgMemberRepository, JwtService jwtService, OrganizationInviteRepo inviteRepo) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.orgMemberRepository = orgMemberRepository;
        this.jwtService = jwtService;
        this.inviteRepo = inviteRepo;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        if (lastName == null) lastName = "";
        if (firstName == null) firstName = "User";

        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;
        String targetOrgId;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            List<OrganizationMember> memberships = orgMemberRepository.findByUser(user);
            targetOrgId = memberships.isEmpty() ? null : memberships.get(0).getOrganization().getId().toString();
        } else {
            user = new User();
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPassword("OAUTH2_USER_NO_PASSWORD");
            user = userRepository.save(user);

            //  PRE-AUTH INVITATION INTERCEPTOR
            Optional<OrganizationInvite> pendingInvite = inviteRepo.findByEmail(email);
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
                targetOrg.setName(firstName + "'s Personal Workspace");
                targetOrg.setOwnerId(user.getId()); // Set the Owner ID
                targetOrg = organizationRepository.save(targetOrg);
                userRole = OrganizationMember.MemberRole.OWNER;
            }

            OrganizationMember member = new OrganizationMember();
            member.setUser(user);
            member.setOrganization(targetOrg);
            member.setRole(userRole);
            orgMemberRepository.save(member);

            targetOrgId = targetOrg.getId().toString();
        }

        String jwtToken = jwtService.generateToken(user, targetOrgId);
        String baseFrontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        response.sendRedirect(baseFrontendUrl + "/oauth2-redirect?token=" + jwtToken);
    }
}
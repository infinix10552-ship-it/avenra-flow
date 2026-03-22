package org.devx.automatedinvoicesystem.Security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.devx.automatedinvoicesystem.Entity.Organization;
import org.devx.automatedinvoicesystem.Entity.OrganizationMember;
import org.devx.automatedinvoicesystem.Entity.User;
import org.devx.automatedinvoicesystem.Repository.OrganizationMemberRepo;
import org.devx.automatedinvoicesystem.Repository.OrganizationRepo;
import org.devx.automatedinvoicesystem.Repository.UserRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepo userRepository;
    private final OrganizationRepo organizationRepository;
    private final OrganizationMemberRepo orgMemberRepository;
    private final JwtService jwtService;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url}")
    private String frontendUrl;
    public OAuth2LoginSuccessHandler(UserRepo userRepository,
                                     OrganizationRepo organizationRepository,
                                     OrganizationMemberRepo orgMemberRepository,
                                     JwtService jwtService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.orgMemberRepository = orgMemberRepository;
        this.jwtService = jwtService;
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
        String targetOrgId = null; // We will store the extracted UUID here

        if (existingUser.isPresent()) {
            user = existingUser.get();

            // Fetch the existing user's Organization ID
            List<OrganizationMember> memberships = orgMemberRepository.findByUser(user);
            if (!memberships.isEmpty()) {
                // Grab the first workspace they belong to
                targetOrgId = memberships.get(0).getOrganization().getId().toString();
            }
        } else {
            // New UsEr: Register them on the fly
            user = new User();
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPassword("OAUTH2_USER_NO_PASSWORD");
            user = userRepository.save(user);

            Organization org = new Organization();
            org.setName(firstName + "'s Personal Workspace");
            org = organizationRepository.save(org);

            OrganizationMember member = new OrganizationMember();
            member.setUser(user);
            member.setOrganization(org);
            member.setRole(OrganizationMember.MemberRole.OWNER);
            orgMemberRepository.save(member);

            // Set the targetOrgId for the newly created workspace
            targetOrgId = org.getId().toString();
        }

        String jwtToken = jwtService.generateToken(user, targetOrgId);

        // Normalize the frontend URL to avoid double slashes
        String baseFrontendUrl = frontendUrl.endsWith("/") 
                                 ? frontendUrl.substring(0, frontendUrl.length() - 1) 
                                 : frontendUrl;

        String redirectUrl = baseFrontendUrl + "/oauth2-redirect?token=" + jwtToken;
        response.sendRedirect(redirectUrl);
    }
}
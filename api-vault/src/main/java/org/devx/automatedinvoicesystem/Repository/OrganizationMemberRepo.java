package org.devx.automatedinvoicesystem.Repository;

import org.devx.automatedinvoicesystem.Entity.OrganizationMember;
import org.devx.automatedinvoicesystem.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationMemberRepo extends JpaRepository<OrganizationMember, UUID> {

    //Used to check if this user belongs to this specific company
    Optional<OrganizationMember> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    //Used to get all employees associated to that specific company dashboard
    List<OrganizationMember> findByOrganizationId(UUID organizationId);

    List<OrganizationMember> findByUser(User user);
}
package org.devx.automatedinvoicesystem.Repository;

import org.devx.automatedinvoicesystem.Entity.OrganizationInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationInviteRepo extends JpaRepository<OrganizationInvite, UUID> {

    Optional<OrganizationInvite> findByEmail(String email);

}
package org.devx.automatedinvoicesystem.Repository;

import org.devx.automatedinvoicesystem.Entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClientRepo extends JpaRepository<Client, UUID> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"clientLedgers"})
    List<Client> findByOrganizationIdOrderByClientNameAsc(UUID organizationId);

    boolean existsByClientGstinAndOrganizationId(String clientGstin, UUID organizationId);

    boolean existsByClientGstinAndFinancialYearAndOrganizationId(
            String clientGstin, String financialYear, UUID organizationId);
}

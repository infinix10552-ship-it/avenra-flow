package org.devx.automatedinvoicesystem.Repository;

import org.devx.automatedinvoicesystem.Entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepo  extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<Invoice> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<Invoice> findByOrganizationIdAndStatus(UUID organizationId, Invoice.ProcessingStatus status);

    //Fast boolean check for duplicates within a tenant
    boolean existsByFileHashAndOrganizationId(String fileHash, UUID organizationId);

}
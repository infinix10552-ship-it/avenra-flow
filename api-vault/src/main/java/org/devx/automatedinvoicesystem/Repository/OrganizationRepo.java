package org.devx.automatedinvoicesystem.Repository;

import org.devx.automatedinvoicesystem.Entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepo extends JpaRepository<Organization,UUID> {

    // by extending with UUID this file instantly inhereits methods like .save(), .findAll(),.findById(), etc

    Optional<Organization> findByName(String name);

}
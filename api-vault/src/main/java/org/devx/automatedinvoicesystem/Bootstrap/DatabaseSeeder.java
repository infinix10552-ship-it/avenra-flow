package org.devx.automatedinvoicesystem.Bootstrap;

import org.devx.automatedinvoicesystem.Entity.Organization;
import org.devx.automatedinvoicesystem.Repository.OrganizationRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    // This class is responsible for seeding the database with initial data when the application starts.
    // It can be used to create default users, roles, or any other necessary data for the application to function properly.

    private final OrganizationRepo organizationRepository;

    public DatabaseSeeder(OrganizationRepo organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        // We only want to create the test company if the database is empty.
        // If we restart the app 10 times, we don't want 10 identical companies!
        String companyName = "Aman Jha Decodes Studios";

        Optional<Organization> existingOrg = organizationRepository.findByName(companyName);

        if (existingOrg.isEmpty()) {
            Organization newOrg = new Organization();
            newOrg.setName(companyName);

            Organization savedOrg = organizationRepository.save(newOrg);

            System.out.println(" DATABASE SEEDED SUCCESSFULLY ");
            System.out.println(" Test Organization Created: " + savedOrg.getName());
            System.out.println(" CRITICAL - COPY UUID FOR POSTMAN: " + savedOrg.getId());
        } else {
            System.out.println(" Database already seeded. Using existing Organization ID: " + existingOrg.get().getId());
        }
    }

}






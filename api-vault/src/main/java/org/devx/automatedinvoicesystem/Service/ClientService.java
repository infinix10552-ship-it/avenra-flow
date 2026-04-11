package org.devx.automatedinvoicesystem.Service;

import org.devx.automatedinvoicesystem.Entity.Client;
import org.devx.automatedinvoicesystem.Entity.Organization;
import org.devx.automatedinvoicesystem.Repository.ClientRepo;
import org.devx.automatedinvoicesystem.Repository.OrganizationRepo;
import org.devx.automatedinvoicesystem.Validation.GstinValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ClientService {

    private final ClientRepo clientRepo;
    private final OrganizationRepo organizationRepo;

    public ClientService(ClientRepo clientRepo, OrganizationRepo organizationRepo) {
        this.clientRepo = clientRepo;
        this.organizationRepo = organizationRepo;
    }

    @Transactional
    public Client createClient(UUID organizationId, String clientName, String clientGstin, String financialYear) {

        // 1. Validate GSTIN format deterministically
        if (!GstinValidator.isValid(clientGstin)) {
            throw new IllegalArgumentException(
                    "Invalid GSTIN format: " + clientGstin
                            + ". Expected format: 2-digit state code + 10-char PAN + entity code + Z + checksum (e.g., 27AAPFU0939F1ZV)");
        }

        // 2. Validate state code range
        if (!GstinValidator.hasValidStateCode(clientGstin)) {
            throw new IllegalArgumentException(
                    "Invalid state code in GSTIN: " + clientGstin.substring(0, 2)
                            + ". Valid range: 01-37.");
        }

        // 3. Check for duplicate GSTIN+FY within the same firm
        if (clientRepo.existsByClientGstinAndFinancialYearAndOrganizationId(
                clientGstin, financialYear, organizationId)) {
            throw new IllegalArgumentException(
                    "A client with GSTIN " + clientGstin
                            + " already exists for financial year " + financialYear + " in this organization.");
        }

        // 4. Fetch parent organization
        Organization organization = organizationRepo.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with ID: " + organizationId));

        // 5. Build and persist
        Client client = new Client();
        client.setOrganization(organization);
        client.setClientName(clientName.trim());
        client.setClientGstin(clientGstin.trim().toUpperCase());
        client.setFinancialYear(financialYear.trim());

        return clientRepo.save(client);
    }

    public List<Client> getClientsByOrganization(UUID organizationId) {
        return clientRepo.findByOrganizationIdOrderByClientNameAsc(organizationId);
    }

    public Client getClientById(UUID clientId) {
        return clientRepo.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found with ID: " + clientId));
    }

    @Transactional
    public Client updateClient(UUID clientId, String clientName, String financialYear) {
        Client client = getClientById(clientId);

        if (clientName != null && !clientName.isBlank()) {
            client.setClientName(clientName.trim());
        }
        if (financialYear != null && !financialYear.isBlank()) {
            client.setFinancialYear(financialYear.trim());
        }

        return clientRepo.save(client);
    }
}

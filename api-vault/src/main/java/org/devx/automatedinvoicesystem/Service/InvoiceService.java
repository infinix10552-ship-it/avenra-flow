package org.devx.automatedinvoicesystem.Service;

import org.devx.automatedinvoicesystem.DTO.InvoiceSearchFilter;
import org.devx.automatedinvoicesystem.DTO.WebhookPayload;
import org.devx.automatedinvoicesystem.Entity.Invoice;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface InvoiceService {

    Invoice processInvoiceUpload(MultipartFile file, UUID organizationId);

    Invoice processInvoiceUpload(MultipartFile file, UUID organizationId, UUID clientId);

    List<Invoice> getAllInvoices();

    List<Invoice> getAllInvoices(UUID organizationId);

    List<Invoice> getInvoicesByClient(UUID clientId);

    Invoice getInvoiceById(UUID invoiceId, UUID organizationId);

    Map<String, Integer> processBulkUpload(MultipartFile zipFile, UUID organizationId);

    Map<String, Integer> processBulkUpload(MultipartFile zipFile, UUID organizationId, UUID clientId);

    void completeInvoiceProcessing(WebhookPayload payload);

    List<Invoice> searchInvoices(InvoiceSearchFilter filter);

    // Review queue methods
    Invoice approveInvoice(UUID invoiceId);

    Invoice updateAndApproveInvoice(UUID invoiceId, WebhookPayload correctedData);
}
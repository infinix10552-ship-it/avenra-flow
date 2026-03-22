package org.devx.automatedinvoicesystem.Service;

import org.devx.automatedinvoicesystem.DTO.InvoiceSearchFilter;
import org.devx.automatedinvoicesystem.DTO.WebhookPayload;
import org.devx.automatedinvoicesystem.Entity.Invoice;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface InvoiceService {

    Invoice processInvoiceUpload(MultipartFile file, UUID organizationId);

    // This method is used to retrieve a specific invoice by its ID and the organization it belongs to.
    List<Invoice> getAllInvoices();

    // THE WEBHOOK METHOD: Python will call this when it finishes the OCR
//    void completeInvoiceProcessing(UUID invoiceId, UUID organizationId, String extractedData);

    // THE BULK ENGINE: Returns a summary report of the batch process
    java.util.Map<String, Integer> processBulkUpload(MultipartFile zipFile, UUID organizationId);

    void completeInvoiceProcessing(WebhookPayload payload);

    // Add this new method signature
    List<Invoice> searchInvoices(InvoiceSearchFilter filter);

}
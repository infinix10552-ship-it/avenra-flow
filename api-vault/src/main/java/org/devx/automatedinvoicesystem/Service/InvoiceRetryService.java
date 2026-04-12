package org.devx.automatedinvoicesystem.Service;

import org.devx.automatedinvoicesystem.Entity.Invoice;
import org.devx.automatedinvoicesystem.Entity.ProcessingLog;
import org.devx.automatedinvoicesystem.Repository.InvoiceRepo;
import org.devx.automatedinvoicesystem.Repository.ProcessingLogRepo;
import org.devx.automatedinvoicesystem.Service.impl.InvoiceMessagePublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduled job that re-queues FAILED invoices for retry.
 * Maximum 3 retries per invoice.
 *
 * PRD §3.3: Invoices with failureReason = DUPLICATE_INVOICE are NEVER retried
 * (they are permanent failures — retrying would produce the same result).
 */
@Service
public class InvoiceRetryService {

    private static final int MAX_RETRIES = 3;

    private final InvoiceRepo invoiceRepo;
    private final ProcessingLogRepo processingLogRepo;
    private final InvoiceMessagePublisher messagePublisher;

    public InvoiceRetryService(InvoiceRepo invoiceRepo,
                                ProcessingLogRepo processingLogRepo,
                                InvoiceMessagePublisher messagePublisher) {
        this.invoiceRepo = invoiceRepo;
        this.processingLogRepo = processingLogRepo;
        this.messagePublisher = messagePublisher;
    }

    /**
     * Runs every 5 minutes. Finds FAILED invoices with retries < MAX_RETRIES
     * and re-queues them to RabbitMQ.
     *
     * Skips invoices with permanent failure reasons (e.g., DUPLICATE_INVOICE).
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    @Transactional
    public void retryFailedInvoices() {
        List<Invoice> failedInvoices = invoiceRepo.findByStatusAndRetryCountLessThan(
                Invoice.ProcessingStatus.FAILED, MAX_RETRIES);

        if (failedInvoices.isEmpty()) {
            return;
        }

        System.out.println("[RETRY] Found " + failedInvoices.size() + " failed invoices to evaluate.");

        for (Invoice invoice : failedInvoices) {
            // PRD §3.3: NEVER retry permanent failures
            if ("DUPLICATE_INVOICE".equals(invoice.getFailureReason())) {
                System.out.println("[RETRY] Skipping invoice " + invoice.getId()
                        + " — permanent failure: " + invoice.getFailureReason());
                continue;
            }

            invoice.setRetryCount(invoice.getRetryCount() + 1);
            invoice.setStatus(Invoice.ProcessingStatus.PENDING);
            invoice.setFailureReason(null); // Clear for retry
            invoiceRepo.save(invoice);

            processingLogRepo.save(ProcessingLog.create(
                    invoice, ProcessingLog.LogLevel.INFO, "RETRY",
                    "Retry attempt " + invoice.getRetryCount() + "/" + MAX_RETRIES
            ));

            messagePublisher.sendInvoiceToQueue(invoice);
            System.out.println("[RETRY] Re-queued invoice " + invoice.getId()
                    + " (attempt " + invoice.getRetryCount() + "/" + MAX_RETRIES + ")");
        }
    }
}

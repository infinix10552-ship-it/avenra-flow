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
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    @Transactional
    public void retryFailedInvoices() {
        List<Invoice> failedInvoices = invoiceRepo.findByStatusAndRetryCountLessThan(
                Invoice.ProcessingStatus.FAILED, MAX_RETRIES);

        if (failedInvoices.isEmpty()) {
            return;
        }

        System.out.println("[RETRY] Found " + failedInvoices.size() + " failed invoices to retry.");

        for (Invoice invoice : failedInvoices) {
            invoice.setRetryCount(invoice.getRetryCount() + 1);
            invoice.setStatus(Invoice.ProcessingStatus.PENDING);
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

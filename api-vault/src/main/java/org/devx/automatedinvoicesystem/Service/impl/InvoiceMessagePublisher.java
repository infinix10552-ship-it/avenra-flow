package org.devx.automatedinvoicesystem.Service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.devx.automatedinvoicesystem.Config.RabbitMQConfig;
import org.devx.automatedinvoicesystem.Entity.Invoice;
import org.devx.automatedinvoicesystem.Repository.ClientLedgerRepo;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InvoiceMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final ClientLedgerRepo clientLedgerRepo;

    public InvoiceMessagePublisher(RabbitTemplate rabbitTemplate,
                                    ObjectMapper objectMapper,
                                    ClientLedgerRepo clientLedgerRepo) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.clientLedgerRepo = clientLedgerRepo;
    }

    public void sendInvoiceToQueue(Invoice invoice) {
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("invoiceId", invoice.getId().toString());
        messagePayload.put("organizationId", invoice.getOrganization().getId().toString());
        messagePayload.put("clientId", invoice.getClient() != null ? invoice.getClient().getId().toString() : null);
        messagePayload.put("fileUrl", invoice.getS3FileUrl());

        // PRD §2.1 & §2.2: Include client's Chart of Accounts for AI ledger mapping
        if (invoice.getClient() != null) {
            List<String> ledgerNames = clientLedgerRepo.findLedgerNamesByClientId(
                    invoice.getClient().getId());
            messagePayload.put("clientLedgers", ledgerNames);
        }

        try {
            String pureJsonString = objectMapper.writeValueAsString(messagePayload);

            // ── TRANSACTIONAL ROBUSTNESS ──────────────────────────────────
            // We only fire the message AFTER the database has committed.
            // This prevents the "Race Condition" where the AI worker starts
            // processing before the Invoice ID exists in the DB.
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            System.out.println("🚀 [ASYNC] Transaction committed. Firing Job to RabbitMQ for Invoice: " + invoice.getId());
                            rabbitTemplate.convertAndSend(
                                    RabbitMQConfig.INVOICE_EXCHANGE,
                                    RabbitMQConfig.INVOICE_ROUTING_KEY,
                                    pureJsonString
                            );
                        }
                    }
                );
            } else {
                System.out.println("🚀 [SYNC] No active transaction. Firing Job to RabbitMQ for Invoice: " + invoice.getId());
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.INVOICE_EXCHANGE,
                        RabbitMQConfig.INVOICE_ROUTING_KEY,
                        pureJsonString
                );
            }

        } catch (JsonProcessingException e) {
            System.err.println("❌ Failed to parse payload into JSON: " + e.getMessage());
        }
    }
}
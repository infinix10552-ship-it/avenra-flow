package org.devx.automatedinvoicesystem.Service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.devx.automatedinvoicesystem.Config.RabbitMQConfig;
import org.devx.automatedinvoicesystem.Entity.Invoice;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class InvoiceMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper; // Jackson's core JSON engine

    // Spring Boot automatically injects the modern ObjectMapper
    public InvoiceMessagePublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendInvoiceToQueue(Invoice invoice) {
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("invoiceId", invoice.getId().toString());
        messagePayload.put("organizationId", invoice.getOrganization().getId().toString());
        messagePayload.put("fileUrl", invoice.getS3FileUrl());

        try {
            // THE OVERRIDE: We manually convert the Map into a pure JSON String.
            // This guarantees RabbitMQ gets clean text, completely bypassing Java Serialization (0xac).
            String pureJsonString = objectMapper.writeValueAsString(messagePayload);

            System.out.println("🚀 Publishing Job to RabbitMQ for Invoice: " + invoice.getId());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.INVOICE_EXCHANGE,
                    RabbitMQConfig.INVOICE_ROUTING_KEY,
                    pureJsonString
            );

        } catch (JsonProcessingException e) {
            System.err.println("❌ Failed to parse payload into JSON: " + e.getMessage());
        }
    }
}
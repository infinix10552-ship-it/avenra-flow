package org.devx.automatedinvoicesystem.Service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }


//      Broadcasts a real-time invoice update to a specific organization's React clients.

    public void sendInvoiceStatusUpdate(UUID organizationId, UUID invoiceId, String status) {

        // 1. THE CHANNEL: We dynamically construct the topic URL for this specific tenant
        String destination = "/topic/organization/" + organizationId;

        // 2. THE PAYLOAD: The exact JSON object React will receive
        Map<String, Object> payload = Map.of(
                "type", "INVOICE_STATUS_UPDATE",
                "invoiceId", invoiceId,
                "status", status,
                "timestamp", System.currentTimeMillis()
        );

        // 3. THE BROADCAST: Fire it down the WebSocket highway
        messagingTemplate.convertAndSend(destination, Optional.of(payload));

        System.out.println("🚀 [WEBSOCKET] Broadcasted update for Invoice " + invoiceId + " to Org " + organizationId);
    }
}
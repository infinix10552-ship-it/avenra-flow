import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export function useWebSocket(onMessageReceived) {
  // We use a ref to hold the client so it persists across renders without causing re-renders itself
  const clientRef = useRef(null);

  useEffect(() => {
    // 1. Retrieve the Vault Keys
    const token = localStorage.getItem('avenra_token');
    const orgId = localStorage.getItem('avenra_org_id');

    if (!token || !orgId) return;

    // 2. Initialize the STOMP Client
    const client = new Client({
      // We use SockJS as a fallback bridge in case the user's corporate firewall blocks pure WebSockets
      webSocketFactory: () => new SockJS(import.meta.env.VITE_WS_BASE_URL),
      
      // CRITICAL: Pass the JWT in the handshake so Java's WebSocketSecurityInterceptor lets us in
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      
      debug: () => {
        // Uncomment this if you need to deep-debug the socket connection
        // console.log('[STOMP] ' + str);
      },
      
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    // 3. The Connection Lifecycle
    client.onConnect = () => {
      console.log('🚀 [WEBSOCKET] Connected to Avenra Real-Time Highway.');
      
      // Subscribe to this specific organization's private broadcast channel
      const subscriptionTopic = `/topic/organization/${orgId}`;
      
      client.subscribe(subscriptionTopic, (message) => {
        if (message.body) {
          const payload = JSON.parse(message.body);
          console.log('📥 [WEBSOCKET] Payload Received:', payload);
          // Pass the data back to the React component that called this hook
          if (onMessageReceived) onMessageReceived(payload);
        }
      });
    };

    client.onStompError = (frame) => {
      console.error('❌ [WEBSOCKET] Broker reported error: ' + frame.headers['message']);
      console.error('❌ [WEBSOCKET] Additional details: ' + frame.body);
    };

    // 4. Ignite the Engine
    client.activate();
    clientRef.current = client;

    // 5. The Teardown (Memory Leak Prevention)
    // When the user leaves the dashboard, React will run this return function to sever the connection gracefully.
    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate();
        console.log('🛑 [WEBSOCKET] Connection severed gracefully.');
      }
    };
  }, [onMessageReceived]);

}
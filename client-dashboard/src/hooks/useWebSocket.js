import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export function useWebSocket(onMessageReceived) {
  // Hold the STOMP client in a ref so it persists across renders
  const clientRef = useRef(null);

  // CRITICAL FIX: Store the callback in a ref so the STOMP effect never needs
  // to re-run when the callback reference changes. Without this, every render
  // of DashboardLayout (triggered by navigation) would teardown and reconnect
  // the WebSocket, causing the white screen flash.
  const onMessageRef = useRef(onMessageReceived);
  useEffect(() => {
    onMessageRef.current = onMessageReceived;
  }, [onMessageReceived]);

  useEffect(() => {
    // 1. Retrieve the Vault Keys
    const token = localStorage.getItem('avenra_token');
    const orgId = localStorage.getItem('avenra_org_id');

    if (!token || !orgId) return;

    // 2. Initialize the STOMP Client
    const client = new Client({
      // SockJS as a fallback bridge for corporate firewalls blocking pure WebSockets
      webSocketFactory: () => new SockJS(import.meta.env.VITE_WS_BASE_URL),

      // CRITICAL: Pass the JWT in the handshake so Java's WebSocketSecurityInterceptor lets us in
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },

      debug: () => {
        // console.log('[STOMP] ' + str);
      },

      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    // 3. The Connection Lifecycle
    client.onConnect = () => {
      console.log('🚀 [WEBSOCKET] Connected to Avenra Real-Time Highway.');

      const subscriptionTopic = `/topic/organization/${orgId}`;

      client.subscribe(subscriptionTopic, (message) => {
        if (message.body) {
          const payload = JSON.parse(message.body);
          console.log('📥 [WEBSOCKET] Payload Received:', payload);
          // Always call the latest callback via the ref — never stale
          if (onMessageRef.current) onMessageRef.current(payload);
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

    // 5. Teardown — only runs when DashboardLayout unmounts (user fully leaves the authenticated area)
    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate();
        clientRef.current = null;
        console.log('🛑 [WEBSOCKET] Connection severed gracefully.');
      }
    };
  // Empty deps: connect once on mount, disconnect on unmount. The callback ref
  // handles keeping the handler current without triggering reconnects.
  }, []); // eslint-disable-line react-hooks/exhaustive-deps
}
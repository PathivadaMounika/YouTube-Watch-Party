import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = 'http://localhost:8080/ws';

/**
 * Connects to the room's WebSocket topic and keeps `room` in sync live -
 * no more refreshing to see other participants join or leave.
 *
 * roomId / userId: identifies which room's topic to subscribe to and who
 * "we" are when sending the join frame (userId comes from the earlier
 * REST create/join call - see api/identity.js).
 */
export function useRoomSocket(roomId, userId) {
  const [room, setRoom] = useState(null);
  const [connected, setConnected] = useState(false);
  const [notification, setNotification] = useState(null);
  const [removed, setRemoved] = useState(false);
  const [chatEvent, setChatEvent] = useState(null);
  const clientRef = useRef(null);

  useEffect(() => {
    if (!roomId || !userId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true);

        client.subscribe(`/topic/room/${roomId}`, (message) => {
          const parsed = JSON.parse(message.body);
          setRoom(parsed);

          // The room broadcast is the one channel we know reaches every
          // client reliably (including our own, since it goes to
          // everyone subscribed to the topic). Rather than depend on a
          // private per-session message to know we were kicked - which
          // requires Spring's anonymous-session user-destination
          // machinery to work perfectly - we just check the broadcast
          // we're already getting: if we're no longer in the
          // participant list, we were removed.
          const stillHere = parsed.participants.some((p) => p.userId === userId);
          if (!stillHere) {
            setRemoved(true);
          }
        });

        // Chat messages and reactions - separate topic from room state.
        client.subscribe(`/topic/room/${roomId}/chat`, (message) => {
          setChatEvent({ ...JSON.parse(message.body), _receivedAt: Date.now() });
        });

        // Private channel for "action denied" toasts. (Room removal is
        // detected via the public broadcast above instead, since that's
        // proven reliable even without an authenticated session.)
        client.subscribe('/user/queue/notifications', (message) => {
          setNotification(JSON.parse(message.body));
        });

        client.publish({
          destination: `/app/room/${roomId}/join`,
          body: JSON.stringify({ userId }),
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers?.message, frame.body);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [roomId, userId]);

  /**
   * Sends a playback action (play/pause/seek/change-video) to the server.
   * `action` maps directly onto the backend's @MessageMapping path suffix.
   */
  function sendAction(action, payload) {
    const client = clientRef.current;
    if (!client || !client.connected) return;

    client.publish({
      destination: `/app/room/${roomId}/${action}`,
      body: JSON.stringify(payload),
    });
  }

  return {
    room,
    connected,
    sendAction,
    notification,
    clearNotification: () => setNotification(null),
    removed,
    chatEvent,
  };
}

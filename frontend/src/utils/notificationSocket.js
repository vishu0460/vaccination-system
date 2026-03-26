import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { getAccessToken } from "./auth";

let activeClient = null;

const resolveSocketUrl = () => {
  if (typeof window === "undefined") {
    return "/ws";
  }
  return `${window.location.origin}/ws`;
};

export function connectNotificationSocket({ onNotification, onUnreadCount, onConnectError } = {}) {
  const token = getAccessToken();
  if (!token) {
    return () => {};
  }

  const client = new Client({
    webSocketFactory: () => new SockJS(resolveSocketUrl()),
    connectHeaders: {
      Authorization: `Bearer ${token}`
    },
    debug: () => {},
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe("/user/queue/notifications", (message) => {
        if (!message.body) return;
        try {
          onNotification?.(JSON.parse(message.body));
        } catch {
          // ignore malformed payload
        }
      });

      client.subscribe("/user/queue/notifications/unread", (message) => {
        if (!message.body) return;
        try {
          onUnreadCount?.(JSON.parse(message.body));
        } catch {
          // ignore malformed payload
        }
      });
    },
    onStompError: (frame) => {
      onConnectError?.(frame);
    },
    onWebSocketError: (event) => {
      onConnectError?.(event);
    }
  });

  client.activate();
  activeClient = client;

  return () => {
    if (activeClient === client) {
      activeClient = null;
    }
    client.deactivate();
  };
}

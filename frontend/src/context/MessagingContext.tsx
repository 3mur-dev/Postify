import {
  Client,
  type IMessage,
} from "@stomp/stompjs";
import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import {
  useQueryClient,
  type InfiniteData,
} from "@tanstack/react-query";

import type {
  ConversationSummary,
  DirectMessage,
  NotificationResponse,
  SpringPage,
} from "../api/types";
import { getAccessToken } from "../lib/auth";
import {
  conversationMessagesQueryKey,
  conversationsQueryKey,
} from "../hooks/useConversations";
import { MessagingContext } from "./messagingContextStore";
import type {
  MessagingConnectionState,
  ParticipantPresence,
  PendingDirectMessage,
} from "./messagingTypes";

const MAX_MESSAGE_LENGTH = 10_000;

function isDirectMessage(value: unknown): value is DirectMessage {
  if (!value || typeof value !== "object") {
    return false;
  }
  const message = value as Record<string, unknown>;
  return (
    Number.isFinite(message.id) &&
    typeof message.conversationPublicId === "string" &&
    typeof message.senderPublicId === "string" &&
    typeof message.senderUsername === "string" &&
    (typeof message.senderAvatarUrl === "string" ||
      message.senderAvatarUrl === null) &&
    typeof message.content === "string" &&
    (typeof message.clientMessageId === "string" ||
      message.clientMessageId === null) &&
    typeof message.createdAt === "string" &&
    typeof message.read === "boolean" &&
    (typeof message.readAt === "string" || message.readAt === null)
  );
}

function isNotificationResponse(value: unknown): value is NotificationResponse {
  if (!value || typeof value !== "object") {
    return false;
  }
  const notification = value as Record<string, unknown>;
  return (
    typeof notification.publicId === "string" &&
    typeof notification.message === "string" &&
    typeof notification.type === "string" &&
    ["LIKE", "COMMENT", "FOLLOW", "SYSTEM"].includes(notification.type) &&
    (typeof notification.actorPublicId === "string" ||
      notification.actorPublicId === null) &&
    (typeof notification.actorUsername === "string" ||
      notification.actorUsername === null) &&
    (typeof notification.postPublicId === "string" ||
      notification.postPublicId === null) &&
    (typeof notification.commentId === "number" ||
      notification.commentId === null) &&
    typeof notification.isRead === "boolean" &&
    typeof notification.createdAt === "string"
  );
}

function isPresenceResponse(value: unknown): value is ParticipantPresence {
  if (!value || typeof value !== "object") {
    return false;
  }
  const presence = value as Record<string, unknown>;
  return (
    typeof presence.conversationPublicId === "string" &&
    typeof presence.participantPublicId === "string" &&
    typeof presence.online === "boolean"
  );
}

export function MessagingProvider({
  authenticated,
  children,
}: {
  authenticated: boolean;
  children: ReactNode;
}) {
  const queryClient = useQueryClient();
  const clientRef = useRef<Client | null>(null);
  const [connectionState, setConnectionState] =
    useState<MessagingConnectionState>(() => {
      if (!authenticated) {
        return "disconnected";
      }
      return getAccessToken() ? "connecting" : "error";
    });
  const [connectionError, setConnectionError] = useState<string | null>(() =>
    authenticated && !getAccessToken()
      ? "Your session is unavailable. Sign in again."
      : null,
  );
  const [pendingMessages, setPendingMessages] = useState<
    PendingDirectMessage[]
  >([]);
  const [participantPresence, setParticipantPresence] = useState<
    Record<string, boolean>
  >({});

  const resolvePendingMessage = useCallback((clientMessageId: string) => {
    setPendingMessages((current) =>
      current.filter((message) => message.clientMessageId !== clientMessageId),
    );
  }, []);

  const handleIncomingMessage = useCallback(
    (message: IMessage) => {
      let parsed: unknown;
      try {
        parsed = JSON.parse(message.body) as unknown;
      } catch {
        setConnectionError("A message event from the server was invalid.");
        return;
      }
      if (!isDirectMessage(parsed)) {
        setConnectionError("A message event from the server was invalid.");
        return;
      }
      const incoming = parsed;

      const messageKey = conversationMessagesQueryKey(
        incoming.conversationPublicId,
      );
      queryClient.setQueryData<
        InfiniteData<SpringPage<DirectMessage>, number>
      >(messageKey, (current) => {
        if (!current || current.pages.length === 0) {
          return current;
        }

        const alreadyPresent = current.pages.some((page) =>
          page.content.some(
            (item) =>
              item.id === incoming.id ||
              (!!incoming.clientMessageId &&
                item.clientMessageId === incoming.clientMessageId),
          ),
        );
        if (alreadyPresent) {
          return current;
        }

        const [newestPage] = current.pages;
        const content = [...newestPage.content, incoming].sort(
          (left, right) =>
            right.createdAt.localeCompare(left.createdAt) ||
            right.id - left.id,
        );
        const totalElements = newestPage.totalElements + 1;
        const totalPages = Math.ceil(totalElements / newestPage.size);

        return {
          ...current,
          pages: current.pages.map((page, index) => ({
            ...page,
            content: index === 0 ? content : page.content,
            totalElements,
            totalPages,
            last: page.number >= totalPages - 1,
          })),
        };
      });

      queryClient.setQueryData<ConversationSummary[]>(
        conversationsQueryKey,
        (conversations) => {
          if (!conversations) {
            return conversations;
          }
          return conversations
            .map((conversation) => {
              if (conversation.publicId !== incoming.conversationPublicId) {
                return conversation;
              }
              const currentLatestAt =
                conversation.latestMessage?.createdAt ?? conversation.updatedAt;
              if (incoming.createdAt.localeCompare(currentLatestAt) < 0) {
                return conversation;
              }
              return {
                    ...conversation,
                    latestMessage: {
                      content: incoming.content,
                      senderUsername: incoming.senderUsername,
                      createdAt: incoming.createdAt,
                    },
                    updatedAt: incoming.createdAt,
                  };
            })
            .sort((left, right) =>
              right.updatedAt.localeCompare(left.updatedAt),
            );
        },
      );

      if (incoming.clientMessageId) {
        resolvePendingMessage(incoming.clientMessageId);
      }
      void queryClient.invalidateQueries({ queryKey: conversationsQueryKey });
    },
    [queryClient, resolvePendingMessage],
  );

  useEffect(() => {
    const token = authenticated ? getAccessToken() : null;
    if (!token) {
      queryClient.removeQueries({ queryKey: conversationsQueryKey });
      queryClient.removeQueries({ queryKey: ["conversationMessages"] });
      return;
    }

    let disposed = false;
    const setState = (state: MessagingConnectionState) => {
      if (!disposed) {
        setConnectionState(state);
      }
    };
    const apiUrl = new URL(
      import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api",
    );
    const socketUrl = new URL("/ws", apiUrl.origin);
    socketUrl.protocol = socketUrl.protocol === "https:" ? "wss:" : "ws:";

    const client = new Client({
      brokerURL: socketUrl.toString(),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5_000,
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      onConnect: () => {
        setState("connected");
        if (!disposed) {
          setConnectionError(null);
        }
        client.subscribe("/user/queue/messages", handleIncomingMessage);
        client.subscribe("/user/queue/presence", (frame) => {
          let parsed: unknown;
          try {
            parsed = JSON.parse(frame.body) as unknown;
          } catch {
            console.error("A presence event could not be parsed.");
            return;
          }
          if (!isPresenceResponse(parsed)) {
            console.error("A presence event had an invalid shape.");
            return;
          }
          setParticipantPresence((current) => ({
            ...current,
            [parsed.participantPublicId]: parsed.online,
          }));
        });
        client.subscribe("/user/queue/notifications", (frame) => {
          let parsed: unknown;
          try {
            parsed = JSON.parse(frame.body) as unknown;
          } catch {
            console.error("A notification event could not be parsed.");
            return;
          }
          if (!isNotificationResponse(parsed)) {
            console.error("A notification event had an invalid shape.");
            return;
          }
          const notification = parsed;
          queryClient.setQueryData<NotificationResponse[]>(
            ["notifications"],
            (current) => [
              notification,
              ...(current ?? []).filter(
                (item) => item.publicId !== notification.publicId,
              ),
            ],
          );
        });
        void queryClient.invalidateQueries({
          queryKey: conversationsQueryKey,
        });
        void queryClient.invalidateQueries({
          queryKey: ["conversationMessages"],
        });
      },
      onDisconnect: () => setState("disconnected"),
      onWebSocketClose: () => {
        if (disposed) {
          return;
        }
        setState("reconnecting");
        setParticipantPresence({});
        setPendingMessages((current) =>
          current.map((message) =>
            message.status === "pending"
              ? {
                  ...message,
                  status: "failed",
                  error:
                    "Connection lost before confirmation. Retry safely when reconnected.",
                }
              : message,
          ),
        );
      },
      onWebSocketError: () => setState("reconnecting"),
      onStompError: (frame) => {
        if (disposed) {
          return;
        }
        const detail = frame?.body ?? "The messaging connection was rejected.";
        setState("error");
        setConnectionError(detail);
        setPendingMessages((current) =>
          current.map((message) =>
            message.status === "pending"
              ? {
                  ...message,
                  status: "failed",
                  error:
                    "The server couldn’t confirm this message. Retry safely.",
                }
              : message,
          ),
        );
      },
    });

    clientRef.current = client;
    client.activate();

    return () => {
      disposed = true;
      if (clientRef.current === client) {
        clientRef.current = null;
      }
      void client.deactivate();
    };
  }, [authenticated, handleIncomingMessage, queryClient]);

  const publishMessage = useCallback((message: PendingDirectMessage) => {
    const client = clientRef.current;
    if (!client?.connected) {
      setPendingMessages((current) =>
        current.map((item) =>
          item.clientMessageId === message.clientMessageId
            ? {
                ...item,
                status: "failed",
                error: "You are offline. Retry when the connection returns.",
              }
            : item,
        ),
      );
      return;
    }

    try {
      client.publish({
        destination: "/app/chat.send",
        body: JSON.stringify({
          conversationPublicId: message.conversationPublicId,
          content: message.content,
          clientMessageId: message.clientMessageId,
        }),
      });
      setPendingMessages((current) =>
        current.map((item) =>
          item.clientMessageId === message.clientMessageId
            ? { ...item, status: "pending", error: undefined }
            : item,
        ),
      );
    } catch {
      setPendingMessages((current) =>
        current.map((item) =>
          item.clientMessageId === message.clientMessageId
            ? {
                ...item,
                status: "failed",
                error: "The message could not be sent. Retry safely.",
              }
            : item,
        ),
      );
    }
  }, []);

  const sendMessage = useCallback(
    (conversationPublicId: string, content: string) => {
      if (!content.trim() || content.length > MAX_MESSAGE_LENGTH) {
        throw new Error("Messages must contain 1–10,000 characters.");
      }

      const pending: PendingDirectMessage = {
        clientMessageId: crypto.randomUUID(),
        conversationPublicId,
        content,
        createdAt: new Date().toISOString(),
        status: "pending",
      };
      setPendingMessages((current) => [...current, pending]);
      publishMessage(pending);
      return pending.clientMessageId;
    },
    [publishMessage],
  );

  const requestPresence = useCallback((conversationPublicId: string) => {
    const client = clientRef.current;
    if (!client?.connected) {
      return;
    }
    try {
      client.publish({
        destination: "/app/chat.presence",
        body: JSON.stringify({ conversationPublicId }),
      });
    } catch {
      setConnectionError("Couldn’t check the other participant’s status.");
    }
  }, []);

  const retryMessage = useCallback(
    (clientMessageId: string) => {
      const pending = pendingMessages.find(
        (message) => message.clientMessageId === clientMessageId,
      );
      if (pending) {
        publishMessage(pending);
      }
    },
    [pendingMessages, publishMessage],
  );

  return (
    <MessagingContext.Provider
      value={{
        connectionState,
        connectionError,
        pendingMessages,
        participantPresence,
        sendMessage,
        requestPresence,
        retryMessage,
        resolvePendingMessage,
      }}
    >
      {children}
    </MessagingContext.Provider>
  );
}

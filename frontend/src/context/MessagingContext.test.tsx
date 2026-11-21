import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type {
  ConversationSummary,
  DirectMessage,
  SpringPage,
} from "../api/types";
import { conversationMessagesQueryKey, conversationsQueryKey } from "../hooks/useConversations";
import { MessagingProvider } from "./MessagingContext";
import { useMessaging } from "../hooks/useMessaging";

type FakeClient = {
  connected: boolean;
  options: Record<string, unknown>;
  activate: ReturnType<typeof vi.fn>;
  deactivate: ReturnType<typeof vi.fn>;
  subscribe: ReturnType<typeof vi.fn>;
  publish: ReturnType<typeof vi.fn>;
};

const { clients } = vi.hoisted(() => ({
  clients: [] as FakeClient[],
}));

vi.mock("@stomp/stompjs", () => ({
  Client: class {
    connected = false;
    options: Record<string, unknown>;
    activate = vi.fn();
    deactivate = vi.fn(async () => undefined);
    subscribe = vi.fn();
    publish = vi.fn();

    constructor(options: Record<string, unknown>) {
      this.options = options;
      clients.push(this as unknown as FakeClient);
    }
  },
}));

const conversationId = "thread-1";
const now = "2025-03-01T12:00:00.000Z";

function createDirectMessage(overrides: Partial<DirectMessage> = {}): DirectMessage {
  return {
    id: 1,
    conversationPublicId: conversationId,
    senderPublicId: "user-2",
    senderUsername: "sam",
    senderAvatarUrl: null,
    content: "Hello",
    clientMessageId: null,
    createdAt: now,
    read: false,
    readAt: null,
    ...overrides,
  };
}

function createPage(content: DirectMessage[]): SpringPage<DirectMessage> {
  return {
    content,
    totalPages: 1,
    totalElements: content.length,
    size: 20,
    number: 0,
    last: true,
    first: true,
    empty: content.length === 0,
  };
}

function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: Infinity },
      mutations: { retry: false },
    },
  });
}

function createWrapper(queryClient: QueryClient) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <MessagingProvider authenticated>{children}</MessagingProvider>
      </QueryClientProvider>
    );
  };
}

function connect(client: FakeClient) {
  client.connected = true;
  (client.options.onConnect as () => void)();
}

function subscription(client: FakeClient, destination: string) {
  const call = client.subscribe.mock.calls.find(
    ([subscribedDestination]) => subscribedDestination === destination,
  );
  if (!call) {
    throw new Error(`Missing subscription: ${destination}`);
  }
  return call[1] as (frame: { body: string }) => void;
}

describe("MessagingProvider", () => {
  beforeEach(() => {
    clients.length = 0;
    localStorage.clear();
    localStorage.setItem("token", "access-token");
    vi.clearAllMocks();
  });

  it("records offline send failures and retries after the socket reconnects", () => {
    const queryClient = createQueryClient();
    const { result, unmount } = renderHook(() => useMessaging(), {
      wrapper: createWrapper(queryClient),
    });
    const client = clients[0];

    expect(result.current.connectionState).toBe("connecting");
    let clientMessageId = "";
    act(() => {
      clientMessageId = result.current.sendMessage(conversationId, "Hello");
    });
    expect(result.current.pendingMessages).toMatchObject([
      {
        clientMessageId,
        conversationPublicId: conversationId,
        content: "Hello",
        status: "failed",
        error: "You are offline. Retry when the connection returns.",
      },
    ]);

    act(() => connect(client));
    expect(result.current.connectionState).toBe("connected");
    act(() => result.current.retryMessage(clientMessageId));

    expect(client.publish).toHaveBeenCalledWith({
      destination: "/app/chat.send",
      body: JSON.stringify({
        conversationPublicId: conversationId,
        content: "Hello",
        clientMessageId,
      }),
    });
    expect(result.current.pendingMessages[0]).toMatchObject({
      clientMessageId,
      status: "pending",
      error: undefined,
    });
    unmount();
  });

  it("marks unconfirmed messages failed on disconnect and allows a safe retry", () => {
    const queryClient = createQueryClient();
    const { result, unmount } = renderHook(() => useMessaging(), {
      wrapper: createWrapper(queryClient),
    });
    const client = clients[0];
    act(() => connect(client));

    let clientMessageId = "";
    act(() => {
      clientMessageId = result.current.sendMessage(conversationId, "Retry me");
    });
    expect(result.current.pendingMessages[0].status).toBe("pending");
    act(() => (client.options.onWebSocketClose as () => void)());

    expect(result.current.connectionState).toBe("reconnecting");
    expect(result.current.pendingMessages[0]).toMatchObject({
      status: "failed",
      error: "Connection lost before confirmation. Retry safely when reconnected.",
    });

    act(() => connect(client));
    act(() => result.current.retryMessage(clientMessageId));
    expect(client.publish).toHaveBeenCalledTimes(2);
    expect(result.current.pendingMessages[0].status).toBe("pending");
    unmount();
  });

  it("surfaces broker rejections and fails pending messages", () => {
    const queryClient = createQueryClient();
    const { result, unmount } = renderHook(() => useMessaging(), {
      wrapper: createWrapper(queryClient),
    });
    const client = clients[0];
    act(() => connect(client));
    act(() => {
      result.current.sendMessage(conversationId, "Awaiting broker");
    });
    act(() =>
      (client.options.onStompError as () => void)(),
    );

    expect(result.current.connectionState).toBe("error");
    expect(result.current.connectionError).toBe(
      "The messaging connection was rejected.",
    );
    expect(result.current.pendingMessages[0]).toMatchObject({
      status: "failed",
      error: "The server couldn’t confirm this message. Retry safely.",
    });
    unmount();
  });

  it("reports publish failures and rejects empty or oversized messages", () => {
    const queryClient = createQueryClient();
    const { result, unmount } = renderHook(() => useMessaging(), {
      wrapper: createWrapper(queryClient),
    });
    const client = clients[0];
    act(() => connect(client));
    client.publish.mockImplementationOnce(() => {
      throw new Error("Socket unavailable");
    });

    act(() => {
      result.current.sendMessage(conversationId, "Hello");
    });
    expect(result.current.pendingMessages[0]).toMatchObject({
      status: "failed",
      error: "The message could not be sent. Retry safely.",
    });
    expect(() => result.current.sendMessage(conversationId, "  ")).toThrow(
      "Messages must contain 1–10,000 characters.",
    );
    expect(() =>
      result.current.sendMessage(conversationId, "x".repeat(10_001)),
    ).toThrow("Messages must contain 1–10,000 characters.");
    unmount();
  });

  it("updates only the matching conversation and deduplicates incoming messages", () => {
    const queryClient = createQueryClient();
    const original = createDirectMessage({ id: 2, createdAt: "2025-03-01T11:00:00.000Z" });
    queryClient.setQueryData(conversationMessagesQueryKey(conversationId), {
      pages: [createPage([original])],
      pageParams: [0],
    });
    queryClient.setQueryData(conversationMessagesQueryKey("thread-2"), {
      pages: [createPage([])],
      pageParams: [0],
    });
    const conversation: ConversationSummary = {
      publicId: conversationId,
      participant: { publicId: "user-2", username: "sam", avatarUrl: null },
      latestMessage: null,
      updatedAt: "2025-03-01T11:00:00.000Z",
    };
    queryClient.setQueryData(conversationsQueryKey, [conversation]);

    const { result, unmount } = renderHook(() => useMessaging(), {
      wrapper: createWrapper(queryClient),
    });
    const client = clients[0];
    act(() => connect(client));
    const incoming = createDirectMessage({ id: 3, content: "Newest" });
    const onMessage = subscription(client, "/user/queue/messages");

    act(() => onMessage({ body: JSON.stringify(incoming) }));
    act(() => onMessage({ body: JSON.stringify(incoming) }));

    const messages = queryClient.getQueryData<{
      pages: Array<SpringPage<DirectMessage>>;
    }>(conversationMessagesQueryKey(conversationId));
    expect(messages?.pages[0].content.map((message) => message.id)).toEqual([
      3,
      2,
    ]);
    expect(messages?.pages[0].totalElements).toBe(2);
    expect(
      queryClient.getQueryData<{ pages: Array<SpringPage<DirectMessage>> }>(
        conversationMessagesQueryKey("thread-2"),
      )?.pages[0].content,
    ).toEqual([]);
    expect(result.current.pendingMessages).toEqual([]);
    expect(queryClient.getQueryData<ConversationSummary[]>(conversationsQueryKey)?.[0])
      .toMatchObject({
        latestMessage: {
          content: "Newest",
          senderUsername: "sam",
        },
      });
    unmount();
  });

  it("resolves optimistic messages by client id and surfaces invalid server events", () => {
    const queryClient = createQueryClient();
    queryClient.setQueryData(conversationMessagesQueryKey(conversationId), {
      pages: [createPage([])],
      pageParams: [0],
    });
    const { result, unmount } = renderHook(() => useMessaging(), {
      wrapper: createWrapper(queryClient),
    });
    const client = clients[0];
    act(() => connect(client));

    const clientMessageId = result.current.sendMessage(conversationId, "Mine");
    act(() =>
      subscription(client, "/user/queue/messages")({
        body: JSON.stringify(
          createDirectMessage({ id: 4, clientMessageId, senderUsername: "ava" }),
        ),
      }),
    );
    expect(result.current.pendingMessages).toEqual([]);

    act(() =>
      subscription(client, "/user/queue/messages")({ body: "{invalid json" }),
    );
    expect(result.current.connectionError).toBe(
      "A message event from the server was invalid.",
    );
    unmount();
  });
});

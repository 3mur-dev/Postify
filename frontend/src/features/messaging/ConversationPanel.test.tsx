import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { ConversationSummary, DirectMessage, SpringPage } from "../../api/types";
import { markMessagesAsRead } from "../../api/conversationApi";
import { useConversationMessages } from "../../hooks/useConversations";
import { useMessaging } from "../../hooks/useMessaging";
import ConversationPanel from "./ConversationPanel";

vi.mock("../../api/conversationApi", () => ({
  markMessagesAsRead: vi.fn(),
}));
vi.mock("../../hooks/useConversations", () => ({
  useConversationMessages: vi.fn(),
}));
vi.mock("../../hooks/useMessaging", () => ({
  useMessaging: vi.fn(),
}));
vi.mock("./MessageComposer", () => ({
  default: () => <div>Composer</div>,
}));

const mockMarkMessagesAsRead = vi.mocked(markMessagesAsRead);
const mockUseConversationMessages = vi.mocked(useConversationMessages);
const mockUseMessaging = vi.mocked(useMessaging);

function createMessage(
  conversationPublicId: string,
  overrides: Partial<DirectMessage> = {},
): DirectMessage {
  return {
    id: 1,
    conversationPublicId,
    senderPublicId: "user-2",
    senderUsername: "sam",
    senderAvatarUrl: null,
    content: "Incoming message",
    clientMessageId: null,
    createdAt: "2025-03-01T12:00:00.000Z",
    read: false,
    readAt: null,
    ...overrides,
  };
}

function createPage(messages: DirectMessage[]): SpringPage<DirectMessage> {
  return {
    content: messages,
    totalPages: 1,
    totalElements: messages.length,
    size: 20,
    number: 0,
    last: true,
    first: true,
    empty: messages.length === 0,
  };
}

function createConversation(publicId: string): ConversationSummary {
  return {
    publicId,
    participant: { publicId: "user-2", username: "sam", avatarUrl: null },
    latestMessage: null,
    updatedAt: "2025-03-01T12:00:00.000Z",
  };
}

function messagesResult(messages: DirectMessage[]) {
  return {
    data: { pages: [createPage(messages)], pageParams: [0] },
    isLoading: false,
    isError: false,
    isFetchingNextPage: false,
    isFetchNextPageError: false,
    hasNextPage: false,
    fetchNextPage: vi.fn(),
    refetch: vi.fn(),
  } as never;
}

function renderPanel(conversationPublicId: string, messages: DirectMessage[]) {
  mockUseConversationMessages.mockReturnValue(messagesResult(messages));
  return render(
    <MemoryRouter>
      <ConversationPanel
        conversationPublicId={conversationPublicId}
        currentUsername="ava"
        conversation={createConversation(conversationPublicId)}
      />
    </MemoryRouter>,
  );
}

describe("ConversationPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockMarkMessagesAsRead.mockResolvedValue(undefined);
    mockUseMessaging.mockReturnValue({
      connectionState: "connected",
      connectionError: null,
      pendingMessages: [],
      participantPresence: {},
      sendMessage: vi.fn(),
      requestPresence: vi.fn(),
      retryMessage: vi.fn(),
      resolvePendingMessage: vi.fn(),
    });
  });

  it("marks unread incoming messages as read and shows outgoing read receipts", async () => {
    renderPanel("thread-1", [
      createMessage("thread-1", { id: 10 }),
      createMessage("thread-1", {
        id: 11,
        senderPublicId: "user-1",
        senderUsername: "ava",
        content: "Sent message",
        read: true,
        readAt: "2025-03-01T12:01:00.000Z",
      }),
    ]);

    await waitFor(() =>
      expect(mockMarkMessagesAsRead).toHaveBeenCalledWith("thread-1"),
    );
    expect(screen.getByText("Sent message")).toBeInTheDocument();
    expect(screen.getByText(/Read/)).toBeInTheDocument();
  });

  it("tracks read receipts separately when the selected conversation changes", async () => {
    const sameIdUnread = createMessage("thread-1", { id: 17 });
    const { rerender } = renderPanel("thread-1", [sameIdUnread]);
    await waitFor(() =>
      expect(mockMarkMessagesAsRead).toHaveBeenCalledTimes(1),
    );

    mockUseConversationMessages.mockReturnValue(
      messagesResult([createMessage("thread-2", { id: 17 })]),
    );
    rerender(
      <MemoryRouter>
        <ConversationPanel
          conversationPublicId="thread-2"
          currentUsername="ava"
          conversation={createConversation("thread-2")}
        />
      </MemoryRouter>,
    );

    await waitFor(() =>
      expect(mockMarkMessagesAsRead).toHaveBeenCalledTimes(2),
    );
    expect(mockMarkMessagesAsRead).toHaveBeenNthCalledWith(1, "thread-1");
    expect(mockMarkMessagesAsRead).toHaveBeenNthCalledWith(2, "thread-2");
  });

  it("requests presence when connected and retries failed history loads", () => {
    const requestPresence = vi.fn();
    const refetch = vi.fn();
    mockUseMessaging.mockReturnValue({
      connectionState: "connected",
      connectionError: "Connection retrying",
      pendingMessages: [],
      participantPresence: { "user-2": true },
      sendMessage: vi.fn(),
      requestPresence,
      retryMessage: vi.fn(),
      resolvePendingMessage: vi.fn(),
    });
    mockUseConversationMessages.mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      isFetchingNextPage: false,
      isFetchNextPageError: false,
      hasNextPage: false,
      fetchNextPage: vi.fn(),
      refetch,
    } as never);
    render(
      <MemoryRouter>
        <ConversationPanel
          conversationPublicId="thread-1"
          currentUsername="ava"
          conversation={createConversation("thread-1")}
        />
      </MemoryRouter>,
    );

    expect(requestPresence).toHaveBeenCalledWith("thread-1");
    expect(screen.getByText("Online")).toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Connection retrying");
    fireEvent.click(screen.getByRole("button", { name: "Try again" }));
    expect(refetch).toHaveBeenCalledOnce();
  });
});

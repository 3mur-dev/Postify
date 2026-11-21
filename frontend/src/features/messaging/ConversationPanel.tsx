import {
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
} from "react";
import {
  ArrowLeft,
  Check,
  CheckCheck,
  CircleAlert,
  LoaderCircle,
  MessageCircle,
  RefreshCw,
} from "lucide-react";
import { useNavigate } from "react-router-dom";

import type {
  ConversationSummary,
  DirectMessage,
} from "../../api/types";
import Avatar from "../home/components/Avatar";
import type { PendingDirectMessage } from "../../context/messagingTypes";
import { useMessaging } from "../../hooks/useMessaging";
import { useConversationMessages } from "../../hooks/useConversations";
import MessageComposer from "./MessageComposer";
import { markMessagesAsRead } from "../../api/conversationApi";

interface ConversationPanelProps {
  conversationPublicId: string;
  currentUsername: string;
  conversation?: ConversationSummary;
}

interface DisplayMessage {
  key: string;
  content: string;
  senderUsername: string;
  senderAvatarUrl: string | null;
  createdAt: string;
  clientMessageId: string | null;
  read?: boolean;
  readAt?: string | null;
  status?: PendingDirectMessage["status"];
  error?: string;
  retry?: () => void;
}

function formatMessageTime(value: string) {
  return new Date(value).toLocaleTimeString([], {
    hour: "numeric",
    minute: "2-digit",
  });
}

function formatDay(value: string) {
  const date = new Date(value);
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);

  if (date.toDateString() === today.toDateString()) {
    return "Today";
  }
  if (date.toDateString() === yesterday.toDateString()) {
    return "Yesterday";
  }

  return date.toLocaleDateString([], {
    weekday: "long",
    month: "long",
    day: "numeric",
  });
}

export default function ConversationPanel({
  conversationPublicId,
  currentUsername,
  conversation,
}: ConversationPanelProps) {
  const navigate = useNavigate();
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const olderScrollAnchorRef = useRef<{ height: number; top: number } | null>(
    null,
  );
  const initializedScrollRef = useRef(false);
  const shouldFollowRef = useRef(true);
  const lastMarkedReadMessageRef = useRef<string | null>(null);
  const {
    connectionState,
    connectionError,
    pendingMessages,
    participantPresence,
    requestPresence,
    retryMessage,
    resolvePendingMessage,
  } = useMessaging();
  const {
    data,
    isLoading,
    isError,
    isFetchingNextPage,
    isFetchNextPageError,
    hasNextPage,
    fetchNextPage,
    refetch,
  } = useConversationMessages(conversationPublicId);

  const serverMessages = useMemo(() => {
    const unique = new Map<number, DirectMessage>();
    for (const message of data?.pages.flatMap((page) => page.content) ?? []) {
      unique.set(message.id, message);
    }
    return [...unique.values()].sort(
      (left, right) =>
        left.createdAt.localeCompare(right.createdAt) || left.id - right.id,
    );
  }, [data]);

  const confirmedClientMessageIds = useMemo(
    () =>
      new Set(
        serverMessages
          .map((message) => message.clientMessageId)
          .filter((id): id is string => !!id),
      ),
    [serverMessages],
  );

  useEffect(() => {
    confirmedClientMessageIds.forEach(resolvePendingMessage);
  }, [confirmedClientMessageIds, resolvePendingMessage]);

  useEffect(() => {
    const newestUnreadIncomingMessage = serverMessages.findLast(
      (message) =>
        message.senderUsername !== currentUsername && !message.read,
    );
    if (
      !conversationPublicId ||
      !newestUnreadIncomingMessage ||
      lastMarkedReadMessageRef.current ===
        `${conversationPublicId}:${newestUnreadIncomingMessage.id}`
    ) {
      return;
    }

    lastMarkedReadMessageRef.current =
      `${conversationPublicId}:${newestUnreadIncomingMessage.id}`;
    markMessagesAsRead(conversationPublicId).catch((error: unknown) => {
      console.error("Could not mark conversation messages as read.", error);
    });
  }, [conversationPublicId, currentUsername, serverMessages]);

  const pendingForConversation = useMemo(
    () =>
      pendingMessages.filter(
        (message) =>
          message.conversationPublicId === conversationPublicId &&
          !confirmedClientMessageIds.has(message.clientMessageId),
      ),
    [confirmedClientMessageIds, conversationPublicId, pendingMessages],
  );

  const displayMessages = useMemo<DisplayMessage[]>(() => {
    const persisted: DisplayMessage[] = serverMessages.map((message) => ({
      key: `message-${message.id}`,
      content: message.content,
      senderUsername: message.senderUsername,
      senderAvatarUrl: message.senderAvatarUrl,
      createdAt: message.createdAt,
      clientMessageId: message.clientMessageId,
      read: message.read,
      readAt: message.readAt,
    }));
    const pending: DisplayMessage[] = pendingForConversation.map((message) => ({
      key: `pending-${message.clientMessageId}`,
      content: message.content,
      senderUsername: currentUsername,
      senderAvatarUrl: null,
      createdAt: message.createdAt,
      clientMessageId: message.clientMessageId,
      status: message.status,
      error: message.error,
      retry: () => retryMessage(message.clientMessageId),
    }));
    const chronologicalPending = pending.sort((left, right) =>
      left.createdAt.localeCompare(right.createdAt),
    );
    return [...persisted, ...chronologicalPending];
  }, [currentUsername, pendingForConversation, retryMessage, serverMessages]);

  useLayoutEffect(() => {
    const container = scrollContainerRef.current;
    if (!container || !data) {
      return;
    }

    if (olderScrollAnchorRef.current && !isFetchingNextPage) {
      const anchor = olderScrollAnchorRef.current;
      container.scrollTop =
        container.scrollHeight - anchor.height + anchor.top;
      olderScrollAnchorRef.current = null;
      shouldFollowRef.current = false;
      return;
    }

    if (isFetchingNextPage) {
      return;
    }

    if (!initializedScrollRef.current) {
      container.scrollTop = container.scrollHeight;
      initializedScrollRef.current = true;
      shouldFollowRef.current = true;
      return;
    }

    if (shouldFollowRef.current) {
      container.scrollTop = container.scrollHeight;
    }
  }, [data, displayMessages, isFetchingNextPage]);

  const loadOlderMessages = () => {
    const container = scrollContainerRef.current;
    if (!container || !hasNextPage || isFetchingNextPage) {
      return;
    }
    olderScrollAnchorRef.current = {
      height: container.scrollHeight,
      top: container.scrollTop,
    };
    void fetchNextPage();
  };

  const participant = conversation?.participant;
  const participantPublicId = participant?.publicId;
  const isParticipantOnline = participant
    ? participantPresence[participant.publicId]
    : undefined;

  useEffect(() => {
    if (
      connectionState === "connected" &&
      conversationPublicId &&
      participantPublicId
    ) {
      requestPresence(conversationPublicId);
    }
  }, [
    connectionState,
    conversationPublicId,
    participantPublicId,
    requestPresence,
  ]);

  return (
    <section
      aria-label={
        participant
          ? `Conversation with ${participant.username}`
          : "Conversation"
      }
      className="flex min-h-0 flex-1 flex-col bg-slate-950"
    >
      <header className="sticky top-0 z-10 flex min-h-[4.5rem] shrink-0 items-center gap-3 border-b border-slate-800 bg-slate-950/95 px-3 py-3 backdrop-blur sm:px-5">
        <button
          type="button"
          onClick={() => navigate("/messages")}
          aria-label="Back to conversations"
          className="flex size-9 shrink-0 items-center justify-center rounded-full text-slate-400 transition hover:bg-slate-900 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-fuchsia-500 md:hidden"
        >
          <ArrowLeft aria-hidden="true" className="size-5" />
        </button>
        {participant ? (
          <Avatar
            imageUrl={participant.avatarUrl}
            name={participant.username}
            size="md"
            className="shrink-0 border border-slate-700"
          />
        ) : (
          <div className="flex size-9 shrink-0 items-center justify-center rounded-full bg-slate-900 text-slate-500">
            <MessageCircle aria-hidden="true" className="size-4" />
          </div>
        )}
        <div className="min-w-0 flex-1">
          <h2 className="truncate text-sm font-semibold text-white">
            {participant?.username ?? "Conversation"}
          </h2>
          <p
            aria-live="polite"
            className={`mt-0.5 flex items-center gap-1.5 text-xs ${
              isParticipantOnline === true
                ? "text-emerald-400"
                : isParticipantOnline === false
                  ? "text-slate-500"
                  : "text-slate-500"
            }`}
          >
            <span
              aria-hidden="true"
              className={`size-1.5 rounded-full ${
                isParticipantOnline === true ? "bg-emerald-400" : "bg-slate-500"
              }`}
            />
            {isParticipantOnline === true
              ? "Online"
              : isParticipantOnline === false
                ? "Offline"
                : connectionState === "connected"
                  ? "Checking status…"
                  : "Status unavailable"}
          </p>
        </div>
      </header>

      {connectionError && (
        <p role="status" className="border-b border-rose-900/50 bg-rose-950/30 px-4 py-2 text-xs text-rose-200">
          {connectionError}
        </p>
      )}

      <div
        ref={scrollContainerRef}
        onScroll={(event) => {
          const container = event.currentTarget;
          shouldFollowRef.current =
            container.scrollHeight -
              container.clientHeight -
              container.scrollTop <
            80;
          if (container.scrollTop < 48) {
            loadOlderMessages();
          }
        }}
        role="log"
        aria-label="Messages"
        aria-live="polite"
        aria-relevant="additions text"
        className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-3 py-4 sm:px-6"
      >
        {hasNextPage && (
          <div className="mb-4 flex justify-center">
            <button
              type="button"
              onClick={loadOlderMessages}
              disabled={isFetchingNextPage}
              className="inline-flex min-h-9 items-center gap-2 rounded-full border border-slate-800 bg-slate-900/80 px-3 text-xs font-medium text-slate-300 transition hover:bg-slate-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-fuchsia-500 disabled:opacity-60"
            >
              {isFetchingNextPage ? (
                <LoaderCircle aria-hidden="true" className="size-3.5 animate-spin" />
              ) : (
                <RefreshCw aria-hidden="true" className="size-3.5" />
              )}
              {isFetchingNextPage ? "Loading older messages" : "Load older messages"}
            </button>
          </div>
        )}

        {isFetchNextPageError && (
          <div className="mb-4 text-center text-xs text-rose-300">
            Older messages couldn’t load.{" "}
            <button
              type="button"
              onClick={loadOlderMessages}
              className="font-semibold underline underline-offset-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-fuchsia-500"
            >
              Try again
            </button>
          </div>
        )}

        {isLoading ? (
          <div
            aria-label="Loading message history"
            className="flex h-full flex-col justify-end gap-3 py-6"
          >
            {[0, 1, 2, 3].map((item) => (
              <div
                key={item}
                className={`h-12 max-w-[72%] animate-pulse rounded-2xl bg-slate-900 ${
                  item % 2 === 0 ? "self-start" : "self-end"
                }`}
              />
            ))}
          </div>
        ) : isError && !data ? (
          <div className="flex h-full min-h-52 flex-col items-center justify-center text-center">
            <p className="text-sm font-medium text-white">
              Messages couldn’t load
            </p>
            <p className="mt-1 text-sm text-slate-400">
              This conversation may be unavailable.
            </p>
            <button
              type="button"
              onClick={() => void refetch()}
              className="mt-4 rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-200 transition hover:bg-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-fuchsia-500"
            >
              Try again
            </button>
          </div>
        ) : displayMessages.length === 0 ? (
          <div className="flex h-full min-h-52 flex-col items-center justify-center text-center">
            <div className="flex size-12 items-center justify-center rounded-2xl border border-slate-800 bg-slate-900 text-fuchsia-300">
              <MessageCircle aria-hidden="true" className="size-5" />
            </div>
            <p className="mt-4 text-sm font-medium text-white">
              Start the conversation
            </p>
            <p className="mt-1 text-sm text-slate-400">
              Send a message to get things going.
            </p>
          </div>
        ) : (
          <ol className="mx-auto flex max-w-3xl flex-col gap-1.5">
            {displayMessages.map((message, index) => {
              const outgoing = message.senderUsername === currentUsername;
              const previous = displayMessages[index - 1];
              const grouped =
                previous?.senderUsername === message.senderUsername &&
                new Date(message.createdAt).getTime() -
                  new Date(previous.createdAt).getTime() <
                  5 * 60 * 1000;
              const showDay =
                !previous ||
                new Date(previous.createdAt).toDateString() !==
                  new Date(message.createdAt).toDateString();
              return (
                <li key={message.key} className="flex flex-col">
                  {showDay && (
                    <div className="my-4 flex justify-center">
                      <time
                        dateTime={message.createdAt}
                        className="rounded-full border border-slate-800 bg-slate-900/80 px-3 py-1 text-[11px] font-medium text-slate-400"
                      >
                        {formatDay(message.createdAt)}
                      </time>
                    </div>
                  )}
                  <div
                    className={`flex items-end gap-2 ${
                      outgoing ? "justify-end" : "justify-start"
                    } ${grouped ? "mt-0" : "mt-2"}`}
                  >
                    {!outgoing && (
                      <span className="w-8 shrink-0">
                        {!grouped && (
                          <Avatar
                            imageUrl={message.senderAvatarUrl}
                            name={message.senderUsername}
                            size="sm"
                            className="border border-slate-700"
                          />
                        )}
                      </span>
                    )}
                    <div
                      className={`max-w-[min(82%,34rem)] rounded-2xl px-3.5 py-2.5 shadow-sm ${
                        outgoing
                          ? "rounded-br-md bg-fuchsia-600 text-white"
                          : "rounded-bl-md border border-slate-800 bg-slate-900 text-slate-100"
                      }`}
                    >
                      <p className="whitespace-pre-wrap break-words text-sm leading-5">
                        {message.content}
                      </p>
                      <div
                        className={`mt-1 flex items-center justify-end gap-1.5 text-[10px] ${
                          outgoing ? "text-fuchsia-100/70" : "text-slate-500"
                        }`}
                      >
                        <time
                          dateTime={message.createdAt}
                          title={new Date(message.createdAt).toLocaleString()}
                        >
                          {formatMessageTime(message.createdAt)}
                        </time>
                        {message.status === "pending" && (
                          <span className="inline-flex items-center gap-1">
                            <LoaderCircle
                              aria-hidden="true"
                              className="size-3 animate-spin"
                            />
                            Sending
                          </span>
                        )}
                        {message.status === "failed" && (
                          <>
                            <span className="inline-flex items-center gap-1 text-rose-200">
                              <CircleAlert
                                aria-hidden="true"
                                className="size-3"
                              />
                              Not sent
                            </span>
                            <button
                              type="button"
                              onClick={message.retry}
                              aria-label="Retry sending message"
                              className="rounded-sm font-semibold underline underline-offset-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white"
                            >
                              Retry
                            </button>
                          </>
                        )}
                        {outgoing && !message.status && message.read && (
                          <span
                            className="inline-flex items-center gap-1 text-sky-200"
                            title={
                              message.readAt
                                ? `Read at ${new Date(message.readAt).toLocaleString()}`
                                : "Read"
                            }
                            aria-label={
                              message.readAt
                                ? `Read at ${new Date(message.readAt).toLocaleString()}`
                                : "Read"
                            }
                          >
                            <CheckCheck aria-hidden="true" className="size-3.5" />
                            <span>
                              Read
                              {message.readAt &&
                                ` · ${formatMessageTime(message.readAt)}`}
                            </span>
                          </span>
                        )}
                        {outgoing && !message.status && !message.read && (
                          <span className="inline-flex items-center gap-1">
                            <Check aria-hidden="true" className="size-3" />
                            Sent
                          </span>
                        )}
                      </div>
                      {message.status === "failed" && message.error && (
                        <p className="mt-1 text-right text-[10px] text-rose-100/80">
                          {message.error}
                        </p>
                      )}
                    </div>
                  </div>
                </li>
              );
            })}
          </ol>
        )}
      </div>

      {isError && data && (
        <div role="status" className="border-t border-amber-900/50 bg-amber-950/20 px-4 py-2 text-xs text-amber-200">
          Couldn’t refresh the message history.{" "}
          <button
            type="button"
            onClick={() => void refetch()}
            className="font-semibold underline underline-offset-2"
          >
            Retry
          </button>
        </div>
      )}
      <MessageComposer conversationPublicId={conversationPublicId} />
    </section>
  );
}

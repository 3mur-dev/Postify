import { useState } from "react";
import { Link } from "react-router-dom";
import { Search, SquarePen } from "lucide-react";

import type { ConversationSummary } from "../../api/types";
import Avatar from "../home/components/Avatar";
import { formatRelativeTime } from "../../utils/time";

interface ConversationInboxProps {
  conversations: ConversationSummary[];
  currentUsername: string;
  activeConversationId?: string;
  isLoading: boolean;
  isError: boolean;
  onRetry: () => void;
}

export default function ConversationInbox({
  conversations,
  currentUsername,
  activeConversationId,
  isLoading,
  isError,
  onRetry,
}: ConversationInboxProps) {
  const [search, setSearch] = useState("");
  const filteredConversations = conversations.filter((conversation) =>
    conversation.participant.username
      .toLocaleLowerCase()
      .includes(search.trim().toLocaleLowerCase()),
  );

  return (
    <section
      aria-label="Conversation inbox"
      className="flex min-h-0 flex-1 flex-col bg-slate-950"
    >
      <header className="border-b border-slate-800 px-4 pb-4 pt-5 sm:px-5">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-semibold tracking-tight text-white">
            Messages
          </h1>
          <div className="flex items-center gap-2">
            <span
              aria-label={`${conversations.length} conversations`}
              className="rounded-full bg-slate-900 px-2.5 py-1 text-xs text-slate-400"
            >
              {conversations.length}
            </span>
            <Link
              to="/search"
              className="inline-flex h-9 items-center gap-1.5 rounded-xl bg-fuchsia-500 px-3 text-xs font-semibold text-white transition hover:bg-fuchsia-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-fuchsia-300 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950"
            >
              <SquarePen aria-hidden="true" className="size-3.5" />
              New
              <span className="sr-only">conversation</span>
            </Link>
          </div>
        </div>
        <label className="relative mt-4 block">
          <Search
            aria-hidden="true"
            className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-500"
          />
          <input
            type="search"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search conversations"
            aria-label="Search conversations"
            className="h-10 w-full rounded-xl border border-slate-800 bg-slate-900/80 pl-9 pr-3 text-sm text-white outline-none placeholder:text-slate-500 focus-visible:border-fuchsia-500/60 focus-visible:ring-2 focus-visible:ring-fuchsia-500/20"
          />
        </label>
      </header>

      <div className="min-h-0 flex-1 overflow-y-auto">
        {isLoading ? (
          <div aria-label="Loading conversations" className="space-y-2 p-3">
            {Array.from({ length: 6 }, (_, index) => (
              <div
                key={index}
                className="flex animate-pulse items-center gap-3 rounded-xl p-3"
              >
                <div className="size-12 shrink-0 rounded-full bg-slate-800" />
                <div className="min-w-0 flex-1 space-y-2">
                  <div className="h-3 w-2/5 rounded bg-slate-800" />
                  <div className="h-3 w-4/5 rounded bg-slate-800" />
                </div>
              </div>
            ))}
          </div>
        ) : isError && conversations.length === 0 ? (
          <div className="flex min-h-56 flex-col items-center justify-center px-6 text-center">
            <p className="text-sm font-medium text-white">
              Your inbox couldn’t load
            </p>
            <p className="mt-1 text-sm text-slate-400">
              Check your connection and try again.
            </p>
            <button
              type="button"
              onClick={onRetry}
              className="mt-4 rounded-lg border border-slate-700 px-3 py-2 text-sm font-medium text-slate-200 transition hover:bg-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-fuchsia-500"
            >
              Try again
            </button>
          </div>
        ) : filteredConversations.length > 0 ? (
          <ul className="space-y-1 p-2">
            {isError && (
              <li className="rounded-lg border border-amber-900/50 bg-amber-950/20 px-3 py-2 text-xs text-amber-200">
                Inbox refresh failed.{" "}
                <button
                  type="button"
                  onClick={onRetry}
                  className="font-semibold underline underline-offset-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-fuchsia-500"
                >
                  Retry
                </button>
              </li>
            )}
            {filteredConversations.map((conversation) => {
              const preview = conversation.latestMessage;
              const isActive =
                conversation.publicId === activeConversationId;
              return (
                <li key={conversation.publicId}>
                  <Link
                    to={`/messages/${encodeURIComponent(conversation.publicId)}`}
                    aria-current={isActive ? "page" : undefined}
                    className={`flex items-center gap-3 rounded-xl px-3 py-3 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-fuchsia-500 ${
                      isActive
                        ? "bg-slate-800/90"
                        : "hover:bg-slate-900/80"
                    }`}
                  >
                    <Avatar
                      imageUrl={conversation.participant.avatarUrl}
                      name={conversation.participant.username}
                      size="lg"
                      className="shrink-0 border border-slate-700"
                    />
                    <span className="min-w-0 flex-1">
                      <span className="flex items-center justify-between gap-2">
                        <span className="truncate text-sm font-semibold text-slate-100">
                          {conversation.participant.username}
                        </span>
                        <time
                          dateTime={preview?.createdAt ?? conversation.updatedAt}
                          title={new Date(
                            preview?.createdAt ?? conversation.updatedAt,
                          ).toLocaleString()}
                          className="shrink-0 text-[11px] text-slate-500"
                        >
                          {formatRelativeTime(
                            preview?.createdAt ?? conversation.updatedAt,
                          )}
                        </time>
                      </span>
                      <span className="mt-1 block truncate text-xs text-slate-400">
                        {preview ? (
                          <>
                            {preview.senderUsername === currentUsername
                              ? "You: "
                              : ""}
                            {preview.content}
                          </>
                        ) : (
                          "No messages yet"
                        )}
                      </span>
                    </span>
                  </Link>
                </li>
              );
            })}
          </ul>
        ) : conversations.length === 0 && !search.trim() ? (
          <div className="flex min-h-64 flex-col items-center justify-center px-6 text-center">
            <div className="flex size-12 items-center justify-center rounded-2xl border border-slate-800 bg-slate-900 text-fuchsia-300">
              <Search aria-hidden="true" className="size-5" />
            </div>
            <p className="mt-4 text-sm font-medium text-white">
              No conversations yet
            </p>
            <p className="mt-1 max-w-56 text-sm leading-5 text-slate-400">
              Find someone from Search and start a conversation.
            </p>
            <Link
              to="/search"
              className="mt-4 inline-flex min-h-9 items-center gap-2 rounded-xl bg-fuchsia-500 px-3 text-xs font-semibold text-white transition hover:bg-fuchsia-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-fuchsia-300 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950"
            >
              <SquarePen aria-hidden="true" className="size-3.5" />
              New conversation
            </Link>
          </div>
        ) : (
          <p className="px-5 py-10 text-center text-sm text-slate-400">
            No conversations match “{search.trim()}”.
          </p>
        )}
      </div>
    </section>
  );
}

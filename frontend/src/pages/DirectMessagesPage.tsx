import { MessageCircle } from "lucide-react";
import { Link, useParams, useNavigate } from "react-router-dom";

import ConversationInbox from "../features/messaging/ConversationInbox";
import ConversationPanel from "../features/messaging/ConversationPanel";
import { useAuth } from "../context/useAuth";
import { useConversations } from "../hooks/useConversations";

export default function DirectMessagesPage() {
  const navigate = useNavigate();
  const { conversationPublicId } = useParams();
  const { user, loading: authLoading, isAuthenticated } = useAuth();
  const {
    data: conversations = [],
    isLoading,
    isError,
    refetch,
  } = useConversations(isAuthenticated && !authLoading);

  if (authLoading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-2 border-slate-700 border-t-fuchsia-400" />
        <span className="sr-only">Checking your session</span>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return (
      <section className="mx-auto flex min-h-[60vh] max-w-lg flex-col items-center justify-center rounded-3xl border border-slate-800 bg-slate-950 px-6 text-center">
        <div className="flex size-14 items-center justify-center rounded-2xl border border-slate-800 bg-slate-900 text-fuchsia-300">
          <MessageCircle aria-hidden="true" className="size-6" />
        </div>
        <h1 className="mt-5 text-xl font-semibold text-white">
          Sign in to see your messages
        </h1>
        <p className="mt-2 text-sm text-slate-400">
          Your conversations are private and only available to you.
        </p>
        <button
          type="button"
          onClick={() =>
            navigate("/login", {
              state: { from: { pathname: window.location.pathname } },
            })
          }
          className="mt-5 rounded-xl bg-fuchsia-500 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-fuchsia-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-fuchsia-300 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950"
        >
          Sign in
        </button>
      </section>
    );
  }

  const activeConversation = conversations.find(
    (conversation) => conversation.publicId === conversationPublicId,
  );

  return (
    <div className="h-[calc(100dvh-5.5rem)] overflow-hidden rounded-2xl border border-slate-800 bg-slate-950 shadow-2xl shadow-black/20 sm:h-[calc(100dvh-2.5rem)]">
      <div className="grid h-full min-h-0 grid-cols-1 md:grid-cols-[minmax(17rem,21rem)_minmax(0,1fr)]">
        <div
          className={`${
            conversationPublicId ? "hidden md:flex" : "flex"
          } min-h-0 flex-col border-r border-slate-800`}
        >
          <ConversationInbox
            conversations={conversations}
            currentUsername={user.username}
            activeConversationId={conversationPublicId}
            isLoading={isLoading}
            isError={isError}
            onRetry={() => void refetch()}
          />
        </div>

        <div
          className={`${
            conversationPublicId ? "flex" : "hidden md:flex"
          } min-h-0 min-w-0 flex-col`}
        >
          {conversationPublicId ? (
            <ConversationPanel
              key={conversationPublicId}
              conversationPublicId={conversationPublicId}
              currentUsername={user.username}
              conversation={activeConversation}
            />
          ) : (
            <div className="flex h-full flex-col items-center justify-center px-6 text-center">
              <div className="flex size-16 items-center justify-center rounded-3xl border border-slate-800 bg-slate-900 text-fuchsia-300">
                <MessageCircle aria-hidden="true" className="size-7" />
              </div>
              <h2 className="mt-5 text-lg font-semibold text-white">
                Your conversations, all in one place
              </h2>
              <p className="mt-2 max-w-sm text-sm leading-6 text-slate-400">
                Choose a conversation or start a new one.
              </p>
              <Link
                to="/search"
                className="mt-5 inline-flex min-h-10 items-center justify-center rounded-xl bg-fuchsia-500 px-4 text-sm font-semibold text-white transition hover:bg-fuchsia-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-fuchsia-300 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950"
              >
                New conversation
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

import {
  useRef,
  useState,
  type FormEvent,
  type KeyboardEvent,
} from "react";
import { Send } from "lucide-react";

import { useMessaging } from "../../hooks/useMessaging";

const MAX_MESSAGE_LENGTH = 10_000;

export default function MessageComposer({
  conversationPublicId,
}: {
  conversationPublicId: string;
}) {
  const { sendMessage } = useMessaging();
  const [draft, setDraft] = useState("");
  const [error, setError] = useState<string | null>(null);
  const sendingRef = useRef(false);
  const submittedDraftRef = useRef<string | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const submit = (event?: FormEvent<HTMLFormElement>) => {
    event?.preventDefault();
    const content = draft;
    if (
      !content.trim() ||
      content.length > MAX_MESSAGE_LENGTH ||
      sendingRef.current ||
      submittedDraftRef.current === content
    ) {
      return;
    }

    sendingRef.current = true;
    submittedDraftRef.current = content;
    try {
      sendMessage(conversationPublicId, content);
      setDraft("");
      setError(null);
      if (textareaRef.current) {
        textareaRef.current.style.height = "auto";
      }
    } catch (sendError) {
      submittedDraftRef.current = null;
      setError(
        sendError instanceof Error
          ? sendError.message
          : "The message could not be queued.",
      );
    } finally {
      sendingRef.current = false;
    }
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault();
      submit();
    }
  };

  return (
    <form
      onSubmit={submit}
      className="shrink-0 border-t border-slate-800 bg-slate-950/95 px-3 pb-3 pt-3 sm:px-5 sm:pb-4"
    >
      {error && (
        <p role="alert" className="mb-2 text-xs text-rose-300">
          {error}
        </p>
      )}
      <div className="flex items-end gap-2 rounded-2xl border border-slate-700 bg-slate-900/90 p-2 transition focus-within:border-fuchsia-500/60 focus-within:ring-2 focus-within:ring-fuchsia-500/15">
        <label className="sr-only" htmlFor="direct-message-composer">
          Write a message
        </label>
        <textarea
          ref={textareaRef}
          id="direct-message-composer"
          value={draft}
          onChange={(event) => {
            setDraft(event.target.value);
            submittedDraftRef.current = null;
            setError(null);
            event.currentTarget.style.height = "auto";
            event.currentTarget.style.height = `${Math.min(
              event.currentTarget.scrollHeight,
              128,
            )}px`;
          }}
          onKeyDown={handleKeyDown}
          rows={1}
          maxLength={MAX_MESSAGE_LENGTH}
          placeholder="Write a message…"
          aria-describedby="direct-message-hint"
          className="max-h-32 min-h-10 min-w-0 flex-1 resize-none overflow-y-auto bg-transparent px-2 py-2 text-sm leading-5 text-white outline-none placeholder:text-slate-500"
        />
        <button
          type="submit"
          disabled={!draft.trim() || draft.trim().length > MAX_MESSAGE_LENGTH}
          aria-label="Send message"
          className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-fuchsia-500 text-white shadow-lg shadow-fuchsia-950/30 transition hover:bg-fuchsia-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-fuchsia-300 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950 disabled:cursor-not-allowed disabled:bg-slate-800 disabled:text-slate-500 disabled:shadow-none"
        >
          <Send aria-hidden="true" className="size-4" />
        </button>
      </div>
      <div
        id="direct-message-hint"
        className="mt-2 flex items-center justify-between gap-3 px-1 text-[11px] text-slate-500"
      >
        <span>Enter to send · Shift+Enter for a new line</span>
        {draft.length > MAX_MESSAGE_LENGTH - 100 && (
          <span aria-live="polite">
            {draft.length.toLocaleString()} / {MAX_MESSAGE_LENGTH.toLocaleString()}
          </span>
        )}
      </div>
    </form>
  );
}

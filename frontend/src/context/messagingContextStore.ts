import { createContext } from "react";
import type {
  MessagingConnectionState,
  PendingDirectMessage,
} from "./messagingTypes";

export interface MessagingContextValue {
  connectionState: MessagingConnectionState;
  connectionError: string | null;
  pendingMessages: PendingDirectMessage[];
  participantPresence: Record<string, boolean>;
  sendMessage: (conversationPublicId: string, content: string) => string;
  requestPresence: (conversationPublicId: string) => void;
  retryMessage: (clientMessageId: string) => void;
  resolvePendingMessage: (clientMessageId: string) => void;
}

export const MessagingContext =
  createContext<MessagingContextValue | null>(null);

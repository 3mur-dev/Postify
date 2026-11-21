export type MessagingConnectionState =
  | "disconnected"
  | "connecting"
  | "connected"
  | "reconnecting"
  | "error";

export interface PendingDirectMessage {
  clientMessageId: string;
  conversationPublicId: string;
  content: string;
  createdAt: string;
  status: "pending" | "failed";
  error?: string;
}

export interface ParticipantPresence {
  conversationPublicId: string;
  participantPublicId: string;
  online: boolean;
}

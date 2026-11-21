import { privateApi } from "./axios";
import type {
  ApiResponse,
  ConversationCreatedResponse,
  ConversationSummary,
  DirectMessage,
  SpringPage,
} from "./types";

export async function getConversations(): Promise<ConversationSummary[]> {
  const { data } = await privateApi.get<ApiResponse<ConversationSummary[]>>(
    "/conversations",
  );
  return data.data;
}

export async function getConversationMessages(
  conversationPublicId: string,
  page: number,
  size = 30,
): Promise<SpringPage<DirectMessage>> {
  const { data } = await privateApi.get<ApiResponse<SpringPage<DirectMessage>>>(
    `/conversations/${encodeURIComponent(conversationPublicId)}/messages`,
    { params: { page, size } },
  );
  return data.data;
}

export async function createConversation(
  userPublicId: string,
): Promise<ConversationCreatedResponse> {
  const { data } = await privateApi.post<
    ApiResponse<ConversationCreatedResponse>
  >(`/conversations/${encodeURIComponent(userPublicId)}`);
  return data.data;
}

export async function markMessagesAsRead(conversationPublicId: string): Promise<void> {
  await privateApi.post(
    `/conversations/${encodeURIComponent(conversationPublicId)}/read`,
  );
}

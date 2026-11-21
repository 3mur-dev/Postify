import type { ApiResponse, NotificationResponse, PagedResponse } from "./types";
import { privateApi } from "./axios";

export async function getNotifications(
    page = 0,
    size = 20,
): Promise<PagedResponse<NotificationResponse>> {
    const response = await privateApi.get<ApiResponse<PagedResponse<NotificationResponse>>>(
        "/notifications",
        { params: { page, size } },
    );
    return response.data.data;
}

export async function markAsRead(
    publicId: string
): Promise<void> {
    await privateApi.patch(`/notifications/${publicId}/read`);
}
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { markAsRead } from "../api/notification";
import type { NotificationResponse, PagedResponse } from "../api/types";

export function useMarkAsRead() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (publicId: string) => markAsRead(publicId),

        onSuccess: (_, publicId) => {
            queryClient.setQueryData<PagedResponse<NotificationResponse>>(
                ["notifications"],
                (paged) =>
                    paged
                        ? {
                              ...paged,
                              content: paged.content.map((notification) =>
                                  notification.publicId === publicId
                                      ? {
                                            ...notification,
                                            isRead: true,
                                        }
                                      : notification
                              ),
                          }
                        : paged,
            );
        },
    });
}
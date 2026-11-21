import {
    Heart,
    MessageCircle,
    UserPlus,
    Bell,
    CheckCircle2,
} from "lucide-react";
import { useGetNotifications } from "../hooks/useGetNotification";
import { useNavigate } from "react-router-dom";
import { useMarkAsRead } from "../hooks/useMarkAsRead";
import type { NotificationResponse } from "../api/types";

function NotificationIcon({ type }: { type: string }) {
    switch (type) {
        case "LIKE":
            return (
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-pink-500/10 text-pink-500">
                    <Heart size={19} fill="currentColor" />
                </div>
            );

        case "COMMENT":
            return (
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-blue-500/10 text-blue-500">
                    <MessageCircle size={19} />
                </div>
            );

        case "FOLLOW":
            return (
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-green-500/10 text-green-500">
                    <UserPlus size={19} />
                </div>
            );

        case "SYSTEM":
            return (
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-purple-500/10 text-purple-500">
                    <Bell size={19} />
                </div>
            );

        default:
            return (
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-gray-500/10 text-gray-500">
                    <Bell size={19} />
                </div>
            );
    }
}

function formatTime(dateString: string) {
    const date = new Date(dateString);
    const now = new Date();

    const seconds = Math.floor(
        (now.getTime() - date.getTime()) / 1000
    );

    if (seconds < 60) {
        return "Just now";
    }

    const minutes = Math.floor(seconds / 60);

    if (minutes < 60) {
        return `${minutes}m`;
    }

    const hours = Math.floor(minutes / 60);

    if (hours < 24) {
        return `${hours}h`;
    }

    const days = Math.floor(hours / 24);

    if (days === 1) {
        return "Yesterday";
    }

    if (days < 7) {
        return `${days}d`;
    }

    return date.toLocaleDateString();
}

function handleClickNotification(
    notification: NotificationResponse,
    markAsRead: (publicId: string) => void,
    navigate: (path: string) => void
) {
    if (!notification.isRead) {
        markAsRead(notification.publicId);
    }

    if (
        (notification.type === "LIKE" ||
            notification.type === "COMMENT") &&
        notification.actorUsername &&
        notification.postPublicId
    ) {
        navigate(
            `/${notification.actorUsername}/posts/${notification.postPublicId}`
        );
    } else if (
        notification.type === "FOLLOW" &&
        notification.actorUsername
    ) {
        navigate(`/profile/${notification.actorUsername}`);
    }
}

export function NotificationsPage() {

    const markAsReadMutation = useMarkAsRead();
    const {
        data: pagedNotifications,
        isLoading,
        isError,
    } = useGetNotifications();

    const notifications = pagedNotifications?.content ?? [];

    const navigate = useNavigate();

    if (isLoading) {
        return (
            <div className="mx-auto w-full max-w-2xl px-4 py-8">
                <div className="mb-8">
                    <div className="h-8 w-40 animate-pulse rounded bg-gray-200 dark:bg-gray-800" />
                </div>

                <div className="space-y-3">
                    {Array.from({ length: 6 }).map((_, index) => (
                        <div
                            key={index}
                            className="flex items-center gap-4 rounded-2xl p-4"
                        >
                            <div className="h-11 w-11 animate-pulse rounded-full bg-gray-200 dark:bg-gray-800" />

                            <div className="flex-1 space-y-2">
                                <div className="h-4 w-3/4 animate-pulse rounded bg-gray-200 dark:bg-gray-800" />
                                <div className="h-3 w-16 animate-pulse rounded bg-gray-200 dark:bg-gray-800" />
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        );
    }

    if (isError) {
        return (
            <div className="mx-auto w-full max-w-2xl px-4 py-8">
                <h1 className="text-2xl font-bold">Notifications</h1>

                <div className="mt-8 rounded-2xl border border-red-500/20 bg-red-500/5 p-6 text-center">
                    <p className="text-sm text-red-500">
                        Failed to load notifications.
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className="mx-auto w-full max-w-2xl px-4 py-8">
            {/* Header */}
            <div className="mb-8 flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight">
                        Notifications
                    </h1>

                    <p className="mt-1 text-sm text-gray-500">
                        Stay up to date with what's happening.
                    </p>
                </div>

                {notifications.length > 0 && (
                    <div className="flex items-center gap-2 text-xs text-gray-500">
                        <CheckCircle2 size={15} />
                        {notifications.length} notifications
                    </div>
                )}
            </div>

            {/* Empty state */}
            {notifications.length === 0 ? (
                <div className="flex flex-col items-center justify-center rounded-3xl border border-gray-200/70 bg-white px-6 py-20 text-center shadow-sm dark:border-gray-800 dark:bg-gray-950">
                    <div className="mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-gray-100 dark:bg-gray-900">
                        <Bell
                            size={28}
                            className="text-gray-400"
                        />
                    </div>

                    <h2 className="text-lg font-semibold">
                        No notifications yet
                    </h2>

                    <p className="mt-2 max-w-sm text-sm text-gray-500">
                        When someone likes, comments on, or follows you,
                        you'll see it here.
                    </p>
                </div>
            ) : (
                /* Notification list */
                <div className="overflow-hidden rounded-3xl border border-gray-200/70 bg-white shadow-sm dark:border-gray-800 dark:bg-gray-950">
                    {notifications.map((notification) => (
                        <button
                            key={notification.publicId}
                            type="button"
                            className={`group flex w-full items-center gap-4 border-b border-gray-100 px-5 py-4 text-left transition last:border-b-0 dark:border-gray-900 ${
                                notification.isRead
                                    ? "bg-white hover:bg-gray-50 dark:bg-gray-950 dark:hover:bg-gray-900/60"
                                    : "bg-blue-50/60 hover:bg-blue-50 dark:bg-blue-950/20 dark:hover:bg-blue-950/30"
                            }`}
                            onClick={() =>
                                handleClickNotification(
                                    notification,
                                    (publicId) => markAsReadMutation.mutate(publicId),
                                    navigate
                                )
                            }                       
                                
                            >
                            <NotificationIcon
                                type={notification.type}
                            />

                            <div className="min-w-0 flex-1">
                                <p className="text-sm leading-5 text-gray-900 dark:text-gray-100">
                                    {notification.message}
                                </p>

                                <p className="mt-1 text-xs text-gray-400">
                                    {formatTime(
                                        notification.createdAt
                                    )}
                                </p>
                            </div>

                            {!notification.isRead && (
                                <span className="h-2.5 w-2.5 shrink-0 rounded-full bg-blue-500" />
                            )}
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
}

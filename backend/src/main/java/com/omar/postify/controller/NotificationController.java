package com.omar.postify.controller;

import com.omar.postify.dto.ApiResponse;
import com.omar.postify.dto.NotificationResponse;
import com.omar.postify.dto.PagedResponse;
import com.omar.postify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PagedResponse<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        return ApiResponse.success(
                "Notifications retrieved successfully",
                notificationService.getNotifications(page, size)
        );
    }

    @PatchMapping("/{publicId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable String publicId) {

        notificationService.markAsRead(publicId);

        return ApiResponse.success(
                "Notification marked as read successfully",
                null
        );
    }
}

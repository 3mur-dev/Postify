package com.omar.postify.mapper;

import com.omar.postify.dto.NotificationResponse;
import com.omar.postify.entities.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getPublicId(),
                notification.getMessage(),
                notification.getType(),
                notification.getActor() != null
                        ? notification.getActor().getPublicId()
                        : null,
                notification.getActor() != null
                        ? notification.getActor().getUsername()
                        : null,
                notification.getPost() != null
                        ? notification.getPost().getPublicId()
                        : null,
                notification.getComment() != null
                        ? notification.getComment().getId()
                        : null,
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}

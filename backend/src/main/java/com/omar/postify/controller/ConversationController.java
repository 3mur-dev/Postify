package com.omar.postify.controller;

import com.omar.postify.dto.ApiResponse;
import com.omar.postify.dto.ConversationCreatedResponse;
import com.omar.postify.dto.ConversationSummaryResponse;
import com.omar.postify.dto.MessageResponse;
import com.omar.postify.service.ConversationService;
import com.omar.postify.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {

    private final MessageService messageService;
    private final ConversationService conversationService;

    @GetMapping
    public ApiResponse<List<ConversationSummaryResponse>> getConversations() {
        return ApiResponse.success(
                "Conversations retrieved successfully",
                conversationService.getConversations()
        );
    }

    @GetMapping("/{publicId}/messages")
    public ApiResponse<Page<MessageResponse>> getMessages(
            @PathVariable String publicId,
            @PageableDefault(size = 30) Pageable pageable) {

        return ApiResponse.success("Messages retrieved successfully", messageService.getMessages(publicId, pageable));
    }

    @PostMapping("/{targetUserPublicId}")
    public ApiResponse<ConversationCreatedResponse> createConversation(
            @PathVariable String targetUserPublicId
    ) {
        return ApiResponse.success(
                "Conversation created successfully",
                new ConversationCreatedResponse(
                        conversationService.getOrCreateConversation(targetUserPublicId).getPublicId()
                )
        );
    }
    @PostMapping("/{publicId}/read")
    public ApiResponse<Void> markConversationAsRead(
            @PathVariable String publicId
    ) {
        conversationService.markConversationAsRead(publicId);
        return ApiResponse.success("Conversation marked as read", null);
    }

}

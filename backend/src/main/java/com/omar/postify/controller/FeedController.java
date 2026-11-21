package com.omar.postify.controller;

import com.omar.postify.dto.*;
import com.omar.postify.service.feed.FeedService;
import com.omar.postify.util.RequestIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;
    private final RequestIdentityResolver requestIdentityResolver;

    @GetMapping
    public PagedResponse<PostFeedDto> feed(
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        FeedService.validatePagination(page, size);
        RequestIdentity identity =
                requestIdentityResolver.resolve(request, response);

        return PagedResponse.from(feedService.getFeed(identity, page, size));
    }
}

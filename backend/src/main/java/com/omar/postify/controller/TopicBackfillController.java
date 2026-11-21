package com.omar.postify.controller;

import com.omar.postify.service.PostTopicBackfillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/topic-backfill")
public class TopicBackfillController {

    private final PostTopicBackfillService backfillService;

    @PostMapping
    public ResponseEntity<Void> backfill() {
        backfillService.backfill();
        return ResponseEntity.ok().build();
    }
}
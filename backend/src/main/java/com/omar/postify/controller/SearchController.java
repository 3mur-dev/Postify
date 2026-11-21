package com.omar.postify.controller;

import com.omar.postify.dto.ApiResponse;
import com.omar.postify.dto.SearchResponse;
import com.omar.postify.dto.UserSearchDto;
import com.omar.postify.service.ProfileService;
import com.omar.postify.service.SearchService;
import com.omar.postify.service.ai.QdrantVectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
@Slf4j
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ApiResponse<SearchResponse> search(@RequestParam("q") String query) {

        log.info("Searching for query: {}", query);

        SearchResponse results = searchService.search(query);

        log.info("Search results for query {}: {}", query, results);

        return ApiResponse.success("Search successful", results);
    }
}

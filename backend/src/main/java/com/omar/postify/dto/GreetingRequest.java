package com.omar.postify.dto;

public record GreetingRequest(
        String username,
        String message
) {}
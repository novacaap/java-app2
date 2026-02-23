package com.dockerforjavadevelopers.hello.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A simple message response")
public record Message(
        @Schema(description = "Message content", example = "Hello, World!")
        String content,
        @Schema(description = "Timestamp in ISO-8601 format")
        String timestamp
) {}

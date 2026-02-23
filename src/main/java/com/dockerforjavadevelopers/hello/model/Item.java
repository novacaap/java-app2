package com.dockerforjavadevelopers.hello.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sample item entity")
public record Item(
        @Schema(description = "Unique identifier")
        Long id,
        @Schema(description = "Item name", example = "Sample Item")
        String name,
        @Schema(description = "Optional description")
        String description
) {}

package com.arcticsurge.cosmolab.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Generic paginated response wrapper")
public record PagedResponse<T>(
        @Schema(description = "Page content") List<T> content,
        @Schema(description = "Zero-based current page index") int page,
        @Schema(description = "Number of elements per page") int size,
        @Schema(description = "Total number of elements across all pages") long totalElements,
        @Schema(description = "Total number of pages") int totalPages,
        @Schema(description = "True if this is the last page") boolean last
) {
    public static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}

package com.portfolio.onthisday.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Serialization-friendly pagination envelope for infinite scroll. Avoids leaking Spring's
 * {@code PageImpl} JSON shape (which is unstable across versions) to the frontend.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    /** Wrap a Spring {@link Page} of entities, mapping each element to a response DTO. */
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}

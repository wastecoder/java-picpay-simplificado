package com.wastecoder.picpay.common.adapter.controller.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wastecoder.picpay.common.domain.viewmodels.PagedResult;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(

        @JsonProperty("content")
        List<T> content,

        @JsonProperty("page")
        int page,

        @JsonProperty("size")
        int size,

        @JsonProperty("total_elements")
        long totalElements,

        @JsonProperty("total_pages")
        int totalPages,

        @JsonProperty("last")
        boolean last

) {

    public static <D, R> PageResponse<R> from(PagedResult<D> result, Function<? super D, ? extends R> mapper) {
        boolean last = result.page() >= result.totalPages() - 1;
        return new PageResponse<>(
                result.items().stream().<R>map(mapper).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                last
        );
    }
}

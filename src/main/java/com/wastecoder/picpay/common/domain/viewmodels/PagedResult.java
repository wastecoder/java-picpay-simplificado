package com.wastecoder.picpay.common.domain.viewmodels;

import java.util.List;
import java.util.function.Function;

public record PagedResult<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public <U> PagedResult<U> map(Function<? super T, ? extends U> mapper) {
        return new PagedResult<>(
                items.stream().<U>map(mapper).toList(),
                page,
                size,
                totalElements,
                totalPages
        );
    }
}

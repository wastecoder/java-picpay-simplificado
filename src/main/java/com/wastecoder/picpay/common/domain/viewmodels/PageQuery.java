package com.wastecoder.picpay.common.domain.viewmodels;

import java.util.List;

public record PageQuery(
        int page,
        int size,
        List<SortOrder> sortOrders
) {}

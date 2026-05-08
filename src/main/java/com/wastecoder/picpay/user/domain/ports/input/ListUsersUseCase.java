package com.wastecoder.picpay.user.domain.ports.input;

import com.wastecoder.picpay.common.domain.viewmodels.PageQuery;
import com.wastecoder.picpay.common.domain.viewmodels.PagedResult;
import com.wastecoder.picpay.user.domain.viewmodels.UserSummary;

public interface ListUsersUseCase {

    PagedResult<UserSummary> execute(PageQuery query);
}

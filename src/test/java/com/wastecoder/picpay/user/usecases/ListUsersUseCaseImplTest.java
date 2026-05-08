package com.wastecoder.picpay.user.usecases;

import com.wastecoder.picpay.common.domain.viewmodels.PageQuery;
import com.wastecoder.picpay.common.domain.viewmodels.PagedResult;
import com.wastecoder.picpay.common.domain.viewmodels.SortDirection;
import com.wastecoder.picpay.common.domain.viewmodels.SortOrder;
import com.wastecoder.picpay.user.domain.enums.UserType;
import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.input.ListUsersUseCase;
import com.wastecoder.picpay.user.domain.ports.output.UserRepository;
import com.wastecoder.picpay.user.domain.viewmodels.UserSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.wastecoder.picpay.user.UserMother.commonUserWith;
import static com.wastecoder.picpay.user.UserMother.merchantUserWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListUsersUseCaseImplTest {

    private static final UUID USER_A_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_B_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private UserRepository userRepository;

    private ListUsersUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListUsersUseCaseImpl(userRepository);
    }

    @Test
    @DisplayName("GIVEN repository returns 2 users WHEN executing THEN maps each user to a UserSummary preserving page metadata")
    void shouldMapEachUserToSummaryAndPreserveMetadata() {
        User commonUser = commonUserWith(USER_A_ID, new BigDecimal("50.00"));
        User merchantUser = merchantUserWith(USER_B_ID, new BigDecimal("0.00"));
        PageQuery query = new PageQuery(0, 20, List.of(new SortOrder("fullName", SortDirection.ASC)));
        PagedResult<User> repoResult = new PagedResult<>(
                List.of(commonUser, merchantUser), 0, 20, 2L, 1
        );

        when(userRepository.findAll(query)).thenReturn(repoResult);

        PagedResult<UserSummary> result = useCase.execute(query);

        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(2L);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.items()).containsExactly(
                new UserSummary(USER_A_ID, commonUser.fullName(), UserType.COMMON),
                new UserSummary(USER_B_ID, merchantUser.fullName(), UserType.MERCHANT)
        );
    }

    @Test
    @DisplayName("GIVEN repository returns empty list WHEN executing THEN result has empty items but preserves metadata")
    void shouldPreserveMetadataWhenItemsEmpty() {
        PageQuery query = new PageQuery(2, 10, List.of(new SortOrder("type", SortDirection.DESC)));
        PagedResult<User> repoResult = new PagedResult<>(List.of(), 2, 10, 0L, 0);

        when(userRepository.findAll(query)).thenReturn(repoResult);

        PagedResult<UserSummary> result = useCase.execute(query);

        assertThat(result.items()).isEmpty();
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }
}

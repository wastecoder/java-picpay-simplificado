package com.wastecoder.picpay.user.adapter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastecoder.picpay.common.domain.viewmodels.PageQuery;
import com.wastecoder.picpay.common.domain.viewmodels.PagedResult;
import com.wastecoder.picpay.common.domain.viewmodels.SortDirection;
import com.wastecoder.picpay.user.UserMother;
import com.wastecoder.picpay.user.adapter.controller.request.CreateUserRequest;
import com.wastecoder.picpay.user.domain.enums.UserType;
import com.wastecoder.picpay.user.domain.exceptions.DocumentAlreadyRegisteredException;
import com.wastecoder.picpay.user.domain.exceptions.EmailAlreadyRegisteredException;
import com.wastecoder.picpay.user.domain.exceptions.UserNotFoundException;
import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.input.CreateUserUseCase;
import com.wastecoder.picpay.user.domain.ports.input.DepositUseCase;
import com.wastecoder.picpay.user.domain.ports.input.ListUsersUseCase;
import com.wastecoder.picpay.user.domain.viewmodels.DepositCommand;
import com.wastecoder.picpay.user.domain.viewmodels.DepositResult;
import com.wastecoder.picpay.user.domain.viewmodels.UserSummary;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    private static final String USERS_ENDPOINT = "/api/v1/users";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateUserUseCase createUserUseCase;

    @MockBean
    private DepositUseCase depositUseCase;

    @MockBean
    private ListUsersUseCase listUsersUseCase;

    @Test
    @DisplayName("GIVEN a valid common user payload WHEN POST /api/v1/users THEN returns 201 with Location header pointing to the new user")
    void shouldReturnCreatedWithLocationHeaderPointingToNewUser() throws Exception {
        // Given
        CreateUserRequest request = CreateUserRequestMother.valid();
        UUID newId = UUID.randomUUID();
        when(createUserUseCase.execute(any(User.class)))
                .thenReturn(UserMother.validCommonUserWithId(newId));

        // When / Then
        mockMvc.perform(post(USERS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(USERS_ENDPOINT + "/" + newId)));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(createUserUseCase).execute(captor.capture());
        User passed = captor.getValue();
        Assertions.assertAll(
                () -> assertThat(passed.fullName()).isEqualTo(CreateUserRequestMother.FULL_NAME_DEFAULT),
                () -> assertThat(passed.document()).isEqualTo(CreateUserRequestMother.DOCUMENT_DEFAULT),
                () -> assertThat(passed.email()).isEqualTo(CreateUserRequestMother.EMAIL_DEFAULT),
                () -> assertThat(passed.password()).isEqualTo(CreateUserRequestMother.PASSWORD_DEFAULT),
                () -> assertThat(passed.type()).isEqualTo(UserType.COMMON)
        );
    }

    @Test
    @DisplayName("GIVEN a valid merchant user payload WHEN POST /api/v1/users THEN returns 201 and use case receives MERCHANT type")
    void shouldAcceptMerchantType() throws Exception {
        // Given
        CreateUserRequest request = CreateUserRequestMother.validMerchant();
        UUID newId = UUID.randomUUID();
        when(createUserUseCase.execute(any(User.class)))
                .thenReturn(UserMother.merchantUserWith(newId, java.math.BigDecimal.ZERO));

        // When / Then
        mockMvc.perform(post(USERS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(USERS_ENDPOINT + "/" + newId)));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(createUserUseCase).execute(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(UserType.MERCHANT);
    }


    @Test
    @DisplayName("GIVEN a payload with blank full_name WHEN POST /api/v1/users THEN returns 400 and use case is not called")
    void shouldReturn400WhenFullNameIsBlank() throws Exception {
        // Given
        CreateUserRequest request = CreateUserRequestMother.withBlankFullName();

        // When / Then
        mockMvc.perform(post(USERS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(createUserUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN a payload with blank document WHEN POST /api/v1/users THEN returns 400 and use case is not called")
    void shouldReturn400WhenDocumentIsBlank() throws Exception {
        // Given
        CreateUserRequest request = CreateUserRequestMother.withBlankDocument();

        // When / Then
        mockMvc.perform(post(USERS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(createUserUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN a payload with blank email WHEN POST /api/v1/users THEN returns 400 and use case is not called")
    void shouldReturn400WhenEmailIsBlank() throws Exception {
        // Given
        CreateUserRequest request = CreateUserRequestMother.withBlankEmail();

        // When / Then
        mockMvc.perform(post(USERS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(createUserUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN a payload with blank password WHEN POST /api/v1/users THEN returns 400 and use case is not called")
    void shouldReturn400WhenPasswordIsBlank() throws Exception {
        // Given
        CreateUserRequest request = CreateUserRequestMother.withBlankPassword();

        // When / Then
        mockMvc.perform(post(USERS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(createUserUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN a payload with blank type WHEN POST /api/v1/users THEN returns 400 and use case is not called")
    void shouldReturn400WhenTypeIsBlank() throws Exception {
        // Given
        CreateUserRequest request = CreateUserRequestMother.withBlankType();

        // When / Then
        mockMvc.perform(post(USERS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(createUserUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN a payload with type not in {COMMON, MERCHANT} WHEN POST /api/v1/users THEN returns 400 and use case is not called")
    void shouldReturn400WhenTypeIsInvalid() throws Exception {
        // Given
        CreateUserRequest request = CreateUserRequestMother.withInvalidType();

        // When / Then
        mockMvc.perform(post(USERS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(createUserUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN use case throws EmailAlreadyRegisteredException WHEN POST /api/v1/users THEN returns 409")
    void shouldReturn409WhenEmailAlreadyRegistered() throws Exception {
        // Given
        CreateUserRequest request = CreateUserRequestMother.valid();
        when(createUserUseCase.execute(any(User.class)))
                .thenThrow(new EmailAlreadyRegisteredException());

        // When / Then
        mockMvc.perform(post(USERS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GIVEN use case throws DocumentAlreadyRegisteredException WHEN POST /api/v1/users THEN returns 409")
    void shouldReturn409WhenDocumentAlreadyRegistered() throws Exception {
        // Given
        CreateUserRequest request = CreateUserRequestMother.valid();
        when(createUserUseCase.execute(any(User.class)))
                .thenThrow(new DocumentAlreadyRegisteredException());

        // When / Then
        mockMvc.perform(post(USERS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GIVEN a valid deposit payload WHEN POST /api/v1/users/{user_id}/deposit THEN returns 200 with new_balance and deposited_at")
    void shouldReturnOkWhenDepositSucceeds() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        BigDecimal newBalance = new BigDecimal("150.00");
        LocalDateTime depositedAt = LocalDateTime.parse("2026-05-07T12:00:00");
        when(depositUseCase.execute(any(DepositCommand.class)))
                .thenReturn(new DepositResult(userId, newBalance, depositedAt));

        // When / Then
        mockMvc.perform(post(USERS_ENDPOINT + "/" + userId + "/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": 100.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value(userId.toString()))
                .andExpect(jsonPath("$.new_balance").value(150.00))
                .andExpect(jsonPath("$.deposited_at").value("2026-05-07T12:00"));

        ArgumentCaptor<DepositCommand> captor = ArgumentCaptor.forClass(DepositCommand.class);
        verify(depositUseCase).execute(captor.capture());
        DepositCommand passed = captor.getValue();
        Assertions.assertAll(
                () -> assertThat(passed.userId()).isEqualTo(userId),
                () -> assertThat(passed.value()).isEqualByComparingTo("100.00")
        );
    }

    @Test
    @DisplayName("GIVEN deposit payload with missing value WHEN POST /api/v1/users/{user_id}/deposit THEN returns 400 and use case is not called")
    void shouldReturn400WhenDepositValueIsMissing() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post(USERS_ENDPOINT + "/" + userId + "/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(depositUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN deposit payload with zero value WHEN POST /api/v1/users/{user_id}/deposit THEN returns 400 and use case is not called")
    void shouldReturn400WhenDepositValueIsZero() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post(USERS_ENDPOINT + "/" + userId + "/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": 0}"))
                .andExpect(status().isBadRequest());

        verify(depositUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN deposit payload with negative value WHEN POST /api/v1/users/{user_id}/deposit THEN returns 400 and use case is not called")
    void shouldReturn400WhenDepositValueIsNegative() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post(USERS_ENDPOINT + "/" + userId + "/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": -10.00}"))
                .andExpect(status().isBadRequest());

        verify(depositUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN user_id path variable that is not a UUID WHEN POST /api/v1/users/{user_id}/deposit THEN returns 400 and use case is not called")
    void shouldReturn400WhenDepositUserIdIsNotUuid() throws Exception {
        mockMvc.perform(post(USERS_ENDPOINT + "/not-a-uuid/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": 100.00}"))
                .andExpect(status().isBadRequest());

        verify(depositUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN deposit use case throws UserNotFoundException WHEN POST /api/v1/users/{user_id}/deposit THEN returns 412")
    void shouldReturn412WhenDepositUserNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        when(depositUseCase.execute(any(DepositCommand.class)))
                .thenThrow(new UserNotFoundException());

        mockMvc.perform(post(USERS_ENDPOINT + "/" + userId + "/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": 100.00}"))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    @DisplayName("GIVEN no query params WHEN GET /api/v1/users THEN returns 200 with paged body and only id/full_name/type per item")
    void shouldReturnPagedListWithDefaults() throws Exception {
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();
        PagedResult<UserSummary> result = new PagedResult<>(
                List.of(
                        new UserSummary(idA, "Alice Silva", UserType.COMMON),
                        new UserSummary(idB, "Bob Souza", UserType.MERCHANT)
                ),
                0, 20, 2L, 1
        );
        when(listUsersUseCase.execute(any(PageQuery.class))).thenReturn(result);

        mockMvc.perform(get(USERS_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(idA.toString()))
                .andExpect(jsonPath("$.content[0].full_name").value("Alice Silva"))
                .andExpect(jsonPath("$.content[0].type").value("COMMON"))
                .andExpect(jsonPath("$.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.content[0].document").doesNotExist())
                .andExpect(jsonPath("$.content[0].balance").doesNotExist())
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total_elements").value(2))
                .andExpect(jsonPath("$.total_pages").value(1))
                .andExpect(jsonPath("$.last").value(true));

        ArgumentCaptor<PageQuery> captor = ArgumentCaptor.forClass(PageQuery.class);
        verify(listUsersUseCase).execute(captor.capture());
        PageQuery passed = captor.getValue();
        Assertions.assertAll(
                () -> assertThat(passed.page()).isEqualTo(0),
                () -> assertThat(passed.size()).isEqualTo(20),
                () -> assertThat(passed.sortOrders()).hasSize(1),
                () -> assertThat(passed.sortOrders().get(0).field()).isEqualTo("fullName"),
                () -> assertThat(passed.sortOrders().get(0).direction()).isEqualTo(SortDirection.ASC)
        );
    }

    @Test
    @DisplayName("GIVEN custom page/size/sort WHEN GET /api/v1/users THEN passes the parsed PageQuery to the use case")
    void shouldPassCustomQueryParamsToUseCase() throws Exception {
        when(listUsersUseCase.execute(any(PageQuery.class))).thenReturn(
                new PagedResult<>(List.of(), 1, 5, 0L, 0)
        );

        mockMvc.perform(get(USERS_ENDPOINT)
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.last").value(true));

        ArgumentCaptor<PageQuery> captor = ArgumentCaptor.forClass(PageQuery.class);
        verify(listUsersUseCase).execute(captor.capture());
        PageQuery passed = captor.getValue();
        Assertions.assertAll(
                () -> assertThat(passed.page()).isEqualTo(1),
                () -> assertThat(passed.size()).isEqualTo(5),
                () -> assertThat(passed.sortOrders().get(0).field()).isEqualTo("createdAt"),
                () -> assertThat(passed.sortOrders().get(0).direction()).isEqualTo(SortDirection.DESC)
        );
    }

}

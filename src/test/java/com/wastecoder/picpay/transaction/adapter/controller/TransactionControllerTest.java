package com.wastecoder.picpay.transaction.adapter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastecoder.picpay.transaction.adapter.controller.request.TransferRequest;
import com.wastecoder.picpay.transaction.domain.exceptions.InsufficientBalanceException;
import com.wastecoder.picpay.transaction.domain.exceptions.TransferNotAllowedException;
import com.wastecoder.picpay.transaction.domain.exceptions.UserCantTransferException;
import com.wastecoder.picpay.transaction.domain.ports.input.TransferUseCase;
import com.wastecoder.picpay.transaction.domain.viewmodels.TransferCommand;
import com.wastecoder.picpay.transaction.domain.viewmodels.TransferResult;
import com.wastecoder.picpay.user.domain.exceptions.UserNotFoundException;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    private static final String        TRANSFER_ENDPOINT = "/api/v1/users/{user_id}/transfer";
    private static final LocalDateTime SENT_DATE_DEFAULT = LocalDateTime.of(2026, 5, 3, 14, 30);
    private static final String        SENT_DATE_JSON    = "2026-05-03T14:30";
    private static final String        SUCCESS_MESSAGE   = "Transfer sent successfully.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransferUseCase transferUseCase;

    @Test
    @DisplayName("GIVEN a valid transfer payload WHEN POST /api/v1/users/{user_id}/transfer THEN returns 200 with sent_date and success message")
    void shouldReturnOkWithTransferResponseWhenPayloadIsValid() throws Exception {
        // Given
        TransferRequest request = TransferRequestMother.valid();
        when(transferUseCase.execute(any(TransferCommand.class)))
                .thenReturn(new TransferResult(SENT_DATE_DEFAULT));

        // When / Then
        mockMvc.perform(post(TRANSFER_ENDPOINT, TransferRequestMother.USER_ID_DEFAULT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent_date").value(SENT_DATE_JSON))
                .andExpect(jsonPath("$.message").value(SUCCESS_MESSAGE));

        ArgumentCaptor<TransferCommand> captor = ArgumentCaptor.forClass(TransferCommand.class);
        verify(transferUseCase).execute(captor.capture());
        TransferCommand passed = captor.getValue();
        Assertions.assertAll(
                () -> assertThat(passed.fromUserId()).isEqualTo(TransferRequestMother.USER_ID_DEFAULT),
                () -> assertThat(passed.targetUserId()).isEqualTo(TransferRequestMother.TARGET_ID_DEFAULT),
                () -> assertThat(passed.value()).isEqualTo(TransferRequestMother.VALUE_DEFAULT),
                () -> assertThat(passed.description()).isEqualTo(TransferRequestMother.DESCRIPTION_DEFAULT)
        );
    }

    @Test
    @DisplayName("GIVEN a payload with blank target_id WHEN POST /transfer THEN returns 400 and use case is not called")
    void shouldReturn400WhenTargetIdIsBlank() throws Exception {
        // Given
        TransferRequest request = TransferRequestMother.withBlankTargetId();

        // When / Then
        mockMvc.perform(post(TRANSFER_ENDPOINT, TransferRequestMother.USER_ID_DEFAULT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transferUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN a payload with value equal to zero WHEN POST /transfer THEN returns 400 and use case is not called")
    void shouldReturn400WhenValueIsZero() throws Exception {
        // Given
        TransferRequest request = TransferRequestMother.withZeroValue();

        // When / Then
        mockMvc.perform(post(TRANSFER_ENDPOINT, TransferRequestMother.USER_ID_DEFAULT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transferUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN a payload with negative value WHEN POST /transfer THEN returns 400 and use case is not called")
    void shouldReturn400WhenValueIsNegative() throws Exception {
        // Given
        TransferRequest request = TransferRequestMother.withNegativeValue();

        // When / Then
        mockMvc.perform(post(TRANSFER_ENDPOINT, TransferRequestMother.USER_ID_DEFAULT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transferUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN a payload with blank description WHEN POST /transfer THEN returns 400 and use case is not called")
    void shouldReturn400WhenDescriptionIsBlank() throws Exception {
        // Given
        TransferRequest request = TransferRequestMother.withBlankDescription();

        // When / Then
        mockMvc.perform(post(TRANSFER_ENDPOINT, TransferRequestMother.USER_ID_DEFAULT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transferUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN a non-UUID user_id in the path WHEN POST /transfer THEN returns 400 and use case is not called")
    void shouldReturn400WhenUserIdInPathIsNotUuid() throws Exception {
        // Given
        TransferRequest request = TransferRequestMother.valid();

        // When / Then
        mockMvc.perform(post(TRANSFER_ENDPOINT, "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transferUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN a non-UUID target_id in the body WHEN POST /transfer THEN returns 400 and use case is not called")
    void shouldReturn400WhenTargetIdInBodyIsNotUuid() throws Exception {
        // Given
        TransferRequest request = TransferRequestMother.withInvalidTargetId();

        // When / Then
        mockMvc.perform(post(TRANSFER_ENDPOINT, TransferRequestMother.USER_ID_DEFAULT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transferUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN both user_id and target_id are non-UUID WHEN POST /transfer THEN returns 400 and use case is not called")
    void shouldReturn400WhenBothUserIdAndTargetIdAreNotUuid() throws Exception {
        // Given
        TransferRequest request = TransferRequestMother.withInvalidTargetId();

        // When / Then
        mockMvc.perform(post(TRANSFER_ENDPOINT, "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transferUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN use case throws UserNotFoundException WHEN POST /transfer THEN returns 412")
    void shouldReturn412WhenUserNotFound() throws Exception {
        // Given
        TransferRequest request = TransferRequestMother.valid();
        when(transferUseCase.execute(any(TransferCommand.class)))
                .thenThrow(new UserNotFoundException());

        // When / Then
        mockMvc.perform(post(TRANSFER_ENDPOINT, TransferRequestMother.USER_ID_DEFAULT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    @DisplayName("GIVEN use case throws UserCantTransferException WHEN POST /transfer THEN returns 412")
    void shouldReturn412WhenUserCantTransfer() throws Exception {
        // Given
        TransferRequest request = TransferRequestMother.valid();
        when(transferUseCase.execute(any(TransferCommand.class)))
                .thenThrow(new UserCantTransferException());

        // When / Then
        mockMvc.perform(post(TRANSFER_ENDPOINT, TransferRequestMother.USER_ID_DEFAULT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    @DisplayName("GIVEN use case throws InsufficientBalanceException WHEN POST /transfer THEN returns 412")
    void shouldReturn412WhenBalanceIsInsufficient() throws Exception {
        // Given
        TransferRequest request = TransferRequestMother.valid();
        when(transferUseCase.execute(any(TransferCommand.class)))
                .thenThrow(new InsufficientBalanceException());

        // When / Then
        mockMvc.perform(post(TRANSFER_ENDPOINT, TransferRequestMother.USER_ID_DEFAULT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    @DisplayName("GIVEN use case throws TransferNotAllowedException WHEN POST /transfer THEN returns 422")
    void shouldReturn422WhenTransferIsNotAllowed() throws Exception {
        // Given
        TransferRequest request = TransferRequestMother.valid();
        when(transferUseCase.execute(any(TransferCommand.class)))
                .thenThrow(new TransferNotAllowedException());

        // When / Then
        mockMvc.perform(post(TRANSFER_ENDPOINT, TransferRequestMother.USER_ID_DEFAULT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }
}

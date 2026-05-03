package com.wastecoder.picpay.user.adapter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastecoder.picpay.user.adapter.controller.request.LoginUserRequest;
import com.wastecoder.picpay.user.domain.exceptions.IncorrectPasswordException;
import com.wastecoder.picpay.user.domain.exceptions.UserNotFoundException;
import com.wastecoder.picpay.user.domain.ports.input.LoginUserUseCase;
import com.wastecoder.picpay.user.domain.viewmodels.LoginUserCommand;
import com.wastecoder.picpay.user.domain.viewmodels.LoginUserResult;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    private static final String LOGIN_ENDPOINT      = "/api/v1/auth/login";
    private static final UUID   USER_ID_DEFAULT     = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String ACCESS_TOKEN_DEFAULT = "access-token-abc";
    private static final long   EXPIRES_IN_DEFAULT  = 3600L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoginUserUseCase loginUserUseCase;

    @Test
    @DisplayName("GIVEN valid credentials WHEN POST /api/v1/auth/login THEN returns 200 with user_id, access_token and expires_in")
    void shouldReturnOkWithTokenWhenCredentialsAreValid() throws Exception {
        // Given
        LoginUserRequest request = LoginUserRequestMother.valid();
        when(loginUserUseCase.execute(any(LoginUserCommand.class)))
                .thenReturn(new LoginUserResult(USER_ID_DEFAULT, ACCESS_TOKEN_DEFAULT, EXPIRES_IN_DEFAULT));

        // When / Then
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value(USER_ID_DEFAULT.toString()))
                .andExpect(jsonPath("$.access_token").value(ACCESS_TOKEN_DEFAULT))
                .andExpect(jsonPath("$.expires_in").value(EXPIRES_IN_DEFAULT));

        ArgumentCaptor<LoginUserCommand> captor = ArgumentCaptor.forClass(LoginUserCommand.class);
        verify(loginUserUseCase).execute(captor.capture());
        LoginUserCommand passed = captor.getValue();
        Assertions.assertAll(
                () -> assertThat(passed.email()).isEqualTo(LoginUserRequestMother.EMAIL_DEFAULT),
                () -> assertThat(passed.password()).isEqualTo(LoginUserRequestMother.PASSWORD_DEFAULT)
        );
    }

    @Test
    @DisplayName("GIVEN use case throws UserNotFoundException WHEN POST /api/v1/auth/login THEN returns 412")
    void shouldReturn412WhenUserNotFound() throws Exception {
        // Given
        LoginUserRequest request = LoginUserRequestMother.valid();
        when(loginUserUseCase.execute(any(LoginUserCommand.class)))
                .thenThrow(new UserNotFoundException());

        // When / Then
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    @DisplayName("GIVEN use case throws IncorrectPasswordException WHEN POST /api/v1/auth/login THEN returns 412")
    void shouldReturn412WhenPasswordIsIncorrect() throws Exception {
        // Given
        LoginUserRequest request = LoginUserRequestMother.valid();
        when(loginUserUseCase.execute(any(LoginUserCommand.class)))
                .thenThrow(new IncorrectPasswordException());

        // When / Then
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    @DisplayName("GIVEN a payload with blank email WHEN POST /api/v1/auth/login THEN returns 400 and use case is not called")
    void shouldReturn400WhenEmailIsBlank() throws Exception {
        // Given
        LoginUserRequest request = LoginUserRequestMother.withBlankEmail();

        // When / Then
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(loginUserUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN a payload with blank password WHEN POST /api/v1/auth/login THEN returns 400 and use case is not called")
    void shouldReturn400WhenPasswordIsBlank() throws Exception {
        // Given
        LoginUserRequest request = LoginUserRequestMother.withBlankPassword();

        // When / Then
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(loginUserUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN a payload with both email and password blank WHEN POST /api/v1/auth/login THEN returns 400 and use case is not called")
    void shouldReturn400WhenBothEmailAndPasswordAreBlank() throws Exception {
        // Given
        LoginUserRequest request = LoginUserRequestMother.withBothBlank();

        // When / Then
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(loginUserUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN an empty JSON body WHEN POST /api/v1/auth/login THEN returns 400 and use case is not called")
    void shouldReturn400WhenBodyIsEmpty() throws Exception {
        // When / Then
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(loginUserUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GIVEN a malformed JSON body WHEN POST /api/v1/auth/login THEN returns 400 and use case is not called")
    void shouldReturn400WhenBodyIsMalformedJson() throws Exception {
        // When / Then
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest());

        verify(loginUserUseCase, never()).execute(any());
    }
}

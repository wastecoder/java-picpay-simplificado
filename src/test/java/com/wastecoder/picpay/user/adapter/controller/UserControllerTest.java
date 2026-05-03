package com.wastecoder.picpay.user.adapter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastecoder.picpay.user.UserMother;
import com.wastecoder.picpay.user.adapter.controller.request.CreateUserRequest;
import com.wastecoder.picpay.user.domain.enums.UserType;
import com.wastecoder.picpay.user.domain.exceptions.DocumentAlreadyRegisteredException;
import com.wastecoder.picpay.user.domain.exceptions.EmailAlreadyRegisteredException;
import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.input.CreateUserUseCase;
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
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
}

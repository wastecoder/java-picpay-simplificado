package com.wastecoder.picpay.user.adapter.controller;

import com.wastecoder.picpay.user.adapter.controller.request.LoginUserRequest;
import com.wastecoder.picpay.user.adapter.controller.response.LoginUserResponse;
import com.wastecoder.picpay.user.domain.ports.input.LoginUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth Endpoints")
public class AuthController {

    private final LoginUserUseCase loginUserUseCase;

    public AuthController(LoginUserUseCase loginUserUseCase) {
        this.loginUserUseCase = loginUserUseCase;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login user",
            description = "On success returns OK status code with access token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Auth successfully.")
    })
    public ResponseEntity<LoginUserResponse> loginUser(
            @Valid @RequestBody LoginUserRequest request
    ) {
        final var result = loginUserUseCase.execute(request.toCommand());

        final var response = new LoginUserResponse(result);
        return ResponseEntity.ok(response);
    }
}

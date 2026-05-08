package com.wastecoder.picpay.user.adapter.controller;

import com.wastecoder.picpay.user.adapter.controller.request.CreateUserRequest;
import com.wastecoder.picpay.user.adapter.controller.request.DepositRequest;
import com.wastecoder.picpay.user.adapter.controller.response.DepositResponse;
import com.wastecoder.picpay.user.domain.ports.input.CreateUserUseCase;
import com.wastecoder.picpay.user.domain.ports.input.DepositUseCase;
import com.wastecoder.picpay.user.domain.viewmodels.DepositResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Endpoints")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final DepositUseCase depositUseCase;

    public UserController(CreateUserUseCase createUserUseCase, DepositUseCase depositUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.depositUseCase = depositUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Create user",
            description = "On success returns 201 status code."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully.")
    })
    public ResponseEntity<Void> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        var createdUser = createUserUseCase.execute(request.toModel());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdUser.id().toString())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @PostMapping("/{user_id}/deposit")
    @Operation(
            summary = "Deposit funds into user balance",
            description = "On success returns 200 status code with the updated balance."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deposit applied successfully.")
    })
    public ResponseEntity<DepositResponse> deposit(
            @PathVariable("user_id") String userId,
            @Valid @RequestBody DepositRequest request
    ) {
        DepositResult result = depositUseCase.execute(request.toCommand(userId));

        return ResponseEntity.ok(new DepositResponse(
                result.userId(),
                result.newBalance(),
                result.depositedAt()
        ));
    }
}

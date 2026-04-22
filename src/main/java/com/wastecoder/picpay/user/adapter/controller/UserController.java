package com.wastecoder.picpay.user.adapter.controller;

import com.wastecoder.picpay.user.adapter.controller.request.CreateUserRequest;
import com.wastecoder.picpay.user.domain.ports.input.CreateUserUseCase;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Endpoints")
public class UserController {

    private final CreateUserUseCase createUserUseCase;

    public UserController(CreateUserUseCase createUserUseCase) {
        this.createUserUseCase = createUserUseCase;
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
}

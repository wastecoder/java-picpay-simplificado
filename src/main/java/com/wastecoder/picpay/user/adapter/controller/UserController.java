package com.wastecoder.picpay.user.adapter.controller;

import com.wastecoder.picpay.common.adapter.controller.response.PageResponse;
import com.wastecoder.picpay.common.domain.viewmodels.PageQuery;
import com.wastecoder.picpay.common.domain.viewmodels.PagedResult;
import com.wastecoder.picpay.common.domain.viewmodels.SortDirection;
import com.wastecoder.picpay.common.domain.viewmodels.SortOrder;
import com.wastecoder.picpay.user.adapter.controller.request.CreateUserRequest;
import com.wastecoder.picpay.user.adapter.controller.request.DepositRequest;
import com.wastecoder.picpay.user.adapter.controller.response.DepositResponse;
import com.wastecoder.picpay.user.adapter.controller.response.UserResponse;
import com.wastecoder.picpay.user.adapter.controller.response.UserSummaryResponse;
import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.input.CreateUserUseCase;
import com.wastecoder.picpay.user.domain.ports.input.DepositUseCase;
import com.wastecoder.picpay.user.domain.ports.input.GetUserByIdUseCase;
import com.wastecoder.picpay.user.domain.ports.input.ListUsersUseCase;
import com.wastecoder.picpay.user.domain.viewmodels.DepositResult;
import com.wastecoder.picpay.user.domain.viewmodels.UserSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static com.wastecoder.picpay.common.domain.utils.UuidUtils.uuidCustomValueOf;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Endpoints")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final DepositUseCase depositUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;

    public UserController(
            CreateUserUseCase createUserUseCase,
            DepositUseCase depositUseCase,
            ListUsersUseCase listUsersUseCase,
            GetUserByIdUseCase getUserByIdUseCase
    ) {
        this.createUserUseCase = createUserUseCase;
        this.depositUseCase = depositUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
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

    @GetMapping("/{user_id}")
    @Operation(
            summary = "Get user by id",
            description = "On success returns 200 with the full user payload (without password)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found."),
            @ApiResponse(responseCode = "404", description = "User not found.")
    })
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable("user_id") String userId
    ) {
        User user = getUserByIdUseCase.execute(uuidCustomValueOf(userId, "user_id"));
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @GetMapping
    @Operation(
            summary = "List users (paginated)",
            description = "Returns a paginated list of users exposing only id, full_name and type."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page returned successfully.")
    })
    public ResponseEntity<PageResponse<UserSummaryResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName,asc") String sort
    ) {
        String[] parts = sort.split(",", 2);
        PageQuery query = new PageQuery(page, size, List.of(
                new SortOrder(parts[0], SortDirection.valueOf(parts[1].toUpperCase()))
        ));
        PagedResult<UserSummary> result = listUsersUseCase.execute(query);
        return ResponseEntity.ok(PageResponse.from(result, UserSummaryResponse::from));
    }
}

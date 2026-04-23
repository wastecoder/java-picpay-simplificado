package com.wastecoder.picpay.transaction.adapter.controller;

import com.wastecoder.picpay.transaction.adapter.controller.request.TransferRequest;
import com.wastecoder.picpay.transaction.adapter.controller.response.TransferResponse;
import com.wastecoder.picpay.transaction.domain.ports.input.TransferUseCase;
import com.wastecoder.picpay.transaction.domain.viewmodels.TransferResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{user_id}")
@Tag(name = "Transaction Endpoints")
@SecurityRequirement(name = "Bearer Token")
public class TransactionController {

    private final TransferUseCase transferUseCase;

    public TransactionController(TransferUseCase transferUseCase) {
        this.transferUseCase = transferUseCase;
    }

    @PostMapping("/transfer")
    @Operation(
            summary = "Send money transfer",
            description = "On success returns 200 status code."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transfer sent successfully.")
    })
    public ResponseEntity<TransferResponse> transfer(
            @PathVariable("user_id") String userId,
            @Valid @RequestBody TransferRequest request
    ) {
        TransferResult result = transferUseCase.execute(request.toCommand(userId));

        return ResponseEntity.ok(new TransferResponse(
                result.sentDate(),
                "Transfer sent successfully."
        ));
    }
}

package com.wastecoder.picpay.transaction.usecases;

import com.wastecoder.picpay.transaction.domain.exceptions.InsufficientBalanceException;
import com.wastecoder.picpay.transaction.domain.exceptions.TransferNotAllowedException;
import com.wastecoder.picpay.transaction.domain.exceptions.UserCantTransferException;
import com.wastecoder.picpay.transaction.domain.model.Transaction;
import com.wastecoder.picpay.transaction.domain.ports.input.TransferUseCase;
import com.wastecoder.picpay.transaction.domain.ports.output.TransactionRepository;
import com.wastecoder.picpay.transaction.domain.ports.output.TransferValidationGateway;
import com.wastecoder.picpay.transaction.domain.viewmodels.TransferCommand;
import com.wastecoder.picpay.transaction.domain.viewmodels.TransferResult;
import com.wastecoder.picpay.transaction.domain.viewmodels.TransferValidationResult;
import com.wastecoder.picpay.user.domain.enums.UserType;
import com.wastecoder.picpay.user.domain.exceptions.UserNotFoundException;
import com.wastecoder.picpay.user.domain.model.User;
import com.wastecoder.picpay.user.domain.ports.output.NotifyUserGateway;
import com.wastecoder.picpay.user.domain.ports.output.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class TransferUseCaseImpl implements TransferUseCase {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final TransferValidationGateway transferValidationGateway;
    private final NotifyUserGateway notifyUserGateway;

    public TransferUseCaseImpl(
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            TransferValidationGateway transferValidationGateway,
            NotifyUserGateway notifyUserGateway
    ) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.transferValidationGateway = transferValidationGateway;
        this.notifyUserGateway = notifyUserGateway;
    }

    @Override
    @Transactional
    public TransferResult execute(TransferCommand command) {
        BigDecimal transactionValue = command.value();

        User fromUser = userRepository.findById(command.fromUserId())
                .orElseThrow(UserNotFoundException::new);

        if (fromUser.type() == UserType.MERCHANT) {
            throw new UserCantTransferException();
        }

        if (fromUser.balance().compareTo(transactionValue) < 0) {
            throw new InsufficientBalanceException();
        }

        User targetUser = userRepository.findById(command.targetUserId())
                .orElseThrow(() -> new UserNotFoundException("Target not found."));

        Transaction transaction = new Transaction(
                fromUser,
                targetUser,
                transactionValue,
                command.description()
        );

        if (transferValidationGateway.validate(transaction) == TransferValidationResult.DENIED) {
            throw new TransferNotAllowedException();
        }

        userRepository.updateBalanceWithMinusOperation(fromUser, transactionValue);
        userRepository.updateBalanceWithPlusOperation(targetUser, transactionValue);
        transactionRepository.storage(transaction);

        notifyUserGateway.notify(
                targetUser,
                "Transferência recebida com sucesso",
                String.format(
                        "Você recebeu uma transferência de R$ %s enviada por %s",
                        command.value().setScale(2, RoundingMode.HALF_UP),
                        fromUser.fullName()
                )
        );

        return new TransferResult(LocalDateTime.now());
    }
}

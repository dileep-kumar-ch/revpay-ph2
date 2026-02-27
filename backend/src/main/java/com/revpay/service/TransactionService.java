package com.revpay.service;

import com.revpay.dto.SendMoneyRequest;
import com.revpay.dto.NotificationEvent;
import com.revpay.dto.TransactionResponse;
import com.revpay.model.*;
import com.revpay.repository.TransactionRepository;
import com.revpay.repository.UserRepository;
import com.revpay.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @Transactional
        public TransactionResponse sendMoney(String senderUsername, SendMoneyRequest request) {
                log.info("Initiating transfer from {} to {} amount {}", senderUsername, request.getReceiverUsername(),
                                request.getAmount());
                User sender = userRepository.findByUsername(senderUsername)
                                .or(() -> userRepository.findByEmail(senderUsername))
                                .orElseThrow(() -> new UsernameNotFoundException("Sender not found"));

                User receiver = userRepository.findByUsername(request.getReceiverUsername())
                                .or(() -> userRepository.findByEmail(request.getReceiverUsername()))
                                .orElseThrow(() -> new RuntimeException("Receiver not found"));

                if (sender.getId().equals(receiver.getId())) {
                        throw new RuntimeException("Cannot send money to yourself");
                }

                Wallet senderWallet = walletRepository.findByUser(sender)
                                .orElseThrow(() -> new RuntimeException("Sender wallet not found"));

                Wallet receiverWallet = walletRepository.findByUser(receiver)
                                .orElseThrow(() -> new RuntimeException("Receiver wallet not found"));

                if (senderWallet.getBalance().compareTo(request.getAmount()) < 0) {
                        throw new RuntimeException("Insufficient balance");
                }

                // Deduct from sender
                senderWallet.setBalance(senderWallet.getBalance().subtract(request.getAmount()));
                walletRepository.save(senderWallet);

                // Add to receiver
                receiverWallet.setBalance(receiverWallet.getBalance().add(request.getAmount()));
                walletRepository.save(receiverWallet);

                // Create Transaction Record
                Transaction transaction = Transaction.builder()
                                .senderWallet(senderWallet)
                                .receiverWallet(receiverWallet)
                                .amount(request.getAmount())
                                .type(TransactionType.SEND)
                                .status(TransactionStatus.COMPLETED)
                                .description(request.getNote())
                                .timestamp(LocalDateTime.now())
                                .build();

                Transaction savedTransaction = transactionRepository.save(transaction);

                String receiverCounterparty = receiver.getEmail() != null ? receiver.getEmail() : receiver.getUsername();
                String senderCounterparty = sender.getEmail() != null ? sender.getEmail() : sender.getUsername();

                notificationEventPublisher.publish(NotificationEvent.builder()
                                .recipientUserId(sender.getId())
                                .category(NotificationCategory.TRANSACTIONS)
                                .type("MONEY_SENT")
                                .title("Money sent")
                                .message("You sent $" + request.getAmount() + " to " + receiverCounterparty + ".")
                                .metadata(Map.of(
                                                "transactionId", savedTransaction.getId(),
                                                "amount", request.getAmount(),
                                                "counterparty", receiverCounterparty,
                                                "status", savedTransaction.getStatus().name(),
                                                "navigation", "/transactions/" + savedTransaction.getId(),
                                                "eventTime", savedTransaction.getTimestamp().toString()))
                                .build());

                notificationEventPublisher.publish(NotificationEvent.builder()
                                .recipientUserId(receiver.getId())
                                .category(NotificationCategory.TRANSACTIONS)
                                .type("MONEY_RECEIVED")
                                .title("Money received")
                                .message("You received $" + request.getAmount() + " from " + senderCounterparty + ".")
                                .metadata(Map.of(
                                                "transactionId", savedTransaction.getId(),
                                                "amount", request.getAmount(),
                                                "counterparty", senderCounterparty,
                                                "status", savedTransaction.getStatus().name(),
                                                "navigation", "/transactions/" + savedTransaction.getId(),
                                                "eventTime", savedTransaction.getTimestamp().toString()))
                                .build());
        
                return mapToResponse(savedTransaction, senderWallet);
        }

        public List<TransactionResponse> getTransactionHistory(String username) {
                User user = userRepository.findByUsername(username)
                                .or(() -> userRepository.findByEmail(username))
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                Wallet wallet = walletRepository.findByUser(user)
                                .orElseThrow(() -> new RuntimeException("Wallet not found"));

                return transactionRepository.findAllByWallet(wallet).stream()
                                .map(transaction -> mapToResponse(transaction, wallet))
                                .collect(Collectors.toList());
        }

        private TransactionResponse mapToResponse(Transaction transaction, Wallet contextWallet) {
                TransactionType effectiveType = transaction.getType();
                if (transaction.getType() == TransactionType.SEND
                                && transaction.getReceiverWallet() != null
                                && transaction.getReceiverWallet().getId().equals(contextWallet.getId())) {
                        effectiveType = TransactionType.RECEIVE;
                }

                return TransactionResponse.builder()
                                .id(transaction.getId())
                                .senderName(transaction.getSenderWallet() != null
                                                ? transaction.getSenderWallet().getUser().getFullName()
                                                : "System")
                                .receiverName(transaction.getReceiverWallet() != null
                                                ? transaction.getReceiverWallet().getUser().getFullName()
                                                : "System")
                                .amount(transaction.getAmount())
                                .type(effectiveType)
                                .status(transaction.getStatus())
                                .description(transaction.getDescription())
                                .timestamp(transaction.getTimestamp())
                                .build();
        }
}

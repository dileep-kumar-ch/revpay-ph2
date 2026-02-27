package com.revpay.service;

import com.revpay.dto.WalletResponse;
import com.revpay.model.*;
import com.revpay.repository.TransactionRepository;
import com.revpay.repository.UserRepository;
import com.revpay.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

        private final WalletRepository walletRepository;
        private final UserRepository userRepository;
        private final TransactionRepository transactionRepository;

        public WalletResponse getBalance(String username) {
                User user = userRepository.findByUsername(username)
                                .or(() -> userRepository.findByEmail(username))
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                Wallet wallet = walletRepository.findByUser(user)
                                .orElseGet(() -> {
                                        log.info("Wallet not found for user: {}. Creating a new one.", username);
                                        Wallet newWallet = Wallet.builder()
                                                        .user(user)
                                                        .balance(BigDecimal.ZERO)
                                                        .currency("USD")
                                                        .build();
                                        return walletRepository.save(newWallet);
                                });
                return mapToResponse(wallet);
        }

        @Transactional
        public WalletResponse addFunds(String username, BigDecimal amount, Long cardId) {
                log.info("Adding {} to wallet for user: {} using card: {}", amount, username, cardId);
                User user = userRepository.findByUsername(username)
                                .or(() -> userRepository.findByEmail(username))
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                Wallet wallet = walletRepository.findByUser(user)
                                .orElseThrow(() -> new RuntimeException("Wallet not found"));

                wallet.setBalance(wallet.getBalance().add(amount));
                Wallet savedWallet = walletRepository.save(wallet);

                // Record Transaction
                Transaction transaction = Transaction.builder()
                                .receiverWallet(wallet)
                                .amount(amount)
                                .type(TransactionType.DEPOSIT)
                                .status(TransactionStatus.COMPLETED)
                                .description("Funded wallet using card ID: " + cardId)
                                .timestamp(LocalDateTime.now())
                                .build();
                transactionRepository.save(transaction);

                return mapToResponse(savedWallet);
        }

        @Transactional
        public WalletResponse withdrawFunds(String username, BigDecimal amount) {
                log.info("Withdrawing {} from wallet for user: {}", amount, username);
                User user = userRepository.findByUsername(username)
                                .or(() -> userRepository.findByEmail(username))
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                Wallet wallet = walletRepository.findByUser(user)
                                .orElseThrow(() -> new RuntimeException("Wallet not found"));

                if (wallet.getBalance().compareTo(amount) < 0) {
                        throw new RuntimeException("Insufficient balance");
                }

                wallet.setBalance(wallet.getBalance().subtract(amount));
                Wallet savedWallet = walletRepository.save(wallet);

                // Record Transaction
                Transaction transaction = Transaction.builder()
                                .senderWallet(wallet)
                                .amount(amount)
                                .type(TransactionType.WITHDRAWAL)
                                .status(TransactionStatus.COMPLETED)
                                .description("Withdrawn to primary card")
                                .timestamp(LocalDateTime.now())
                                .build();
                transactionRepository.save(transaction);

                return mapToResponse(savedWallet);
        }

        private WalletResponse mapToResponse(Wallet wallet) {
                return WalletResponse.builder()
                                .id(wallet.getId())
                                .balance(wallet.getBalance())
                                .currency(wallet.getCurrency())
                                .build();
        }
}

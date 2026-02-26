package com.revpay.service;

import com.revpay.dto.*;
import com.revpay.model.BusinessBankAccount;
import com.revpay.model.NotificationCategory;
import com.revpay.model.Role;
import com.revpay.model.User;
import com.revpay.repository.BusinessBankAccountRepository;
import com.revpay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessPaymentMethodService {
    private final UserRepository userRepository;
    private final CardService cardService;
    private final BusinessBankAccountRepository bankAccountRepository;
    private final EncryptionService encryptionService;
    private final NotificationEventPublisher notificationEventPublisher;

    @Transactional
    public CardResponse addBusinessCard(String principal, AddCardRequest request) {
        User user = getBusinessUser(principal);
        validateCard(request);
        CardResponse response = cardService.addCard(principal, request);
        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(user.getId())
                .category(NotificationCategory.ALERTS)
                .type("BUSINESS_PAYMENT_METHOD_ADDED")
                .title("Payment method added")
                .message("Business card ending " + response.getLastFourDigits() + " has been added.")
                .metadata(Map.of("paymentMethodType", "CARD", "id", response.getId()))
                .build());
        return response;
    }

    @Transactional
    public void removeBusinessCard(String principal, Long cardId) {
        User user = getBusinessUser(principal);
        cardService.deleteCard(principal, cardId);
        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(user.getId())
                .category(NotificationCategory.ALERTS)
                .type("BUSINESS_PAYMENT_METHOD_REMOVED")
                .title("Payment method removed")
                .message("Business card has been removed.")
                .metadata(Map.of("paymentMethodType", "CARD", "id", cardId))
                .build());
    }

    @Transactional
    public BankAccountResponse addBankAccount(String principal, BankAccountRequest request) {
        User user = getBusinessUser(principal);
        validateBankAccount(request);

        boolean makeDefault = Boolean.TRUE.equals(request.getSetAsDefault());
        BusinessBankAccount account = BusinessBankAccount.builder()
                .businessUser(user)
                .bankName(request.getBankName())
                .accountHolderName(request.getAccountHolderName())
                .encryptedAccountNumber(encryptionService.encrypt(request.getAccountNumber()))
                .accountLastFour(request.getAccountNumber().substring(request.getAccountNumber().length() - 4))
                .encryptedRoutingNumber(encryptionService.encrypt(request.getRoutingNumber()))
                .accountType(request.getAccountType().toUpperCase())
                .isDefault(makeDefault)
                .build();

        if (makeDefault) {
            bankAccountRepository.clearDefaultForUser(user.getId());
            account.setDefault(true);
        } else if (!bankAccountRepository.existsByBusinessUserAndIsDefaultTrue(user)) {
            account.setDefault(true);
        }

        BusinessBankAccount saved = bankAccountRepository.save(account);
        BankAccountResponse response = mapToBankResponse(saved);

        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(user.getId())
                .category(NotificationCategory.ALERTS)
                .type("BUSINESS_PAYMENT_METHOD_ADDED")
                .title("Payment method added")
                .message("Bank account ending " + response.getAccountLastFour() + " has been added.")
                .metadata(Map.of("paymentMethodType", "BANK_ACCOUNT", "id", response.getId()))
                .build());
        return response;
    }

    public List<BankAccountResponse> listBankAccounts(String principal) {
        User user = getBusinessUser(principal);
        return bankAccountRepository.findByBusinessUser(user).stream()
                .map(this::mapToBankResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeBankAccount(String principal, Long bankAccountId) {
        User user = getBusinessUser(principal);
        BusinessBankAccount account = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new RuntimeException("Bank account not found"));
        if (!account.getBusinessUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to delete this bank account");
        }

        boolean wasDefault = account.isDefault();
        bankAccountRepository.delete(account);
        if (wasDefault) {
            bankAccountRepository.findByBusinessUser(user).stream().findFirst().ifPresent(next -> {
                next.setDefault(true);
                bankAccountRepository.save(next);
            });
        }

        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(user.getId())
                .category(NotificationCategory.ALERTS)
                .type("BUSINESS_PAYMENT_METHOD_REMOVED")
                .title("Payment method removed")
                .message("Bank account has been removed.")
                .metadata(Map.of("paymentMethodType", "BANK_ACCOUNT", "id", bankAccountId))
                .build());
    }

    private User getBusinessUser(String principal) {
        User user = userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (user.getRole() != Role.BUSINESS) {
            throw new RuntimeException("Business role required");
        }
        return user;
    }

    private BankAccountResponse mapToBankResponse(BusinessBankAccount account) {
        return BankAccountResponse.builder()
                .id(account.getId())
                .bankName(account.getBankName())
                .accountHolderName(account.getAccountHolderName())
                .accountLastFour(account.getAccountLastFour())
                .accountType(account.getAccountType())
                .defaultAccount(account.isDefault())
                .build();
    }

    private void validateCard(AddCardRequest request) {
        if (request.getCardNumber() == null || !request.getCardNumber().matches("\\d{12,19}")) {
            throw new RuntimeException("Invalid card number");
        }
        if (request.getCvv() == null || !request.getCvv().matches("\\d{3,4}")) {
            throw new RuntimeException("Invalid CVV");
        }
        if (request.getExpiryDate() == null || !request.getExpiryDate().matches("(0[1-9]|1[0-2])/\\d{2}")) {
            throw new RuntimeException("Invalid expiry date. Use MM/YY");
        }
    }

    private void validateBankAccount(BankAccountRequest request) {
        if (request.getAccountNumber() == null || !request.getAccountNumber().matches("\\d{8,20}")) {
            throw new RuntimeException("Invalid account number");
        }
        if (request.getRoutingNumber() == null || !request.getRoutingNumber().matches("\\d{9,12}")) {
            throw new RuntimeException("Invalid routing number");
        }
    }
}

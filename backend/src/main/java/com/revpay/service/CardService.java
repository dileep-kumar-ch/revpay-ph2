package com.revpay.service;

import com.revpay.dto.AddCardRequest;
import com.revpay.dto.CardResponse;
import com.revpay.dto.NotificationEvent;
import com.revpay.model.Card;
import com.revpay.model.NotificationCategory;
import com.revpay.model.User;
import com.revpay.repository.CardRepository;
import com.revpay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final NotificationEventPublisher notificationEventPublisher;

    @Transactional
    public CardResponse addCard(String username, AddCardRequest request) {
        log.info("Adding card for user: {}", username);
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        boolean makeDefault = Boolean.TRUE.equals(request.getSetAsDefault());
        Card.PaymentMethodType paymentMethodType = parsePaymentMethodType(request.getPaymentMethodType());

        Card.CardType type = detectCardType(request.getCardNumber());

        Card card = Card.builder()
                .user(user)
                .cardHolderName(request.getCardHolderName())
                .encryptedCardNumber(encryptionService.encrypt(request.getCardNumber()))
                .lastFourDigits(request.getCardNumber().substring(request.getCardNumber().length() - 4))
                .expiryDate(request.getExpiryDate())
                .encryptedCvv(encryptionService.encrypt(request.getCvv()))
                .cardType(type)
                .paymentMethodType(paymentMethodType)
                .isDefault(makeDefault)
                .build();

        if (makeDefault) {
            cardRepository.clearDefaultForUser(user.getId());
            card.setDefault(true);
        } else if (!cardRepository.existsByUserAndIsDefaultTrue(user)) {
            card.setDefault(true);
        }

        CardResponse response = mapToResponse(cardRepository.save(card));
        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(user.getId())
                .category(NotificationCategory.ALERTS)
                .type("CARD_ADDED")
                .title("Card added")
                .message("Card ending " + response.getLastFourDigits() + " was added.")
                .metadata(Map.of(
                        "cardId", response.getId(),
                        "lastFourDigits", response.getLastFourDigits(),
                        "navigation", "/payment-methods"))
                .build());
        return response;
    }

    public List<CardResponse> getMyCards(String username) {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return cardRepository.findByUser(user).stream()
                .sorted(Comparator.comparing(Card::isDefault).reversed())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CardResponse setDefaultCard(String username, Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        String principal = username == null ? "" : username.trim();
        String ownerUsername = card.getUser().getUsername() == null ? "" : card.getUser().getUsername().trim();
        String ownerEmail = card.getUser().getEmail() == null ? "" : card.getUser().getEmail().trim();
        if (!ownerUsername.equalsIgnoreCase(principal) && !ownerEmail.equalsIgnoreCase(principal)) {
            throw new RuntimeException("Not authorized to update this card");
        }

        if (card.isDefault()) {
            return mapToResponse(card);
        }

        cardRepository.clearDefaultForUser(card.getUser().getId());
        card.setDefault(true);
        CardResponse response = mapToResponse(cardRepository.save(card));
        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(card.getUser().getId())
                .category(NotificationCategory.ALERTS)
                .type("CARD_SET_DEFAULT")
                .title("Default card updated")
                .message("Card ending " + response.getLastFourDigits() + " is now your default card.")
                .metadata(Map.of(
                        "cardId", response.getId(),
                        "lastFourDigits", response.getLastFourDigits(),
                        "navigation", "/payment-methods"))
                .build());
        return response;
    }



    @Transactional
    public void deleteCard(String username, Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        String principal = username == null ? "" : username.trim();
        String ownerUsername = card.getUser().getUsername() == null ? "" : card.getUser().getUsername().trim();
        String ownerEmail = card.getUser().getEmail() == null ? "" : card.getUser().getEmail().trim();
        if (!ownerUsername.equalsIgnoreCase(principal) && !ownerEmail.equalsIgnoreCase(principal)) {
            throw new RuntimeException("Not authorized to delete this card");
        }

        boolean wasDefault = card.isDefault();
        User user = card.getUser();
        String lastFourDigits = card.getLastFourDigits();
        cardRepository.delete(card);

        if (wasDefault) {
            cardRepository.findByUser(user).stream().findFirst().ifPresent(nextCard -> {
                nextCard.setDefault(true);
                cardRepository.save(nextCard);
            });
        }

        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(user.getId())
                .category(NotificationCategory.ALERTS)
                .type("CARD_REMOVED")
                .title("Card removed")
                .message("Card ending " + lastFourDigits + " was removed.")
                .metadata(Map.of(
                        "lastFourDigits", lastFourDigits,
                        "navigation", "/payment-methods"))
                .build());
    }

    private Card.CardType detectCardType(String cardNumber) {
        if (cardNumber.startsWith("4"))
            return Card.CardType.VISA;
        if (cardNumber.startsWith("5"))
            return Card.CardType.MASTERCARD;
        if (cardNumber.startsWith("3"))
            return Card.CardType.AMEX;
        if (cardNumber.startsWith("6"))
            return Card.CardType.DISCOVER;
        return Card.CardType.OTHER;
    }

    private CardResponse mapToResponse(Card card) {
        return CardResponse.builder()
                .id(card.getId())
                .cardHolderName(card.getCardHolderName())
                .lastFourDigits(card.getLastFourDigits())
                .expiryDate(card.getExpiryDate())
                .cardType(card.getCardType().name())
                .paymentMethodType(card.getPaymentMethodType().name())
                .defaultCard(card.isDefault())
                .build();
    }

    private Card.PaymentMethodType parsePaymentMethodType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return Card.PaymentMethodType.DEBIT;
        }
        try {
            return Card.PaymentMethodType.valueOf(rawType.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid payment method type. Use DEBIT or CREDIT.");
        }
    }
}

package com.revpay.service;

import com.revpay.dto.CreateMoneyRequestDto;
import com.revpay.dto.MoneyRequestResponse;
import com.revpay.dto.NotificationEvent;
import com.revpay.dto.SendMoneyRequest;
import com.revpay.model.*;
import com.revpay.repository.MoneyRequestRepository;
import com.revpay.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MoneyRequestService {

    private final MoneyRequestRepository moneyRequestRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final NotificationEventPublisher notificationEventPublisher;

    public MoneyRequestService(MoneyRequestRepository moneyRequestRepository,
            UserRepository userRepository,
            TransactionService transactionService,
            NotificationEventPublisher notificationEventPublisher) {
        this.moneyRequestRepository = moneyRequestRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    @Transactional
    public MoneyRequest createRequest(String requesterUsername, CreateMoneyRequestDto dto) {
        User requester = userRepository.findByUsername(requesterUsername)
                .or(() -> userRepository.findByEmail(requesterUsername))
                .orElseThrow(() -> new UsernameNotFoundException("Requester not found"));

        User payer = userRepository.findByUsername(dto.getPayerUsername()) // Assuming DTO has payerUsername
                .or(() -> userRepository.findByEmail(dto.getPayerUsername()))
                .orElseThrow(() -> new RuntimeException("Payer not found"));

        if (requester.getId().equals(payer.getId())) {
            throw new RuntimeException("Cannot request money from yourself");
        }

        MoneyRequest request = MoneyRequest.builder()
                .requester(requester)
                .payer(payer)
                .amount(dto.getAmount())
                .note(dto.getNote())
                .status(RequestStatus.PENDING)
                .build();

        MoneyRequest savedRequest = moneyRequestRepository.save(request);

        String requesterCounterparty = payer.getEmail() != null ? payer.getEmail() : payer.getUsername();
        String payerCounterparty = requester.getEmail() != null ? requester.getEmail() : requester.getUsername();

        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(requester.getId())
                .category(NotificationCategory.REQUESTS)
                .type("MONEY_REQUEST_SENT")
                .title("Money request sent")
                .message("Money request sent to " + requesterCounterparty + " for $" + dto.getAmount())
                .metadata(Map.of(
                        "requestId", savedRequest.getId(),
                        "amount", dto.getAmount(),
                        "counterparty", requesterCounterparty,
                        "status", RequestStatus.PENDING.name(),
                        "navigation", "/requests/" + savedRequest.getId(),
                        "eventTime", savedRequest.getCreatedAt().toString()))
                .build());

        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(payer.getId())
                .category(NotificationCategory.REQUESTS)
                .type("MONEY_REQUEST_RECEIVED")
                .title("Money requested from you")
                .message("Money requested from " + payerCounterparty + " for $" + dto.getAmount())
                .metadata(Map.of(
                        "requestId", savedRequest.getId(),
                        "amount", dto.getAmount(),
                        "counterparty", payerCounterparty,
                        "status", RequestStatus.PENDING.name(),
                        "navigation", "/requests/" + savedRequest.getId(),
                        "eventTime", savedRequest.getCreatedAt().toString()))
                .build());

        return savedRequest;
    }

    public List<MoneyRequestResponse> getMyRequests(String username) {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return moneyRequestRepository.findRequestRowsByUserId(user.getId()).stream()
                .map(this::mapRowToResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional
    public void respondToRequest(String username, Long requestId, boolean accept) {
        MoneyRequest request = moneyRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!request.getPayer().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to respond to this request");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Request is already " + request.getStatus());
        }

        if (accept) {
            // Initiate Transaction
            SendMoneyRequest sendRequest = SendMoneyRequest.builder()
                    .receiverUsername(request.getRequester().getUsername())
                    .amount(request.getAmount())
                    .note("Payment for request: " + request.getNote())
                    .build();

            transactionService.sendMoney(user.getUsername(), sendRequest);
            request.setStatus(RequestStatus.ACCEPTED);
        } else {
            request.setStatus(RequestStatus.DECLINED);
        }

        moneyRequestRepository.save(request);

        // Notify requester of response
        String status = accept ? "accepted" : "declined";
        String payerCounterparty = user.getEmail() != null ? user.getEmail() : user.getUsername();
        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(request.getRequester().getId())
                .category(NotificationCategory.REQUESTS)
                .type(accept ? "MONEY_REQUEST_ACCEPTED" : "MONEY_REQUEST_DECLINED")
                .title(accept ? "Money request accepted" : "Money request declined")
                .message("Your request to " + payerCounterparty + " was " + status + ".")
                .metadata(Map.of(
                        "requestId", request.getId(),
                        "amount", request.getAmount(),
                        "counterparty", payerCounterparty,
                        "status", request.getStatus().name(),
                        "navigation", "/requests/" + request.getId(),
                        "eventTime", LocalDateTime.now().toString()))
                .build());
    }

    private MoneyRequestResponse mapRowToResponse(Object[] row) {
        try {
            Long id = row[0] != null ? ((Number) row[0]).longValue() : null;

            MoneyRequestResponse.UserSummary requester = MoneyRequestResponse.UserSummary.builder()
                    .username(row[1] != null ? String.valueOf(row[1]) : "")
                    .fullName(row[2] != null ? String.valueOf(row[2]) : "")
                    .email(row[3] != null ? String.valueOf(row[3]) : "")
                    .build();

            MoneyRequestResponse.UserSummary payer = MoneyRequestResponse.UserSummary.builder()
                    .username(row[4] != null ? String.valueOf(row[4]) : "")
                    .fullName(row[5] != null ? String.valueOf(row[5]) : "")
                    .email(row[6] != null ? String.valueOf(row[6]) : "")
                    .build();

            java.math.BigDecimal amount = row[7] != null ? (java.math.BigDecimal) row[7] : java.math.BigDecimal.ZERO;
            String note = row[8] != null ? String.valueOf(row[8]) : "";
            RequestStatus status = row[9] != null ? RequestStatus.valueOf(String.valueOf(row[9])) : RequestStatus.PENDING;
            java.time.LocalDateTime createdAt = toLocalDateTime(row[10]);
            String direction = row[11] != null ? String.valueOf(row[11]) : "OUTGOING";

            return MoneyRequestResponse.builder()
                    .id(id)
                    .requester(requester)
                    .payer(payer)
                    .amount(amount)
                    .note(note)
                    .status(status)
                    .createdAt(createdAt)
                    .direction(direction)
                    .build();
        } catch (Exception ex) {
            log.warn("Skipping invalid money request row while loading requests: {}", ex.getMessage());
            return null;
        }
    }

    private java.time.LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.time.LocalDateTime dt) {
            return dt;
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        return null;
    }
}

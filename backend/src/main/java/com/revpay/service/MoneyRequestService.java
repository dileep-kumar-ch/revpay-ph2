package com.revpay.service;

import com.revpay.dto.CreateMoneyRequestDto;
import com.revpay.dto.MoneyRequestResponse;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MoneyRequestService {

    private final MoneyRequestRepository moneyRequestRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    public MoneyRequestService(MoneyRequestRepository moneyRequestRepository, UserRepository userRepository, TransactionService transactionService) {
        this.moneyRequestRepository = moneyRequestRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

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

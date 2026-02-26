package com.revpay.service;

import com.revpay.dto.BusinessProfileResponse;
import com.revpay.dto.BusinessVerificationUpdateRequest;
import com.revpay.dto.NotificationEvent;
import com.revpay.model.BusinessVerificationStatus;
import com.revpay.model.NotificationCategory;
import com.revpay.model.Role;
import com.revpay.model.User;
import com.revpay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class BusinessProfileService {
    private final UserRepository userRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    public BusinessProfileResponse getMyBusinessProfile(String principal) {
        User user = getUser(principal);
        ensureBusinessUser(user);
        return mapToProfile(user);
    }

    @Transactional
    public BusinessProfileResponse submitOrUpdateVerification(String principal, BusinessVerificationUpdateRequest request) {
        User user = getUser(principal);
        ensureBusinessUser(user);

        user.setBusinessName(request.getBusinessName());
        user.setBusinessType(request.getBusinessType());
        user.setTaxId(request.getTaxId());
        user.setBusinessAddress(request.getBusinessAddress());
        user.setVerificationDocsPath(request.getVerificationDocsPath());
        user.setBusinessVerificationStatus(BusinessVerificationStatus.PENDING_VERIFICATION);
        User saved = userRepository.save(user);

        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(saved.getId())
                .category(NotificationCategory.REQUESTS)
                .type("BUSINESS_DOCUMENTS_SUBMITTED")
                .title("Business verification submitted")
                .message("Your verification documents were submitted and are pending review.")
                .metadata(Map.of("verificationStatus", saved.getBusinessVerificationStatus().name()))
                .build());

        return mapToProfile(saved);
    }

    @Transactional
    public BusinessProfileResponse approveBusiness(String principal, Long businessUserId) {
        User admin = getUser(principal);
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admin can approve business verification");
        }

        User businessUser = userRepository.findById(businessUserId)
                .orElseThrow(() -> new RuntimeException("Business user not found"));
        ensureBusinessUser(businessUser);

        businessUser.setBusinessVerificationStatus(BusinessVerificationStatus.VERIFIED);
        User saved = userRepository.save(businessUser);

        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(saved.getId())
                .category(NotificationCategory.ALERTS)
                .type("BUSINESS_VERIFICATION_APPROVED")
                .title("Business verification approved")
                .message("Your business account is now verified.")
                .metadata(Map.of("verificationStatus", saved.getBusinessVerificationStatus().name()))
                .build());
        return mapToProfile(saved);
    }

    @Transactional
    public BusinessProfileResponse rejectBusiness(String principal, Long businessUserId, String reason) {
        User admin = getUser(principal);
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admin can reject business verification");
        }

        User businessUser = userRepository.findById(businessUserId)
                .orElseThrow(() -> new RuntimeException("Business user not found"));
        ensureBusinessUser(businessUser);

        businessUser.setBusinessVerificationStatus(BusinessVerificationStatus.REJECTED);
        User saved = userRepository.save(businessUser);

        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(saved.getId())
                .category(NotificationCategory.ALERTS)
                .type("BUSINESS_VERIFICATION_REJECTED")
                .title("Business verification rejected")
                .message(reason == null || reason.isBlank()
                        ? "Your verification request was rejected."
                        : "Your verification request was rejected: " + reason)
                .metadata(Map.of("verificationStatus", saved.getBusinessVerificationStatus().name()))
                .build());
        return mapToProfile(saved);
    }

    private BusinessProfileResponse mapToProfile(User user) {
        return BusinessProfileResponse.builder()
                .userId(user.getId())
                .businessName(user.getBusinessName())
                .businessType(user.getBusinessType())
                .taxId(user.getTaxId())
                .businessAddress(user.getBusinessAddress())
                .verificationDocsPath(user.getVerificationDocsPath())
                .verificationStatus(user.getBusinessVerificationStatus())
                .build();
    }

    private User getUser(String principal) {
        return userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private void ensureBusinessUser(User user) {
        if (user.getRole() != Role.BUSINESS) {
            throw new RuntimeException("Business role required");
        }
    }
}

package com.revpay.service;

import com.revpay.dto.*;
import com.revpay.model.BusinessVerificationStatus;
import com.revpay.model.Role;
import com.revpay.model.User;
import com.revpay.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthService implements UserDetailsService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;

        public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        @Lazy AuthenticationManager authenticationManager) {
                this.userRepository = userRepository;
                this.passwordEncoder = passwordEncoder;
                this.jwtService = jwtService;
                this.authenticationManager = authenticationManager;
        }

        public AuthResponse register(RegisterRequest request) {
                log.info("Attempting to register user: {}", request.getEmail());
                if (userRepository.existsByUsername(request.getUsername())) {
                        throw new RuntimeException("Username already exists");
                }
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new RuntimeException("Email already exists");
                }

                Role role = request.getRole() != null ? request.getRole() : Role.PERSONAL;
                if (role == Role.BUSINESS && (isBlank(request.getBusinessName())
                                || isBlank(request.getBusinessType())
                                || isBlank(request.getTaxId())
                                || isBlank(request.getBusinessAddress())
                                || isBlank(request.getVerificationDocsPath()))) {
                        throw new RuntimeException(
                                        "Business details and verification documents are required for BUSINESS accounts");
                }

                User user = User.builder()
                                .fullName(request.getFullName())
                                .username(request.getUsername())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .phoneNumber(request.getPhoneNumber())
                                .role(role)
                                .enabled(true)
                                .businessName(request.getBusinessName())
                                .businessType(request.getBusinessType())
                                .taxId(request.getTaxId())
                                .businessAddress(request.getBusinessAddress())
                                .verificationDocsPath(request.getVerificationDocsPath())
                                .businessVerificationStatus(
                                                role == Role.BUSINESS ? BusinessVerificationStatus.PENDING_VERIFICATION
                                                                : BusinessVerificationStatus.NOT_SUBMITTED)
                                .failedLoginAttempts(0)
                                .build();

                if (request.getSecurityQuestions() != null) {
                        java.util.List<com.revpay.model.SecurityQuestion> questions = request.getSecurityQuestions()
                                        .stream()
                                        .map(q -> com.revpay.model.SecurityQuestion.builder()
                                                        .user(user)
                                                        .question(q.getQuestion())
                                                        .answer(q.getAnswer()) // Should be hashed in production
                                                        .build())
                                        .collect(java.util.stream.Collectors.toList());
                        user.setSecurityQuestions(questions);
                }

                User savedUser = userRepository.save(user);

                String token = jwtService.generateToken(savedUser);
                return AuthResponse.builder()
                                .token(token)
                                .username(savedUser.getUsername())
                                .role(savedUser.getRole().name())
                                .build();
        }

        private boolean isBlank(String value) {
                return value == null || value.trim().isEmpty();
        }

        public AuthResponse login(AuthRequest request) {
                log.info("Attempting to login user: {}", request.getEmail());
                User user = userRepository.findByEmail(request.getEmail())
                                .or(() -> userRepository.findByUsername(request.getEmail()))
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                if (user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(java.time.LocalDateTime.now())) {
                        throw new RuntimeException(
                                        "Account is locked. Please try again after " + user.getLockoutUntil());
                }

                try {
                        authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(request.getEmail(),
                                                        request.getPassword()));

                        // Reset failed attempts on success
                        user.setFailedLoginAttempts(0);
                        user.setLockoutUntil(null);
                        userRepository.save(user);

                } catch (org.springframework.security.core.AuthenticationException e) {
                        int attempts = user.getFailedLoginAttempts() + 1;
                        user.setFailedLoginAttempts(attempts);
                        if (attempts >= 5) {
                                user.setLockoutUntil(java.time.LocalDateTime.now().plusMinutes(15));
                                userRepository.save(user);
                                throw new RuntimeException("Account locked due to too many failed attempts.");
                        }
                        userRepository.save(user);
                        throw e;
                }

                String token = jwtService.generateToken(user);
                return AuthResponse.builder()
                                .token(token)
                                .username(user.getUsername())
                                .role(user.getRole().name())
                                .build();
        }

        public void setTransactionPin(String username, String pin) {
                User user = userRepository.findByUsername(username)
                                .or(() -> userRepository.findByEmail(username))
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
                user.setTransactionPin(passwordEncoder.encode(pin));
                userRepository.save(user);
        }

        public boolean verifyTransactionPin(String username, String pin) {
                User user = userRepository.findByUsername(username)
                                .or(() -> userRepository.findByEmail(username))
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
                if (user.getTransactionPin() == null) {
                        throw new RuntimeException("Transaction PIN not set");
                }
                return passwordEncoder.matches(pin, user.getTransactionPin());
        }

        public java.util.List<String> getRecoveryQuestions(String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
                return user.getSecurityQuestions().stream()
                                .map(com.revpay.model.SecurityQuestion::getQuestion)
                                .collect(java.util.stream.Collectors.toList());
        }

        public void resetPassword(String email, java.util.List<SecurityQuestionDto> answers, String newPassword) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                // Verify answers
                if (user.getSecurityQuestions().size() != answers.size()) {
                        throw new RuntimeException("Question/Answer count mismatch");
                }

                for (int i = 0; i < user.getSecurityQuestions().size(); i++) {
                        String storedAnswer = user.getSecurityQuestions().get(i).getAnswer();
                        String providedAnswer = answers.get(i).getAnswer();
                        if (!storedAnswer.equalsIgnoreCase(providedAnswer)) {
                                throw new RuntimeException("Incorrect answers to security questions");
                        }
                }

                user.setPassword(passwordEncoder.encode(newPassword));
                user.setFailedLoginAttempts(0);
                user.setLockoutUntil(null);
                userRepository.save(user);
        }

        public boolean resetTransactionPinWithQuestions(String email,
                        java.util.List<java.util.Map<String, String>> answers,
                        String newPin) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                if (user.getSecurityQuestions().size() != answers.size()) {
                        return false;
                }

                for (int i = 0; i < user.getSecurityQuestions().size(); i++) {
                        String storedAnswer = user.getSecurityQuestions().get(i).getAnswer();
                        String providedAnswer = answers.get(i).get("answer");
                        if (!storedAnswer.equalsIgnoreCase(providedAnswer)) {
                                return false;
                        }
                }

                user.setTransactionPin(passwordEncoder.encode(newPin));
                userRepository.save(user);
                return true;
        }

        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                return userRepository.findByEmail(username)
                                .or(() -> userRepository.findByUsername(username))
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        }
}

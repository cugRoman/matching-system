package com.stocktrade.matchingsystem.service;

import com.stocktrade.matchingsystem.auth.AuthPrincipal;
import com.stocktrade.matchingsystem.common.model.dto.LoginRequestDTO;
import com.stocktrade.matchingsystem.common.model.dto.LoginResponseDTO;
import com.stocktrade.matchingsystem.common.model.dto.RegisterRequestDTO;
import com.stocktrade.matchingsystem.common.model.entity.UserAccountEntity;
import com.stocktrade.matchingsystem.common.util.IdGenerator;
import com.stocktrade.matchingsystem.persistence.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long TOKEN_TTL_MS = 8 * 60 * 60 * 1000L;

    private final UserAccountRepository userAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public Optional<LoginResponseDTO> login(LoginRequestDTO request) {
        if (request == null || request.getLoginId() == null || request.getPassword() == null) {
            return Optional.empty();
        }
        String loginId = request.getLoginId().trim();
        Optional<UserAccountEntity> userOpt = userAccountRepository.findByLoginId(loginId);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        UserAccountEntity user = userOpt.get();
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            return Optional.empty();
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return Optional.empty();
        }

        return Optional.of(issueSession(user));
    }

    public Optional<LoginResponseDTO> register(RegisterRequestDTO request) {
        if (request == null
                || isBlank(request.getLoginId())
                || isBlank(request.getPassword())) {
            return Optional.empty();
        }
        String loginId = request.getLoginId().trim();
        if (userAccountRepository.findByLoginId(loginId).isPresent()) {
            return Optional.empty();
        }

        String shareHolderId = nextAvailableShareHolderId();

        UserAccountEntity saved = userAccountRepository.save(UserAccountEntity.builder()
                .loginId(loginId)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .shareHolderId(shareHolderId)
                .role("USER")
                .enabled(true)
                .build());
        return Optional.of(issueSession(saved));
    }

    public Optional<AuthPrincipal> authenticate(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return Optional.empty();
        }
        String token;
        if (authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring("Bearer ".length()).trim();
        } else {
            // Be tolerant for clients that send raw token directly.
            token = authorizationHeader.trim();
        }
        token = stripQuotes(token);
        if (token.isBlank()) {
            return Optional.empty();
        }
        SessionInfo session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.expiresAtEpochMs < Instant.now().toEpochMilli()) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session.principal);
    }

    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    private LoginResponseDTO issueSession(UserAccountEntity user) {
        long expiresAt = Instant.now().toEpochMilli() + TOKEN_TTL_MS;
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new SessionInfo(
                AuthPrincipal.builder()
                        .loginId(user.getLoginId())
                        .shareHolderId(user.getShareHolderId())
                        .role(user.getRole())
                        .build(),
                expiresAt));
        return LoginResponseDTO.builder()
                .token(token)
                .loginId(user.getLoginId())
                .shareHolderId(user.getShareHolderId())
                .role(user.getRole())
                .expiresAtEpochMs(expiresAt)
                .build();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String stripQuotes(String s) {
        if (s == null || s.length() < 2) {
            return s;
        }
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private String nextAvailableShareHolderId() {
        String id;
        do {
            id = IdGenerator.nextShareHolderId();
        } while (userAccountRepository.findByShareHolderId(id).isPresent());
        return id;
    }

    private record SessionInfo(AuthPrincipal principal, long expiresAtEpochMs) {
    }
}

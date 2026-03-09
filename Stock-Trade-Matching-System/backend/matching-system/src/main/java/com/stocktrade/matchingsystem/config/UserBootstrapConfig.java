package com.stocktrade.matchingsystem.config;

import com.stocktrade.matchingsystem.common.model.entity.UserAccountEntity;
import com.stocktrade.matchingsystem.persistence.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class UserBootstrapConfig {

    private final UserAccountRepository userAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner bootstrapUsers() {
        return args -> {
            upsertUser("trader1", "pass123", "SH1001", "USER");
            upsertUser("trader2", "pass123", "SH1002", "USER");
            upsertUser("admin", "admin123", "SH9000", "ADMIN");
        };
    }

    private void upsertUser(String loginId, String rawPassword, String shareHolderId, String role) {
        if (userAccountRepository.findByLoginId(loginId).isPresent()) {
            return;
        }
        userAccountRepository.save(UserAccountEntity.builder()
                .loginId(loginId)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .shareHolderId(shareHolderId)
                .role(role)
                .enabled(true)
                .build());
    }
}


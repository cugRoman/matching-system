package com.stocktrade.matchingsystem.common.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponseDTO {
    private String token;
    private String loginId;
    private String shareHolderId;
    private String role;
    private long expiresAtEpochMs;
}


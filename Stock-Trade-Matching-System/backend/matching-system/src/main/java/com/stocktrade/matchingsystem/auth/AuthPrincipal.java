package com.stocktrade.matchingsystem.auth;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthPrincipal {
    String loginId;
    String shareHolderId;
    String role;
}


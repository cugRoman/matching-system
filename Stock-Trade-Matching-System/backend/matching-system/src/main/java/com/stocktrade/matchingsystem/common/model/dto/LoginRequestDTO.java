package com.stocktrade.matchingsystem.common.model.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class LoginRequestDTO {
    @NotBlank(message = "loginId is required")
    @Size(max = 64, message = "loginId is too long")
    private String loginId;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 128, message = "password length must be 6-128")
    private String password;
}

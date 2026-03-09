package com.stocktrade.matchingsystem.common.model.dto;

import lombok.Data;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

@Data
public class OrderRequestDTO {
    @NotBlank(message = "market is required")
    private String market;

    @NotBlank(message = "securityId is required")
    @Pattern(regexp = "\\d{6}", message = "securityId must be 6 digits")
    private String securityId;

    @NotBlank(message = "side is required")
    private String side;

    @NotNull(message = "qty is required")
    @Positive(message = "qty must be positive")
    private Integer qty;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.0000001", message = "price must be positive")
    private Double price;

    private LocalDate tradeDate;
}

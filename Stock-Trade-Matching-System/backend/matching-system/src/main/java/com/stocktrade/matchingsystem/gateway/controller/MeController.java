package com.stocktrade.matchingsystem.gateway.controller;

import com.stocktrade.matchingsystem.auth.AuthPrincipal;
import com.stocktrade.matchingsystem.common.model.dto.OrderExecutionDetailDTO;
import com.stocktrade.matchingsystem.common.model.dto.UserOrderSummaryDTO;
import com.stocktrade.matchingsystem.common.model.dto.UserStockActivityDTO;
import com.stocktrade.matchingsystem.service.AuthService;
import com.stocktrade.matchingsystem.service.UserActivityService;
import com.stocktrade.matchingsystem.service.UserOrderQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "Me", description = "Current user query APIs")
@SecurityRequirement(name = "bearerAuth")
public class MeController {

    private final AuthService authService;
    private final UserActivityService userActivityService;
    private final UserOrderQueryService userOrderQueryService;

    @GetMapping("/stocks/activity")
    @Operation(summary = "Query current user's orders and reports grouped by stock")
    public ResponseEntity<?> stockActivity(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String securityId) {
        Optional<AuthPrincipal> principalOpt = authService.authenticate(authorization);
        if (principalOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        String shareHolderId = principalOpt.get().getShareHolderId();

        List<UserStockActivityDTO> activities = userActivityService.listByShareHolder(shareHolderId);
        if (market != null && !market.isBlank()) {
            activities = activities.stream()
                    .filter(item -> market.equalsIgnoreCase(item.getMarket()))
                    .toList();
        }
        if (securityId != null && !securityId.isBlank()) {
            activities = activities.stream()
                    .filter(item -> securityId.equals(item.getSecurityId()))
                    .toList();
        }
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/orders")
    @Operation(summary = "Query current user's order list")
    public ResponseEntity<?> myOrders(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String securityId,
            @RequestParam(required = false) String status) {
        Optional<AuthPrincipal> principalOpt = authService.authenticate(authorization);
        if (principalOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        String shareHolderId = principalOpt.get().getShareHolderId();

        List<UserOrderSummaryDTO> orders = userOrderQueryService.listByShareHolder(shareHolderId);
        if (market != null && !market.isBlank()) {
            orders = orders.stream()
                    .filter(item -> market.equalsIgnoreCase(item.getMarket()))
                    .toList();
        }
        if (securityId != null && !securityId.isBlank()) {
            orders = orders.stream()
                    .filter(item -> securityId.equals(item.getSecurityId()))
                    .toList();
        }
        if (status != null && !status.isBlank()) {
            orders = orders.stream()
                    .filter(item -> status.equalsIgnoreCase(item.getStatus()))
                    .toList();
        }
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders/{clOrderId}/executions")
    @Operation(summary = "Query current user's one order execution details")
    public ResponseEntity<?> orderExecutions(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String clOrderId) {
        Optional<AuthPrincipal> principalOpt = authService.authenticate(authorization);
        if (principalOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        String shareHolderId = principalOpt.get().getShareHolderId();

        Optional<OrderExecutionDetailDTO> detailOpt =
                userOrderQueryService.getOrderExecutionDetail(shareHolderId, clOrderId);
        if (detailOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Order not found"));
        }
        return ResponseEntity.ok(detailOpt.get());
    }
}

package com.stocktrade.matchingsystem.gateway.controller;

import com.stocktrade.matchingsystem.auth.AuthPrincipal;
import com.stocktrade.matchingsystem.common.model.InternalOrder;
import com.stocktrade.matchingsystem.common.model.dto.OrderDTO;
import com.stocktrade.matchingsystem.common.model.dto.OrderRequestDTO;
import com.stocktrade.matchingsystem.common.util.IdGenerator;
import com.stocktrade.matchingsystem.report.ReportDispatcher;
import com.stocktrade.matchingsystem.service.AuthService;
import com.stocktrade.matchingsystem.service.OrderProcessResult;
import com.stocktrade.matchingsystem.service.OrchestratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order", description = "Order management")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrchestratorService orchestratorService;
    private final ReportDispatcher reportDispatcher;
    private final AuthService authService;

    @PostMapping
    @Operation(summary = "Submit order", description = "Submit client order with auth")
    public ResponseEntity<Map<String, Object>> submitOrder(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody @Valid OrderRequestDTO request) {
        Optional<AuthPrincipal> principalOpt = authService.authenticate(authorization);
        if (principalOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        AuthPrincipal principal = principalOpt.get();

        String clOrderId = IdGenerator.nextClOrderId();
        OrderDTO orderDTO = OrderDTO.builder()
                .clOrderId(clOrderId)
                .shareHolderId(principal.getShareHolderId())
                .market(request.getMarket())
                .securityId(request.getSecurityId())
                .side(request.getSide())
                .qty(request.getQty())
                .price(request.getPrice())
                .tradeDate(request.getTradeDate())
                .build();

        log.info("Received order: {} user={}", clOrderId, principal.getLoginId());

        OrderProcessResult result = orchestratorService.processOrder(orderDTO);
        reportDispatcher.dispatch(result);

        Map<String, Object> response = Map.of(
                "clOrderId", clOrderId,
                "resultType", result.getType().name(),
                "message", switch (result.getType()) {
                    case REJECTED_INVALID -> "Order rejected as invalid";
                    case REJECTED_WASH -> "Order rejected by wash-trade check";
                    case ACCEPTED -> "Order accepted";
                });

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get orders")
    public ResponseEntity<?> getAllOrders(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<AuthPrincipal> principalOpt = authService.authenticate(authorization);
        if (principalOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        AuthPrincipal principal = principalOpt.get();

        List<InternalOrder> orders = orchestratorService.getAllOrders();
        if (!"ADMIN".equalsIgnoreCase(principal.getRole())) {
            orders = orders.stream()
                    .filter(o -> principal.getShareHolderId().equals(o.getShareHolderId()))
                    .toList();
        }
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{clOrderId}")
    @Operation(summary = "Get one order")
    public ResponseEntity<?> getOrder(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String clOrderId) {
        Optional<AuthPrincipal> principalOpt = authService.authenticate(authorization);
        if (principalOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        AuthPrincipal principal = principalOpt.get();

        Optional<InternalOrder> orderOpt = orchestratorService.getOrder(clOrderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        InternalOrder order = orderOpt.get();
        if (!"ADMIN".equalsIgnoreCase(principal.getRole())
                && !principal.getShareHolderId().equals(order.getShareHolderId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        }
        return ResponseEntity.ok(order);
    }
}

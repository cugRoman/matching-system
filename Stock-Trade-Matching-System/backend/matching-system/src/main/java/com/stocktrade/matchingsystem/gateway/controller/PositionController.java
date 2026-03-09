package com.stocktrade.matchingsystem.gateway.controller;

import com.stocktrade.matchingsystem.auth.AuthPrincipal;
import com.stocktrade.matchingsystem.common.model.entity.PositionEntity;
import com.stocktrade.matchingsystem.service.AuthService;
import com.stocktrade.matchingsystem.service.PositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
@Tag(name = "Position", description = "持仓系统 API")
@SecurityRequirement(name = "bearerAuth")
public class PositionController {

    private final AuthService authService;
    private final PositionService positionService;

    @GetMapping
    @Operation(summary = "查询当前登录用户的持仓列表")
    public ResponseEntity<?> getMyPositions(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        // 1. 鉴权：检查用户是否登录
        Optional<AuthPrincipal> principalOpt = authService.authenticate(authorization);
        if (principalOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized (未登录)"));
        }
        
        // 2. 获取当前登录用户的股东账号
        String shareHolderId = principalOpt.get().getShareHolderId();
        
        // 3. 去数据库查询该账号的持仓
        List<PositionEntity> positions = positionService.getPositionsByShareHolderId(shareHolderId);
        
        // 4. 返回给前端
        return ResponseEntity.ok(positions);
    }
}
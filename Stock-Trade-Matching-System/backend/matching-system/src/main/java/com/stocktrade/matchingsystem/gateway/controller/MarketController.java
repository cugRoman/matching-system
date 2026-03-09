package com.stocktrade.matchingsystem.gateway.controller;

import com.stocktrade.matchingsystem.common.model.dto.RecentExecutionDTO;
import com.stocktrade.matchingsystem.service.MarketDataQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@Tag(name = "Market", description = "Market data APIs")
public class MarketController {

    private final MarketDataQueryService marketDataQueryService;

    @GetMapping("/recent-executions")
    @Operation(summary = "Latest execution snapshot by market and security")
    public ResponseEntity<List<RecentExecutionDTO>> recentExecutions() {
        return ResponseEntity.ok(marketDataQueryService.listRecentExecutionsBySymbol());
    }
}

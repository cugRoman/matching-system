package com.stocktrade.matchingsystem.service;

import com.stocktrade.matchingsystem.common.model.dto.RecentExecutionDTO;
import com.stocktrade.matchingsystem.common.model.entity.ExecutionEntity;
import com.stocktrade.matchingsystem.persistence.repository.ExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MarketDataQueryService {

    private final ExecutionRepository executionRepository;

    public List<RecentExecutionDTO> listRecentExecutionsBySymbol() {
        List<ExecutionEntity> executions = executionRepository.findAllByOrderByCreatedAtDescIdDesc();

        Map<SymbolKey, RecentExecutionDTO> latestBySymbol = new LinkedHashMap<>();
        Map<SymbolKey, Long> countBySymbol = new LinkedHashMap<>();

        for (ExecutionEntity execution : executions) {
            SymbolKey key = new SymbolKey(execution.getMarket(), execution.getSecurityId());
            countBySymbol.merge(key, 1L, Long::sum);

            if (!latestBySymbol.containsKey(key)) {
                latestBySymbol.put(key, RecentExecutionDTO.builder()
                        .market(execution.getMarket())
                        .securityId(execution.getSecurityId())
                        .execId(execution.getExecId())
                        .execQty(execution.getExecQty())
                        .execPrice(execution.getExecPrice())
                        .executedAt(execution.getCreatedAt())
                        .executionCount(0L)
                        .build());
            }
        }

        List<RecentExecutionDTO> result = new ArrayList<>();
        latestBySymbol.forEach((key, dto) -> {
            dto.setExecutionCount(countBySymbol.getOrDefault(key, 0L));
            result.add(dto);
        });

        result.sort(Comparator.comparing(RecentExecutionDTO::getMarket)
                .thenComparing(RecentExecutionDTO::getSecurityId));
        return result;
    }

    private record SymbolKey(String market, String securityId) {
    }
}

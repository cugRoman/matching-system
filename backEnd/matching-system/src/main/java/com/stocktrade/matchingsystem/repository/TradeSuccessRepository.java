package com.stocktrade.matchingsystem.repository;

import com.stocktrade.matchingsystem.entity.TradeSuccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeSuccessRepository extends JpaRepository<TradeSuccess, String> {
    List<TradeSuccess> findByBuyShareHolderIdOrSellShareHolderIdOrderByExecTimeDesc(
            String buyShareHolderId, String sellShareHolderId);
            
    List<TradeSuccess> findTop100ByOrderByExecTimeDesc();

}



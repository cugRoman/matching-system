package com.stocktrade.matchingsystem.repository;

import com.stocktrade.matchingsystem.entity.TradeIllegal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeIllegalRepository extends JpaRepository<TradeIllegal, String> {
}


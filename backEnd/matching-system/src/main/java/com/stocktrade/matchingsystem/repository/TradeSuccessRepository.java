package com.stocktrade.matchingsystem.repository;

import com.stocktrade.matchingsystem.entity.TradeSuccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeSuccessRepository extends JpaRepository<TradeSuccess, String> {
}


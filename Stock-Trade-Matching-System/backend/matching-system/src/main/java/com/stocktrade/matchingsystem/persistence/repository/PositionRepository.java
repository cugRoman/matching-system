package com.stocktrade.matchingsystem.persistence.repository;

import com.stocktrade.matchingsystem.common.model.entity.PositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<PositionEntity, Long> {
    
    // 根据股东账号查询该账号下的所有持仓
    List<PositionEntity> findByShareHolderId(String shareHolderId);

    // 根据股东账号和股票代码，查询某一只特定的股票持仓
    Optional<PositionEntity> findByShareHolderIdAndSecurityId(String shareHolderId, String securityId);
}
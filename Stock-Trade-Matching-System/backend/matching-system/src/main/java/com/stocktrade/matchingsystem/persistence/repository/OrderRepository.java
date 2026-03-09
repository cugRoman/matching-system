package com.stocktrade.matchingsystem.persistence.repository;

import com.stocktrade.matchingsystem.common.model.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 委托持久化仓库
 */
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByClOrderId(String clOrderId);

    List<OrderEntity> findByShareHolderId(String shareHolderId);

    List<OrderEntity> findByShareHolderIdOrderByRecvTimeDesc(String shareHolderId);

    List<OrderEntity> findBySecurityId(String securityId);

    List<OrderEntity> findByWashTradeTrue();
}

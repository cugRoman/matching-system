package com.stocktrade.matchingsystem.persistence.repository;

import com.stocktrade.matchingsystem.common.model.entity.ExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 成交记录持久化仓库
 */
@Repository
public interface ExecutionRepository extends JpaRepository<ExecutionEntity, Long> {

    List<ExecutionEntity> findByClOrderId(String clOrderId);

    List<ExecutionEntity> findByCounterOrderId(String counterOrderId);

    List<ExecutionEntity> findBySecurityId(String securityId);

    List<ExecutionEntity> findByExecType(String execType);

    List<ExecutionEntity> findAllByOrderByCreatedAtDescIdDesc();

    List<ExecutionEntity> findByClOrderIdOrderByCreatedAtDescIdDesc(String clOrderId);
}

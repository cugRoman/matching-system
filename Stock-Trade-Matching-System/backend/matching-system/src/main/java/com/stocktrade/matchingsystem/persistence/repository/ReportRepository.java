package com.stocktrade.matchingsystem.persistence.repository;

import com.stocktrade.matchingsystem.common.model.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, Long> {
    List<ReportEntity> findByShareHolderIdOrderByTsDesc(String shareHolderId);
}

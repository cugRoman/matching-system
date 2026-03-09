package com.stocktrade.matchingsystem.persistence.repository;

import com.stocktrade.matchingsystem.common.model.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {
    Optional<UserAccountEntity> findByLoginId(String loginId);
    Optional<UserAccountEntity> findByShareHolderId(String shareHolderId);
}

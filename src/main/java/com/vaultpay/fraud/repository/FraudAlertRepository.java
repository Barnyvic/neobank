package com.vaultpay.fraud.repository;

import com.vaultpay.fraud.entity.FraudAlert;
import com.vaultpay.fraud.enums.FraudAlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    Page<FraudAlert> findByStatusOrderByCreatedAtDesc(FraudAlertStatus status, Pageable pageable);

    Page<FraudAlert> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

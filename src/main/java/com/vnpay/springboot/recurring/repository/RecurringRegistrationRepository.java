package com.vnpay.springboot.recurring.repository;

import com.vnpay.springboot.recurring.entity.RecurringRegistration;
import com.vnpay.springboot.recurring.entity.RecurringStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecurringRegistrationRepository extends JpaRepository<RecurringRegistration, Long> {
    Optional<RecurringRegistration> findByOrderReference(String orderReference);
    boolean existsByOrderReference(String orderReference);

    List<RecurringRegistration> findByStatusAndRspCodeOrderByCreatedAtDesc(
            RecurringStatus status, String rspCode);
}

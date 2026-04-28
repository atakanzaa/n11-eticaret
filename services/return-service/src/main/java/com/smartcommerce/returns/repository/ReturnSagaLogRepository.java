package com.smartcommerce.returns.repository;

import com.smartcommerce.returns.domain.ReturnSagaLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReturnSagaLogRepository extends JpaRepository<ReturnSagaLog, UUID> {
    List<ReturnSagaLog> findByReturnIdOrderByStartedAtAsc(UUID returnId);
}

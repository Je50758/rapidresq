package com.disaster.repository;

import com.disaster.models.EscalationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EscalationLogRepository extends JpaRepository<EscalationLog, String> {
    List<EscalationLog> findAllByOrderByTimestampDesc();
}

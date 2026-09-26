package com.disaster.repository;

import com.disaster.models.ReliefNeed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReliefNeedRepository extends JpaRepository<ReliefNeed, String> {
    List<ReliefNeed> findByStatus(ReliefNeed.NeedStatus status);
}

package com.disaster.repository;

import com.disaster.models.DonationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationRecordRepository extends JpaRepository<DonationRecord, String> {
    List<DonationRecord> findByReliefNeedId(String reliefNeedId);
}

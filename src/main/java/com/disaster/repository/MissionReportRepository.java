package com.disaster.repository;

import com.disaster.models.MissionReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MissionReportRepository extends JpaRepository<MissionReport, String> {
    List<MissionReport> findAllByOrderByGeneratedAtDesc();
    List<MissionReport> findByIncidentId(String incidentId);
}

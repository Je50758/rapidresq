package com.disaster.repository;

import com.disaster.models.Incident;
import com.disaster.models.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, String> {
    List<Incident> findByStatus(IncidentStatus status);
    List<Incident> findByStatusNot(IncidentStatus status);
}

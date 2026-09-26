package com.disaster.repository;

import com.disaster.models.VolunteerSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VolunteerSignalRepository extends JpaRepository<VolunteerSignal, String> {
    List<VolunteerSignal> findByTargetTeamIdOrderBySentAtDesc(String teamId);
    List<VolunteerSignal> findByTargetUsernameOrderBySentAtDesc(String username);
}

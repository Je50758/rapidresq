package com.disaster.repository;

import com.disaster.models.ResponseTeam;
import com.disaster.models.TeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResponseTeamRepository extends JpaRepository<ResponseTeam, String> {
    List<ResponseTeam> findByStatus(TeamStatus status);
}

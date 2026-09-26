package com.disaster.repository;

import com.disaster.models.SafeLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SafeLocationRepository extends JpaRepository<SafeLocation, String> {
    java.util.List<SafeLocation> findByActiveTrue();
}

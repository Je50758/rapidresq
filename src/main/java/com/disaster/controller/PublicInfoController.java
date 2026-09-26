package com.disaster.controller;

import com.disaster.models.MissionReport;
import com.disaster.service.DisasterManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public transparency endpoints - no authentication required.
 * After a disaster is resolved, the community report (summary, available
 * resources, nearby shelters & safe areas) is visible to everyone.
 */
@RestController
@RequestMapping("/api/public")
public class PublicInfoController {

    private final DisasterManagementService disasterService;

    @Autowired
    public PublicInfoController(DisasterManagementService disasterService) {
        this.disasterService = disasterService;
    }

    /** Public community reports: every resolved incident's mission report. */
    @GetMapping("/mission-reports")
    public ResponseEntity<List<MissionReport>> getPublicMissionReports() {
        return ResponseEntity.ok(disasterService.getPublicMissionReports());
    }
}

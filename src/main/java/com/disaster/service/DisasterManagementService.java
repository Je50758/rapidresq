package com.disaster.service;

import com.disaster.exceptions.DuplicateConfirmationException;
import com.disaster.exceptions.InvalidIncidentException;
import com.disaster.exceptions.NoTeamAvailableException;
import com.disaster.models.*;
import com.disaster.observers.EmergencyEscalationNotifier;
import com.disaster.observers.IncidentObserver;
import com.disaster.observers.StatusLogger;
import com.disaster.observers.VolunteerNotifier;
import com.disaster.repository.DonationRecordRepository;
import com.disaster.repository.IncidentRepository;
import com.disaster.repository.MissionReportRepository;
import com.disaster.repository.ReliefNeedRepository;
import com.disaster.repository.ResponseTeamRepository;
import com.disaster.repository.SafeLocationRepository;
import com.disaster.repository.VerificationCheckRepository;
import com.disaster.repository.VolunteerSignalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Core business logic service for Disaster Response Management.
 * Manages incident triage via PriorityQueue, JPA repository persistence,
 * crowdsourced verification, team dispatching, post-disaster rehabilitation,
 * and observer notifications.
 */
@Service
public class DisasterManagementService {
    private static final Logger log = LoggerFactory.getLogger(DisasterManagementService.class);
    private static final int REQUIRED_CONFIRMATIONS = 3;
    private static final int VERIFICATION_REWARD = 5;
    private static final int HOAX_PENALTY = 20;

    private final IncidentRepository incidentRepository;
    private final ResponseTeamRepository responseTeamRepository;
    private final ReliefNeedRepository reliefNeedRepository;
    private final DonationRecordRepository donationRecordRepository;
    private final UserAuthenticationService userAuthService;
    private final EncryptionService encryptionService;
    private final VolunteerSignalRepository volunteerSignalRepository;
    private final VerificationCheckRepository verificationCheckRepository;
    private final MissionReportRepository missionReportRepository;
    private final SafeLocationRepository safeLocationRepository;

    private int signalCounter = 1;
    private int checkCounter = 1;
    private int reportCounter = 1;

    // In-memory PriorityQueue for fast emergency triage
    private final PriorityQueue<Incident> urgentQueue = new PriorityQueue<>();

    // Observers
    private final List<IncidentObserver> observers = new CopyOnWriteArrayList<>();
    private final StatusLogger statusLogger;
    private final VolunteerNotifier volunteerNotifier;
    private final EmergencyEscalationNotifier escalationNotifier;

    private int incidentCounter = 100;
    private int reliefCounter = 1;

    @Autowired
    public DisasterManagementService(IncidentRepository incidentRepository,
                                     ResponseTeamRepository responseTeamRepository,
                                     ReliefNeedRepository reliefNeedRepository,
                                     DonationRecordRepository donationRecordRepository,
                                     UserAuthenticationService userAuthService,
                                     EncryptionService encryptionService,
                                     VolunteerSignalRepository volunteerSignalRepository,
                                     VerificationCheckRepository verificationCheckRepository,
                                     MissionReportRepository missionReportRepository,
                                     SafeLocationRepository safeLocationRepository,
                                     StatusLogger statusLogger,
                                     VolunteerNotifier volunteerNotifier,
                                     EmergencyEscalationNotifier escalationNotifier) {
        this.incidentRepository = incidentRepository;
        this.responseTeamRepository = responseTeamRepository;
        this.reliefNeedRepository = reliefNeedRepository;
        this.donationRecordRepository = donationRecordRepository;
        this.userAuthService = userAuthService;
        this.encryptionService = encryptionService;
        this.volunteerSignalRepository = volunteerSignalRepository;
        this.verificationCheckRepository = verificationCheckRepository;
        this.missionReportRepository = missionReportRepository;
        this.safeLocationRepository = safeLocationRepository;
        this.statusLogger = statusLogger;
        this.volunteerNotifier = volunteerNotifier;
        this.escalationNotifier = escalationNotifier;

        registerObserver(statusLogger);
        registerObserver(volunteerNotifier);
        registerObserver(escalationNotifier);
    }

    public void registerObserver(IncidentObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    private void notifyReported(Incident incident) {
        for (IncidentObserver obs : observers) {
            obs.onIncidentReported(incident);
        }
    }

    private void notifyVerified(Incident incident) {
        for (IncidentObserver obs : observers) {
            obs.onIncidentVerified(incident);
        }
    }

    private void notifyTeamAssigned(Incident incident, ResponseTeam team) {
        for (IncidentObserver obs : observers) {
            obs.onTeamAssigned(incident, team);
        }
    }

    private void notifyResolved(Incident incident) {
        for (IncidentObserver obs : observers) {
            obs.onIncidentResolved(incident);
        }
    }

    public synchronized String generateNextIncidentId() {
        incidentCounter++;
        while (incidentRepository.existsById("INC-" + incidentCounter)) {
            incidentCounter++;
        }
        return "INC-" + incidentCounter;
    }

    /**
     * Reports a new incident. Low-trust reporters (&lt;20) receive an unverified tag.
     * Client-supplied IDs that already exist are rejected to prevent entity overwrite.
     */
    public synchronized Incident reportIncident(Incident incident) throws InvalidIncidentException {
        if (incident == null) {
            throw new InvalidIncidentException("Incident payload cannot be null.");
        }
        if (incident.getId() == null || incident.getId().trim().isEmpty()) {
            incident.setId(generateNextIncidentId());
        } else if (incidentRepository.existsById(incident.getId())) {
            // Defense-in-depth: never let a caller overwrite an existing incident via save()
            throw new InvalidIncidentException("Incident ID already exists: " + incident.getId());
        }
        if (incident.getLocation() == null || incident.getLocation().trim().isEmpty()) {
            throw new InvalidIncidentException("Incident location is required for emergency dispatch.");
        }

        if (incident.getReporterTrustScore() < 20) {
            incident.setDescription("[⚠️ UNVERIFIED - LOW TRUST REPORTER] " + incident.getDescription());
        }

        incident.setPriority(incident.calculatePriority());
        Incident saved = incidentRepository.save(incident);
        urgentQueue.offer(saved);

        notifyReported(saved);
        return saved;
    }

    /**
     * Crowdsourced confirmation: {@value REQUIRED_CONFIRMATIONS}+ distinct citizen reports
     * automatically transition the incident to VERIFIED and reward the initial reporter
     * with +5 Trust Score. The same account can only confirm an incident once.
     *
     * @return the updated incident
     * @throws DuplicateConfirmationException when this username already confirmed the incident
     */
    public synchronized Incident addConfirmingReport(String incidentId, String reporterUsername)
            throws InvalidIncidentException, DuplicateConfirmationException {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new InvalidIncidentException("Incident not found: " + incidentId));

        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new InvalidIncidentException("Cannot confirm a RESOLVED incident: " + incidentId);
        }

        boolean firstTime = incident.addConfirmation(reporterUsername);
        if (!firstTime) {
            throw new DuplicateConfirmationException(
                    "User '" + reporterUsername + "' has already confirmed incident " + incidentId + ".");
        }

        if (incident.getReportCount() >= REQUIRED_CONFIRMATIONS && incident.getStatus() == IncidentStatus.REPORTED) {
            incident.setStatus(IncidentStatus.VERIFIED);

            User initialReporter = userAuthService.getUser(incident.getReporterUsername());
            if (initialReporter != null) {
                userAuthService.adjustTrustScore(initialReporter.getUsername(), VERIFICATION_REWARD);
                incident.setReporterTrustScore(
                        Math.min(100, initialReporter.getTrustScore() + VERIFICATION_REWARD));
            }

            notifyVerified(incident);
        }

        Incident updated = incidentRepository.save(incident);

        urgentQueue.remove(incident);
        if (incident.getStatus() != IncidentStatus.RESOLVED) {
            urgentQueue.offer(updated);
        }

        return updated;
    }

    /**
     * Marks an incident as a false alarm / hoax. Penalizes the reporter by -20 Trust Score.
     */
    public synchronized Incident markIncidentAsFalseAlarm(String incidentId) throws InvalidIncidentException {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new InvalidIncidentException("Incident not found: " + incidentId));

        User reporter = userAuthService.getUser(incident.getReporterUsername());
        if (reporter != null) {
            userAuthService.adjustTrustScore(reporter.getUsername(), -HOAX_PENALTY);
            incident.setReporterTrustScore(Math.max(0, reporter.getTrustScore() - HOAX_PENALTY));
        }

        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setDescription("[FALSE ALARM / SPAM CLEARED] " + incident.getDescription());
        incident.setPriority(0.0);

        if (incident.getAssignedTeamId() != null) {
            ResponseTeam team = responseTeamRepository.findById(incident.getAssignedTeamId()).orElse(null);
            if (team != null) {
                team.release();
                responseTeamRepository.save(team);
            }
        }

        Incident saved = incidentRepository.save(incident);
        urgentQueue.remove(incident);
        notifyResolved(saved);
        return saved;
    }

    /**
     * Assigns the most appropriate available response team using polymorphism.
     */
    public synchronized ResponseTeam assignTeam(String incidentId)
            throws NoTeamAvailableException, InvalidIncidentException {
        return assignTeam(incidentId, false, null);
    }

    /**
     * Dispatch with scope control. When volunteerUnitsOnly is true (RESPONSE_TEAM scope),
     * only VOLUNTEER units matching the volunteer team type filter are eligible;
     * professional agencies (fire/medical/rescue) stay under ADMIN command.
     *
     * @param volunteerTeamTypeFilter when non-null, restrict to volunteer units of this type
     */
    public synchronized ResponseTeam assignTeam(String incidentId, boolean volunteerUnitsOnly,
                                                VolunteerTeamType volunteerTeamTypeFilter)
            throws NoTeamAvailableException, InvalidIncidentException {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new InvalidIncidentException("Incident not found: " + incidentId));

        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new InvalidIncidentException("Cannot assign team: Incident is already RESOLVED.");
        }

        List<ResponseTeam> allTeams = responseTeamRepository.findAll();
        ResponseTeam chosenTeam = null;

        for (ResponseTeam team : allTeams) {
            if (!team.isAvailable() || !team.canHandle(incident)) {
                continue;
            }
            if (volunteerUnitsOnly) {
                if (!(team instanceof Volunteer v)) continue;
                if (volunteerTeamTypeFilter != null && v.getTeamType() != volunteerTeamTypeFilter) continue;
            }
            chosenTeam = team;
            break;
        }

        if (chosenTeam == null) {
            String scope = volunteerUnitsOnly
                    ? (volunteerTeamTypeFilter != null ? volunteerTeamTypeFilter.getDisplayName() + " volunteer" : "volunteer")
                    : "";
            throw new NoTeamAvailableException(
                    incidentId,
                    incident.getDisasterType().name(),
                    "No available " + scope + " team for " + incident.getDisasterType() + " at this time."
            );
        }

        chosenTeam.assignToIncident(incidentId);
        responseTeamRepository.save(chosenTeam);

        incident.setAssignedTeamId(chosenTeam.getId());
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incidentRepository.save(incident);

        urgentQueue.remove(incident);
        urgentQueue.offer(incident);

        notifyTeamAssigned(incident, chosenTeam);
        return chosenTeam;
    }

    /**
     * Resolves an incident, frees the assigned team, and automatically launches
     * a post-disaster rehabilitation relief drive for the affected zone.
     */
    public synchronized Incident resolveIncident(String incidentId) throws InvalidIncidentException {
        return resolveIncident(incidentId, null);
    }

    public synchronized Incident resolveIncident(String incidentId, String resolvedByAdmin) throws InvalidIncidentException {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new InvalidIncidentException("Incident not found: " + incidentId));

        incident.setStatus(IncidentStatus.RESOLVED);

        if (incident.getAssignedTeamId() != null) {
            ResponseTeam team = responseTeamRepository.findById(incident.getAssignedTeamId()).orElse(null);
            if (team != null) {
                team.release();
                responseTeamRepository.save(team);
            }
        }

        Incident saved = incidentRepository.save(incident);
        urgentQueue.remove(incident);
        notifyResolved(saved);

        createReliefNeedForResolvedIncident(saved);
        MissionReport missionReport = createMissionReportForResolvedIncident(saved, resolvedByAdmin);
        if (missionReport != null) {
            // Deliver to all volunteer units as a broadcast so the field stays informed
            try {
                broadcastToVolunteers(null,
                        "MISSION REPORT " + missionReport.getReportId() + ": " + missionReport.getSummary(),
                        incidentId, resolvedByAdmin != null ? resolvedByAdmin : "system");
            } catch (Exception e) {
                log.warn("Mission report broadcast failed for {}: {}", incidentId, e.getMessage());
            }
        }
        return saved;
    }

    public synchronized Incident overridePriority(String incidentId, double newPriority) throws InvalidIncidentException {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new InvalidIncidentException("Incident not found: " + incidentId));

        incident.setPriority(Math.max(0.0, newPriority));
        Incident saved = incidentRepository.save(incident);

        urgentQueue.remove(incident);
        if (saved.getStatus() != IncidentStatus.RESOLVED) {
            urgentQueue.offer(saved);
        }
        return saved;
    }

    public synchronized ResponseTeam reassignTeam(String incidentId, String newTeamId) throws InvalidIncidentException {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new InvalidIncidentException("Incident not found: " + incidentId));

        ResponseTeam newTeam = responseTeamRepository.findById(newTeamId)
                .orElseThrow(() -> new InvalidIncidentException("Team not found: " + newTeamId));

        if (!newTeam.isAvailable()) {
            throw new InvalidIncidentException("Selected replacement team is not currently AVAILABLE.");
        }

        if (incident.getAssignedTeamId() != null) {
            ResponseTeam oldTeam = responseTeamRepository.findById(incident.getAssignedTeamId()).orElse(null);
            if (oldTeam != null) {
                oldTeam.release();
                responseTeamRepository.save(oldTeam);
            }
        }

        newTeam.assignToIncident(incidentId);
        responseTeamRepository.save(newTeam);

        incident.setAssignedTeamId(newTeam.getId());
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incidentRepository.save(incident);

        urgentQueue.remove(incident);
        urgentQueue.offer(incident);

        notifyTeamAssigned(incident, newTeam);
        return newTeam;
    }

    /**
     * Generates the post-operation mission report: response summary + snapshot of
     * requirement-matched AVAILABLE resources + nearby shelters/safe areas for the
     * affected community, delivered to volunteer units.
     */
    private MissionReport createMissionReportForResolvedIncident(Incident incident, String resolvedBy) {
        try {
            String reportId = "MR-" + String.format("%04d", reportCounter++);
            int signalsSent = (int) volunteerSignalRepository.findAll().stream()
                    .filter(s -> incident.getId().equals(s.getIncidentId()))
                    .count();
            int checksPassed = (int) verificationCheckRepository.findByIncidentIdOrderByCheckedAtDesc(incident.getId())
                    .stream().filter(VerificationCheck::isPassed).count();
            int teamsDeployed = incident.getAssignedTeamId() != null ? 1 : 0;

            String summary = String.format(
                    "%s at %s resolved. Injured handled: %d. Team deployed: %s. Verification checks passed: %d. Relief campaign auto-opened.",
                    incident.getDisasterType(), incident.getLocation(), incident.getInjuredCount(),
                    incident.getAssignedTeamId() != null ? incident.getAssignedTeamId() : "none",
                    checksPassed);

            List<Map<String, Object>> availableResources = getMatchedAvailableResources(incident.getDisasterType());
            List<Map<String, Object>> nearbySafe = getNearbySafeLocations(incident);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("availableResources", availableResources);
            payload.put("nearbySafeLocations", nearbySafe);
            String resourcesJson;
            try {
                resourcesJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
            } catch (Exception e) {
                resourcesJson = "{}";
            }

            MissionReport report = new MissionReport(reportId, incident.getId(), incident.getLocation(),
                    incident.getDisasterType(), summary, incident.getInjuredCount(), teamsDeployed,
                    signalsSent, checksPassed, resourcesJson, resolvedBy != null ? resolvedBy : "system");
            MissionReport savedReport = missionReportRepository.save(report);
            log.info("📋 [MISSION REPORT] {}", savedReport);
            return savedReport;
        } catch (Exception e) {
            log.error("Failed to generate mission report for {}: {}", incident.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * ACTIVE shelters & safe areas nearest to the incident (within 25 km, closest first).
     * Uses report coordinates when present, else a Dhaka-center default so listings still work.
     */
    public List<Map<String, Object>> getNearbySafeLocations(Incident incident) {
        double lat = incident.getLatitude() != 0 ? incident.getLatitude() : 23.78;
        double lon = incident.getLongitude() != 0 ? incident.getLongitude() : 90.40;

        List<Map<String, Object>> result = new ArrayList<>();
        for (SafeLocation loc : safeLocationRepository.findByActiveTrue()) {
            double dist = loc.distanceKmTo(lat, lon);
            if (dist <= 25) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("locationId", loc.getLocationId());
                item.put("name", loc.getName());
                item.put("type", loc.getType().getLabel());
                item.put("address", loc.getAddress());
                item.put("capacityPersons", loc.getCapacityPersons());
                item.put("contact", loc.getContactNumber());
                item.put("hasWaterSupply", loc.isHasWaterSupply());
                item.put("hasMedicalPoint", loc.isHasMedicalPoint());
                item.put("distanceKm", Math.round(dist * 10.0) / 10.0);
                result.add(item);
            }
        }
        result.sort((a, b) -> Double.compare((double) a.get("distanceKm"), (double) b.get("distanceKm")));
        return result;
    }

    /** Public transparency list of all mission reports (community report). */
    public List<MissionReport> getPublicMissionReports() {
        return missionReportRepository.findAllByOrderByGeneratedAtDesc();
    }

    private void createReliefNeedForResolvedIncident(Incident incident) {
        String needId = "RELIEF-" + String.format("%03d", reliefCounter++);
        // Skip IDs that already exist (seeds/restarts) to avoid silent overwrite
        while (reliefNeedRepository.existsById(needId)) {
            needId = "RELIEF-" + String.format("%03d", reliefCounter++);
        }

        double targetFunding = Math.max(30000.0, incident.getInjuredCount() * 12000.0 + 20000.0);
        int targetPackages = Math.max(25, incident.getInjuredCount() * 10 + 15);

        String packageDesc;
        switch (incident.getDisasterType()) {
            case FIRE:
                packageDesc = "Burn Injury Medication & Emergency Shelter Tarpaulins";
                break;
            case FLOOD:
                packageDesc = "Water Purification Kits & Dry Food Rations";
                break;
            case ACCIDENT:
                packageDesc = "Emergency Trauma Surgery & Blood Transfusion Supplies";
                break;
            case EARTHQUAKE:
                packageDesc = "Rubble Clearing Helmets & Family Relief Tents";
                break;
            default:
                packageDesc = "General Emergency Relief Packs";
        }

        ReliefNeed need = new ReliefNeed(
                needId,
                incident.getId(),
                incident.getLocation(),
                incident.getDisasterType(),
                "Post-disaster community rehabilitation for incident " + incident.getId() + " at " + incident.getLocation(),
                targetFunding,
                packageDesc,
                targetPackages
        );

        reliefNeedRepository.save(need);
        log.info("❤️ [POST-DISASTER RELIEF DRIVE CREATED] {}", need);
    }

    public synchronized DonationRecord donateToNeed(String needId, DonationRecord donation,
                                                    String donorPhone, String donorAddress,
                                                    String paymentChannel, boolean anonymous)
            throws InvalidIncidentException {
        ReliefNeed need = reliefNeedRepository.findById(needId)
                .orElseThrow(() -> new InvalidIncidentException("Relief campaign not found: " + needId));

        if (donation == null) {
            throw new InvalidIncidentException("Donation payload cannot be null.");
        }
        if (donation.getAmountBDT() <= 0) {
            throw new InvalidIncidentException("Donation amount must be greater than zero.");
        }
        if (donation.getDonationId() == null || donation.getDonationId().isEmpty()
                || donationRecordRepository.existsById(donation.getDonationId())) {
            // Collision-safe ID generation instead of timestamp-modulo truncation
            donation.setDonationId("DON-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        donation.setReliefNeedId(needId);

        // Sensitive donor PII is encrypted at rest (AES-256-GCM) and never exposed publicly
        donation.setDonorPhoneEnc(encryptionService.encrypt(donorPhone));
        donation.setDonorAddressEnc(encryptionService.encrypt(donorAddress));
        donation.setPaymentChannel(paymentChannel);
        donation.setAnonymous(anonymous);

        DonationRecord savedDonation = donationRecordRepository.save(donation);
        need.recordDonation(savedDonation);
        reliefNeedRepository.save(need);

        return savedDonation;
    }

    /**
     * ADMIN-ONLY donor registry: every donation with fully decrypted donor contact details.
     * Callers must enforce role checks before invoking.
     */
    public List<Map<String, Object>> getDonorRegistry() {
        List<Map<String, Object>> registry = new ArrayList<>();
        for (DonationRecord d : donationRecordRepository.findAll()) {
            registry.add(d.toAdminJson(
                    encryptionService.decrypt(d.getDonorPhoneEnc()),
                    encryptionService.decrypt(d.getDonorAddressEnc())
            ));
        }
        registry.sort((a, b) -> String.valueOf(b.get("formattedTimestamp"))
                .compareTo(String.valueOf(a.get("formattedTimestamp"))));
        return registry;
    }

    // --- Query Methods ---

    public List<Incident> getAllIncidents() {
        List<Incident> list = incidentRepository.findAll();
        list.sort((a, b) -> Double.compare(b.getPriority(), a.getPriority()));
        return list;
    }

    public Incident getIncidentById(String id) {
        return incidentRepository.findById(id).orElse(null);
    }

    public List<DonationRecord> getAllDonations() {
        return donationRecordRepository.findAll();
    }

    public Incident peekHighestUrgencyIncident() {
        return urgentQueue.peek();
    }

    public List<ResponseTeam> getAllResponseTeams() {
        return responseTeamRepository.findAll();
    }

    public ResponseTeam getTeamById(String id) {
        return responseTeamRepository.findById(id).orElse(null);
    }

    public List<ReliefNeed> getAllReliefNeeds() {
        return reliefNeedRepository.findAll();
    }

    public ReliefNeed getReliefNeedById(String id) {
        return reliefNeedRepository.findById(id).orElse(null);
    }

    public StatusLogger getStatusLogger() {
        return statusLogger;
    }

    public VolunteerNotifier getVolunteerNotifier() {
        return volunteerNotifier;
    }

    public EmergencyEscalationNotifier getEscalationNotifier() {
        return escalationNotifier;
    }

    // --- Analytics Methods ---

    public Map<DisasterType, Integer> getIncidentCountsByType() {
        Map<DisasterType, Integer> map = new EnumMap<>(DisasterType.class);
        for (DisasterType dt : DisasterType.values()) {
            map.put(dt, 0);
        }
        for (Incident inc : incidentRepository.findAll()) {
            map.put(inc.getDisasterType(), map.get(inc.getDisasterType()) + 1);
        }
        return map;
    }

    public List<Map<String, Object>> getHotspots() {
        Map<String, List<Incident>> grouped = new HashMap<>();
        for (Incident inc : incidentRepository.findAll()) {
            String loc = inc.getLocation() != null ? inc.getLocation().trim() : "Unknown";
            grouped.computeIfAbsent(loc, k -> new ArrayList<>()).add(inc);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Incident>> entry : grouped.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("location", entry.getKey());
            item.put("count", entry.getValue().size());
            item.put("totalInjured", entry.getValue().stream().mapToInt(Incident::getInjuredCount).sum());
            item.put("primaryType", entry.getValue().get(0).getDisasterType().name());
            item.put("alertLevel", entry.getValue().size() >= 2 ? "HIGH RISK" : "MODERATE");
            result.add(item);
        }

        result.sort((a, b) -> Integer.compare((int) b.get("count"), (int) a.get("count")));
        return result;
    }

    // =====================================================================
    // Volunteer registry & signals
    // =====================================================================

    /**
     * ADMIN-ONLY volunteer registry: all volunteer units grouped by their team type,
     * plus individual volunteer accounts per type.
     */
    public Map<String, Object> getVolunteerRegistry() {
        Map<String, Object> registry = new LinkedHashMap<>();

        // Units grouped by VolunteerTeamType
        Map<String, List<Map<String, Object>>> unitsByType = new LinkedHashMap<>();
        for (VolunteerTeamType type : VolunteerTeamType.values()) {
            unitsByType.put(type.name(), new ArrayList<>());
        }
        for (ResponseTeam team : responseTeamRepository.findAll()) {
            if (team instanceof Volunteer v && v.getTeamType() != null) {
                Map<String, Object> unit = new LinkedHashMap<>();
                unit.put("teamId", v.getId());
                unit.put("name", v.getName());
                unit.put("teamType", v.getTeamType().name());
                unit.put("teamTypeLabel", v.getTeamType().getIcon() + " " + v.getTeamType().getDisplayName());
                unit.put("memberCount", v.getVolunteerMemberCount());
                unit.put("district", v.getCommunityDistrict());
                unit.put("contact", v.getContactNumber());
                unit.put("leader", v.getLeaderName());
                unit.put("status", v.getStatus().name());
                unit.put("missionLocked", v.isMissionLocked());
                unit.put("lastLat", v.getLastLatitude());
                unit.put("lastLon", v.getLastLongitude());
                unit.put("skillTags", v.getSkillTags());
                unitsByType.get(v.getTeamType().name()).add(unit);
            }
        }
        registry.put("unitsByType", unitsByType);

        // Individual volunteer accounts grouped by their chosen type
        Map<String, List<Map<String, Object>>> accountsByType = new LinkedHashMap<>();
        for (VolunteerTeamType type : VolunteerTeamType.values()) {
            accountsByType.put(type.name(), new ArrayList<>());
        }
        for (User u : userAuthService.getAllUsers()) {
            if (u.getRole() == Role.VOLUNTEER && u.getVolunteerTeamType() != null) {
                Map<String, Object> acc = new LinkedHashMap<>();
                acc.put("username", u.getUsername());
                acc.put("trustScore", u.getTrustScore());
                acc.put("active", u.isActive());
                acc.put("teamType", u.getVolunteerTeamType().name());
                acc.put("teamTypeLabel", u.getVolunteerTeamType().getIcon() + " " + u.getVolunteerTeamType().getDisplayName());
                accountsByType.get(u.getVolunteerTeamType().name()).add(acc);
            }
        }
        registry.put("accountsByType", accountsByType);

        return registry;
    }

    /**
     * ADMIN: sends a signal (deployment confirmation, standby, stand-down, move, broadcast)
     * to a volunteer team and/or user. Persisted so volunteers see it in their inbox.
     */
    public synchronized VolunteerSignal sendVolunteerSignal(VolunteerSignal.SignalType type, String targetTeamId,
                                                            String targetUsername, String incidentId,
                                                            String message, String location, String adminUsername)
            throws InvalidIncidentException {
        if ((targetTeamId == null || targetTeamId.isBlank())
                && (targetUsername == null || targetUsername.isBlank())) {
            throw new InvalidIncidentException("Signal needs a target team or volunteer username.");
        }
        if (targetTeamId != null && !targetTeamId.isBlank()
                && responseTeamRepository.findById(targetTeamId).isEmpty()) {
            throw new InvalidIncidentException("Volunteer team not found: " + targetTeamId);
        }

        String signalId = "SIG-" + String.format("%04d", signalCounter++);
        VolunteerSignal signal = new VolunteerSignal(signalId, type, targetTeamId, targetUsername,
                incidentId, message, location, adminUsername);
        VolunteerSignal saved = volunteerSignalRepository.save(signal);

        log.info("📡 [VOLUNTEER SIGNAL] {} -> team={} user={} (incident: {})", type, targetTeamId, targetUsername, incidentId);

        // Mirror deployment confirmations into the broadcast alert feed
        if (type == VolunteerSignal.SignalType.DEPLOYMENT_CONFIRMED) {
            String alertMsg = "ADMIN CONFIRMED DEPLOYMENT: " + (message != null ? message : "Proceed to assigned mission.");
            volunteerNotifier.getActiveAlerts(); // touch to keep feed warm
            statusLogger.logExternal("SIGNAL_SENT", incidentId != null ? incidentId : "-",
                    "Deployment confirmed for team " + targetTeamId + ": " + alertMsg);
        }
        return saved;
    }

    /** Signals targeted at a team (for team members) merged with signals for a username. */
    public List<VolunteerSignal> getAllSignals() {
        List<VolunteerSignal> all = volunteerSignalRepository.findAll();
        all.sort((a, b) -> b.getSentAt().compareTo(a.getSentAt()));
        return all;
    }

    public List<VolunteerSignal> getSignalsForVolunteer(String username, String teamId) {
        List<VolunteerSignal> result = new ArrayList<>();
        if (teamId != null && !teamId.isBlank()) {
            result.addAll(volunteerSignalRepository.findByTargetTeamIdOrderBySentAtDesc(teamId));
        }
        if (username != null && !username.isBlank()) {
            result.addAll(volunteerSignalRepository.findByTargetUsernameOrderBySentAtDesc(username));
        }
        result.sort((a, b) -> b.getSentAt().compareTo(a.getSentAt()));
        return result;
    }

    /**
     * A volunteer's full inbox: signals addressed to their username PLUS signals sent to
     * any volunteer unit sharing their team type (deployment confirmations for their unit type).
     */
    public List<VolunteerSignal> getInboxForUser(User user) {
        Map<String, VolunteerSignal> unique = new LinkedHashMap<>();

        volunteerSignalRepository.findByTargetUsernameOrderBySentAtDesc(user.getUsername())
                .forEach(s -> unique.putIfAbsent(s.getSignalId(), s));

        if (user.getRole() == Role.VOLUNTEER && user.getVolunteerTeamType() != null) {
            for (ResponseTeam team : responseTeamRepository.findAll()) {
                if (team instanceof Volunteer v && v.getTeamType() == user.getVolunteerTeamType()) {
                    volunteerSignalRepository.findByTargetTeamIdOrderBySentAtDesc(team.getId())
                            .forEach(s -> unique.putIfAbsent(s.getSignalId(), s));
                }
            }
        }

        List<VolunteerSignal> result = new ArrayList<>(unique.values());
        result.sort((a, b) -> b.getSentAt().compareTo(a.getSentAt()));
        return result;
    }

    public synchronized VolunteerSignal acknowledgeSignal(String signalId, String username)
            throws InvalidIncidentException {
        VolunteerSignal signal = volunteerSignalRepository.findById(signalId)
                .orElseThrow(() -> new InvalidIncidentException("Signal not found: " + signalId));
        signal.setStatus(VolunteerSignal.SignalStatus.ACKNOWLEDGED);
        signal.setAcknowledgedAt(LocalDateTime.now());
        return volunteerSignalRepository.save(signal);
    }

    // =====================================================================
    // Admin verification methods (GPS pings, thermal scans, M/ETHANE, ...)
    // =====================================================================

    /** Weight total needed for an incident to be admin-verified automatically. */
    private static final int VERIFICATION_WEIGHT_THRESHOLD = 3;

    /**
     * ADMIN: runs one structured verification check against an incident.
     * Accumulated passed weight >= threshold auto-verifies the incident and
     * rewards the reporter (same +5 trust loop as crowd verification).
     */
    public synchronized Map<String, Object> runVerificationCheck(String incidentId, VerificationMethod method,
                                                                 boolean passed, String notes, String adminUsername)
            throws InvalidIncidentException {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new InvalidIncidentException("Incident not found: " + incidentId));

        String checkId = "CHK-" + String.format("%04d", checkCounter++);
        VerificationCheck check = new VerificationCheck(checkId, method, incidentId, passed, notes, adminUsername);
        verificationCheckRepository.save(check);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkId", checkId);
        result.put("method", method.name());
        result.put("methodLabel", method.getIcon() + " " + method.getDisplayName());
        result.put("passed", passed);
        result.put("notes", notes);

        if (passed && incident.getStatus() == IncidentStatus.REPORTED) {
            int weight = verificationCheckRepository.findByIncidentIdOrderByCheckedAtDesc(incidentId).stream()
                    .filter(VerificationCheck::isPassed)
                    .mapToInt(c -> c.getMethod().getWeight())
                    .sum();
            result.put("accumulatedWeight", weight);

            if (weight >= VERIFICATION_WEIGHT_THRESHOLD) {
                incident.setStatus(IncidentStatus.VERIFIED);
                User reporter = userAuthService.getUser(incident.getReporterUsername());
                if (reporter != null) {
                    userAuthService.adjustTrustScore(reporter.getUsername(), VERIFICATION_REWARD);
                    incident.setReporterTrustScore(Math.min(100, reporter.getTrustScore() + VERIFICATION_REWARD));
                }
                incidentRepository.save(incident);
                notifyVerified(incident);
                result.put("autoVerified", true);
            }
        } else if (!passed) {
            result.put("accumulatedWeight", 0);
        }

        statusLogger.logExternal("VERIFICATION_CHECK", incidentId,
                method.getDisplayName() + " " + (passed ? "PASSED" : "FLAGGED") + " by " + adminUsername);
        return result;
    }

    public List<VerificationCheck> getVerificationChecks(String incidentId) {
        return verificationCheckRepository.findByIncidentIdOrderByCheckedAtDesc(incidentId);
    }

    // =====================================================================
    // Mission reports (generated on resolve) & resource matching
    // =====================================================================

    /**
     * Resources whose type matches the disaster requirement AND are currently AVAILABLE.
     * FIRE -> VEHICLE / HEAVY_EQUIPMENT; FLOOD -> VEHICLE / RESCUE_GEAR;
     * ACCIDENT -> VEHICLE / MEDICAL_UNIT; EARTHQUAKE -> HEAVY_EQUIPMENT / RESCUE_GEAR.
     */
    public List<Map<String, Object>> getMatchedAvailableResources(DisasterType type) {
        List<Resource.Type> wanted = switch (type) {
            case FIRE -> List.of(Resource.Type.VEHICLE, Resource.Type.HEAVY_EQUIPMENT);
            case FLOOD -> List.of(Resource.Type.VEHICLE, Resource.Type.RESCUE_GEAR);
            case ACCIDENT -> List.of(Resource.Type.VEHICLE, Resource.Type.MEDICAL_UNIT);
            case EARTHQUAKE -> List.of(Resource.Type.HEAVY_EQUIPMENT, Resource.Type.RESCUE_GEAR);
        };

        List<Map<String, Object>> matched = new ArrayList<>();
        for (ResponseTeam team : responseTeamRepository.findAll()) {
            if (team.isStatusAvailable()) {
                for (Resource r : team.getResources()) {
                    if (wanted.contains(r.getType())) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("resourceId", r.getId());
                        item.put("name", r.getName());
                        item.put("type", r.getType().name());
                        item.put("quantity", r.getQuantity());
                        item.put("condition", r.getOperationalCondition());
                        item.put("heldByTeam", team.getId());
                        item.put("teamName", team.getName());
                        item.put("teamBase", team.getBaseStation());
                        matched.add(item);
                    }
                }
            }
        }
        return matched;
    }

    public List<MissionReport> getAllMissionReports() {
        return missionReportRepository.findAllByOrderByGeneratedAtDesc();
    }

    public List<MissionReport> getMissionReportsForIncident(String incidentId) {
        return missionReportRepository.findByIncidentId(incidentId);
    }

    // =====================================================================
    // Team operations & management (admin blueprint)
    // =====================================================================

    /** ADMIN: teams within radiusKm of a point, optionally filtered by skill tag. */
    public List<Map<String, Object>> findNearbyTeams(double lat, double lon, double radiusKm, String skillTag) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ResponseTeam team : responseTeamRepository.findAll()) {
            double dist = team.distanceKmTo(lat, lon);
            if (dist <= radiusKm) {
                boolean skillMatch = skillTag == null || skillTag.isBlank()
                        || (team.getSkillTags() != null && team.getSkillTags().toLowerCase().contains(skillTag.toLowerCase()));
                if (skillMatch) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("teamId", team.getId());
                    item.put("name", team.getName());
                    item.put("specialization", team.getSpecialization());
                    item.put("status", team.getStatus().name());
                    item.put("distanceKm", Math.round(dist * 10.0) / 10.0);
                    item.put("leader", team.getLeaderName());
                    item.put("skillTags", team.getSkillTags());
                    item.put("missionLocked", team.isMissionLocked());
                    result.add(item);
                }
            }
        }
        result.sort((a, b) -> Double.compare((double) a.get("distanceKm"), (double) b.get("distanceKm")));
        return result;
    }

    /** ADMIN: dynamically assemble an ad-hoc unit from members of a team. */
    public synchronized ResponseTeam assembleAdHocUnit(String sourceTeamId, String unitName, int memberCount,
                                                       String leaderName, String adminUsername)
            throws InvalidIncidentException {
        ResponseTeam source = responseTeamRepository.findById(sourceTeamId)
                .orElseThrow(() -> new InvalidIncidentException("Source team not found: " + sourceTeamId));
        if (!(source instanceof Volunteer srcVol)) {
            throw new InvalidIncidentException("Ad-hoc assembly currently supports volunteer units only.");
        }
        if (memberCount < 1 || memberCount > srcVol.getVolunteerMemberCount()) {
            throw new InvalidIncidentException("Member count must be between 1 and " + srcVol.getVolunteerMemberCount());
        }

        Volunteer unit = new Volunteer(
                "TEAM-ADHOC-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase(),
                unitName,
                source.getContactNumber(),
                source.getBaseStation(),
                memberCount,
                srcVol.getCommunityDistrict()
        );
        unit.setTeamType(srcVol.getTeamType());
        unit.setLeaderName(leaderName != null ? leaderName : source.getLeaderName());
        unit.setSkillTags(source.getSkillTags());
        unit.setLastLatitude(source.getLastLatitude());
        unit.setLastLongitude(source.getLastLongitude());
        unit.setStatus(TeamStatus.STANDBY);
        return responseTeamRepository.save(unit);
    }

    /** ADMIN: assign/change a team leader. */
    public synchronized ResponseTeam assignTeamLeader(String teamId, String leaderName, String adminUsername)
            throws InvalidIncidentException {
        ResponseTeam team = responseTeamRepository.findById(teamId)
                .orElseThrow(() -> new InvalidIncidentException("Team not found: " + teamId));
        team.setLeaderName(leaderName);
        return responseTeamRepository.save(team);
    }

    /** ADMIN: tag skills & certifications on a team ("first-aid, swift-water"). */
    public synchronized ResponseTeam setTeamSkills(String teamId, String skillTags, String adminUsername)
            throws InvalidIncidentException {
        ResponseTeam team = responseTeamRepository.findById(teamId)
                .orElseThrow(() -> new InvalidIncidentException("Team not found: " + teamId));
        team.setSkillTags(skillTags);
        return responseTeamRepository.save(team);
    }

    /** ADMIN: update live GPS position of a unit (simulated GPS ping / check-in). */
    public synchronized ResponseTeam updateUnitPosition(String teamId, double lat, double lon, String adminUsername)
            throws InvalidIncidentException {
        ResponseTeam team = responseTeamRepository.findById(teamId)
                .orElseThrow(() -> new InvalidIncidentException("Team not found: " + teamId));
        team.setLastLatitude(lat);
        team.setLastLongitude(lon);
        ResponseTeam saved = responseTeamRepository.save(team);

        // Geofence boundary alarm
        if (team.getGeofenceRadiusMeters() != null) {
            double driftKm = team.distanceKmTo(team.getBaseLatitude(), team.getBaseLongitude());
            if (driftKm * 1000 > team.getGeofenceRadiusMeters()) {
                statusLogger.logExternal("GEOFENCE_ALARM", "-",
                        team.getId() + " drifted " + Math.round(driftKm * 1000) + "m outside its geofence!");
            }
        }
        return saved;
    }

    /** ADMIN: toggle Mission Lock Mode (unit locked to current mission, not re-dispatchable). */
    public synchronized ResponseTeam toggleMissionLock(String teamId, String adminUsername)
            throws InvalidIncidentException {
        ResponseTeam team = responseTeamRepository.findById(teamId)
                .orElseThrow(() -> new InvalidIncidentException("Team not found: " + teamId));
        team.setMissionLocked(!team.isMissionLocked());
        return responseTeamRepository.save(team);
    }

    /** ADMIN or RESPONSE_TEAM: instant unit demobilization - stand down & free regardless of prior state. */
    public synchronized ResponseTeam demobilizeUnit(String teamId, String actorUsername)
            throws InvalidIncidentException {
        ResponseTeam team = responseTeamRepository.findById(teamId)
                .orElseThrow(() -> new InvalidIncidentException("Team not found: " + teamId));

        String incidentId = team.getCurrentIncidentId();
        team.release();
        team.setMissionLocked(false);
        ResponseTeam saved = responseTeamRepository.save(team);

        if (incidentId != null) {
            Incident incident = incidentRepository.findById(incidentId).orElse(null);
            if (incident != null && incident.getAssignedTeamId() != null
                    && incident.getAssignedTeamId().equals(teamId)
                    && incident.getStatus() != IncidentStatus.RESOLVED) {
                incident.setAssignedTeamId(null);
                incident.setStatus(IncidentStatus.VERIFIED);
                incidentRepository.save(incident);
            }
        }
        statusLogger.logExternal("DEMobilized".toUpperCase(), incidentId != null ? incidentId : "-",
                "Unit " + team.getId() + " demobilized by " + actorUsername);
        return saved;
    }

    /**
     * ADMIN: broadcast alert to all volunteer units of given team types (or all when null).
     * Sends BROADCAST signals to every matching volunteer team.
     */
    public synchronized List<VolunteerSignal> broadcastToVolunteers(List<VolunteerTeamType> types, String message,
                                                                    String incidentId, String adminUsername)
            throws InvalidIncidentException {
        List<VolunteerSignal> sent = new ArrayList<>();
        for (ResponseTeam team : responseTeamRepository.findAll()) {
            if (team instanceof Volunteer v) {
                if (types == null || types.isEmpty() || (v.getTeamType() != null && types.contains(v.getTeamType()))) {
                    String signalId = "SIG-" + String.format("%04d", signalCounter++);
                    VolunteerSignal signal = new VolunteerSignal(signalId,
                            VolunteerSignal.SignalType.BROADCAST, v.getId(), null,
                            incidentId, message, null, adminUsername);
                    sent.add(volunteerSignalRepository.save(signal));
                }
            }
        }
        statusLogger.logExternal("BROADCAST", incidentId != null ? incidentId : "-",
                "Broadcast to " + sent.size() + " volunteer units: " + message);
        return sent;
    }

    public double getAverageResponseTimeMinutes() {
        List<Incident> resolved = incidentRepository.findByStatus(IncidentStatus.RESOLVED);
        if (resolved.isEmpty()) return 14.5; // Baseline realistic estimate

        long totalMinutes = 0;
        for (Incident inc : resolved) {
            if (inc.getResolvedTimestamp() != null && inc.getTimestamp() != null) {
                totalMinutes += Math.max(1, Duration.between(inc.getTimestamp(), inc.getResolvedTimestamp()).toMinutes());
            } else {
                totalMinutes += 15;
            }
        }
        return Math.round(((double) totalMinutes / resolved.size()) * 10.0) / 10.0;
    }
}

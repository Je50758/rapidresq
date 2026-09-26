package patterns;

import auth.User;
import auth.UserAuthenticationService;
import exceptions.InvalidIncidentException;
import exceptions.NoTeamAvailableException;
import models.*;
import observers.EmergencyEscalationNotifier;
import observers.IncidentObserver;
import observers.StatusLogger;
import observers.VolunteerNotifier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton Controller for the Disaster Response Management System.
 * Coordinates incident intake, priority triage, team dispatching,
 * crowd verification, automated 999 escalations, and post-disaster donation tracking.
 */
public class DisasterManagementSystem {
    private static volatile DisasterManagementSystem instance;

    // Core Data structures
    private final Map<String, Incident> allIncidents;
    private final PriorityQueue<Incident> urgentQueue;
    private final List<ResponseTeam> responseTeams;
    private final List<IncidentObserver> observers;

    // Post-Disaster Rehabilitation & Donations
    private final List<ReliefNeed> reliefNeeds;
    private int reliefCounter = 1;

    // Default system observers
    private final StatusLogger statusLogger;
    private final VolunteerNotifier volunteerNotifier;
    private final EmergencyEscalationNotifier escalationNotifier;

    private int incidentCounter = 100;

    /**
     * Private constructor for Singleton Pattern.
     */
    private DisasterManagementSystem() {
        this.allIncidents = new ConcurrentHashMap<>();
        this.urgentQueue = new PriorityQueue<>();
        this.responseTeams = new CopyOnWriteArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();
        this.reliefNeeds = new CopyOnWriteArrayList<>();

        this.statusLogger = new StatusLogger();
        this.volunteerNotifier = new VolunteerNotifier();
        this.escalationNotifier = new EmergencyEscalationNotifier();

        registerObserver(statusLogger);
        registerObserver(volunteerNotifier);
        registerObserver(escalationNotifier);

        // Preload baseline demo data
        preloadDemoData();
    }

    public static DisasterManagementSystem getInstance() {
        if (instance == null) {
            synchronized (DisasterManagementSystem.class) {
                if (instance == null) {
                    instance = new DisasterManagementSystem();
                }
            }
        }
        return instance;
    }

    // --- Observer Management ---

    public void registerObserver(IncidentObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(IncidentObserver observer) {
        observers.remove(observer);
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

    // --- Core Operations ---

    /**
     * Reports a new incident. If reporter trust score < 20, flags as low-trust unverified.
     */
    public synchronized void reportIncident(Incident incident) throws InvalidIncidentException {
        if (incident == null) {
            throw new InvalidIncidentException("Incident payload cannot be null.");
        }
        if (incident.getId() == null || incident.getId().trim().isEmpty()) {
            throw new InvalidIncidentException("Incident ID cannot be empty.");
        }
        if (incident.getLocation() == null || incident.getLocation().trim().isEmpty()) {
            throw new InvalidIncidentException("Incident location is required for emergency dispatch.");
        }

        // Feature 1: Low-Trust Reporter check
        if (incident.getReporterTrustScore() < 20) {
            incident.setDescription("[⚠️ UNVERIFIED - LOW TRUST REPORTER] " + incident.getDescription());
        }

        allIncidents.put(incident.getId(), incident);
        urgentQueue.offer(incident);

        notifyReported(incident);
    }

    public synchronized String generateNextIncidentId() {
        return "INC-" + (++incidentCounter);
    }

    /**
     * Citizen confirmation: 3+ reports trigger auto-VERIFIED and reward reporter's trustScore!
     */
    public synchronized void addConfirmingReport(String incidentId, String reporterUsername) throws InvalidIncidentException {
        Incident incident = allIncidents.get(incidentId);
        if (incident == null) {
            throw new InvalidIncidentException("Incident ID not found: " + incidentId);
        }

        incident.incrementReportCount();

        // Auto-verification threshold: 3+ reports
        if (incident.getReportCount() >= 3 && incident.getStatus() == IncidentStatus.REPORTED) {
            incident.setStatus(IncidentStatus.VERIFIED);

            // Reward initial reporter's trustScore for lodging a verified disaster report (+5)
            User initialReporter = UserAuthenticationService.getInstance().getUser(incident.getReporterUsername());
            if (initialReporter != null) {
                initialReporter.adjustTrustScore(5);
                incident.setReporterTrustScore(initialReporter.getTrustScore());
                System.out.println("⭐ [TRUST SCORE BOOST] @" + initialReporter.getUsername() + 
                                   " rewarded +5 for verified report. New Score: " + initialReporter.getTrustScore() + "%");
            }

            notifyVerified(incident);
        }

        // Re-triage queue
        urgentQueue.remove(incident);
        if (incident.getStatus() != IncidentStatus.RESOLVED) {
            urgentQueue.offer(incident);
        }
    }

    /**
     * Penalizes false/fake disaster alarms. Reduces reporter trust score by -20.
     */
    public synchronized void markIncidentAsFalseAlarm(String incidentId) throws InvalidIncidentException {
        Incident incident = allIncidents.get(incidentId);
        if (incident == null) {
            throw new InvalidIncidentException("Incident not found: " + incidentId);
        }

        // Penalize reporter
        User reporter = UserAuthenticationService.getInstance().getUser(incident.getReporterUsername());
        if (reporter != null) {
            reporter.adjustTrustScore(-20);
            incident.setReporterTrustScore(reporter.getTrustScore());
            System.out.println("❌ [TRUST SCORE PENALTY] @" + reporter.getUsername() + 
                               " penalized -20 for false report. New Score: " + reporter.getTrustScore() + "%");
        }

        // Mark resolved and remove from urgent queue
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setDescription("[FALSE ALARM / SPAM CLEARED] " + incident.getDescription());
        incident.setPriority(0.0);
        urgentQueue.remove(incident);

        // Free team if assigned
        if (incident.getAssignedTeamId() != null) {
            ResponseTeam team = getTeamById(incident.getAssignedTeamId());
            if (team != null) team.release();
        }

        notifyResolved(incident);
    }

    /**
     * Assigns best available response team.
     */
    public synchronized ResponseTeam assignTeam(String incidentId) throws NoTeamAvailableException, InvalidIncidentException {
        Incident incident = allIncidents.get(incidentId);
        if (incident == null) {
            throw new InvalidIncidentException("Incident not found: " + incidentId);
        }
        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new InvalidIncidentException("Cannot assign team: Incident is already RESOLVED.");
        }

        ResponseTeam chosenTeam = null;
        for (ResponseTeam team : responseTeams) {
            if (team.isAvailable() && team.canHandle(incident)) {
                chosenTeam = team;
                break;
            }
        }

        if (chosenTeam == null) {
            throw new NoTeamAvailableException(
                    incidentId,
                    incident.getDisasterType().name(),
                    "No available " + incident.getDisasterType() + " response team currently available."
            );
        }

        chosenTeam.assignToIncident(incidentId);
        incident.setAssignedTeamId(chosenTeam.getId());
        incident.setStatus(IncidentStatus.IN_PROGRESS);

        urgentQueue.remove(incident);
        urgentQueue.offer(incident);

        notifyTeamAssigned(incident, chosenTeam);
        return chosenTeam;
    }

    /**
     * Resolves incident, releases response units, and automatically creates
     * a post-disaster rehabilitation relief drive for the affected community.
     */
    public synchronized void resolveIncident(String incidentId) throws InvalidIncidentException {
        Incident incident = allIncidents.get(incidentId);
        if (incident == null) {
            throw new InvalidIncidentException("Incident not found: " + incidentId);
        }

        incident.setStatus(IncidentStatus.RESOLVED);

        // Release team if assigned
        if (incident.getAssignedTeamId() != null) {
            for (ResponseTeam team : responseTeams) {
                if (team.getId().equals(incident.getAssignedTeamId())) {
                    team.release();
                    break;
                }
            }
        }

        urgentQueue.remove(incident);
        notifyResolved(incident);

        // Feature 3: Automatically generate post-disaster community relief need
        createReliefNeedForResolvedIncident(incident);
    }

    private void createReliefNeedForResolvedIncident(Incident incident) {
        String needId = "RELIEF-" + String.format("%03d", reliefCounter++);
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

        reliefNeeds.add(need);
        System.out.println("❤️ [POST-DISASTER RELIEF DRIVE CREATED] " + need);
    }

    public synchronized void donateToNeed(String needId, DonationRecord donation) {
        for (ReliefNeed need : reliefNeeds) {
            if (need.getNeedId().equalsIgnoreCase(needId)) {
                need.recordDonation(donation);
                System.out.println("🎁 [DONATION MATCHED] " + donation + " -> " + need.getNeedId());
                break;
            }
        }
    }

    public List<ReliefNeed> getAllReliefNeeds() {
        return Collections.unmodifiableList(reliefNeeds);
    }

    public ReliefNeed getReliefNeedById(String needId) {
        for (ReliefNeed n : reliefNeeds) {
            if (n.getNeedId().equalsIgnoreCase(needId)) return n;
        }
        return null;
    }

    public Incident peekHighestUrgencyIncident() {
        return urgentQueue.peek();
    }

    public Incident pollHighestUrgencyIncident() {
        return urgentQueue.poll();
    }

    public void registerResponseTeam(ResponseTeam team) {
        if (team != null && !responseTeams.contains(team)) {
            responseTeams.add(team);
        }
    }

    public List<ResponseTeam> getAllResponseTeams() {
        return Collections.unmodifiableList(responseTeams);
    }

    public ResponseTeam getTeamById(String teamId) {
        for (ResponseTeam team : responseTeams) {
            if (team.getId().equalsIgnoreCase(teamId)) return team;
        }
        return null;
    }

    public Collection<Incident> getAllIncidents() {
        return Collections.unmodifiableCollection(allIncidents.values());
    }

    public Incident getIncidentById(String id) {
        return allIncidents.get(id);
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

    private void preloadDemoData() {
        // Teams
        FireBrigade fb1 = new FireBrigade("TEAM-FB01", "Dhaka Central Fire Unit 5", "+8801711000101", "Sadarghat Fire Station", 45);
        fb1.addResource(new Resource("RES-01", "Water Foam Tender Truck", Resource.Type.VEHICLE, 2, "Optimal"));
        fb1.addResource(new Resource("RES-02", "High-Pressure Hydraulic Hose", Resource.Type.HEAVY_EQUIPMENT, 6, "Optimal"));
        fb1.addResource(new Resource("RES-03", "Thermal Heat Imaging Drone", Resource.Type.EQUIPMENT, 1, "Optimal"));
        registerResponseTeam(fb1);

        MedicalTeam med1 = new MedicalTeam("TEAM-MED01", "Red Crescent Trauma Unit Alpha", "+8801811000202", "Dhaka Medical College Hub", 6, true);
        med1.addResource(new Resource("RES-04", "Advanced Life Support Ambulance", Resource.Type.VEHICLE, 2, "Optimal"));
        med1.addResource(new Resource("RES-05", "Emergency Portable Defibrillator", Resource.Type.MEDICAL_UNIT, 3, "Optimal"));
        med1.addResource(new Resource("RES-06", "Triage Trauma Stabilization Kit", Resource.Type.RESCUE_GEAR, 10, "Optimal"));
        registerResponseTeam(med1);

        RescueTeam rsc1 = new RescueTeam("TEAM-RSC01", "Coast Guard & Rapid Disaster Response", "+8801911000303", "Sylhet Sadar Station", true, true);
        rsc1.addResource(new Resource("RES-07", "Inflatable Motorized Rescue Boat", Resource.Type.VEHICLE, 4, "Optimal"));
        rsc1.addResource(new Resource("RES-08", "Hydraulic Concrete Jaws & Cutter", Resource.Type.HEAVY_EQUIPMENT, 2, "Optimal"));
        rsc1.addResource(new Resource("RES-09", "Buoyancy Vests & Life Rings", Resource.Type.RESCUE_GEAR, 50, "Optimal"));
        registerResponseTeam(rsc1);

        Volunteer vol1 = new Volunteer("TEAM-VOL01", "Mirpur Community First Responders", "+8801611000404", "Mirpur-10 Sector Ward", 28, "Mirpur Dhaka");
        vol1.addResource(new Resource("RES-10", "Community First Aid Packs", Resource.Type.RESCUE_GEAR, 15, "Optimal"));
        vol1.addResource(new Resource("RES-11", "Megaphones & Night Torches", Resource.Type.COMMUNICATION_KIT, 8, "Optimal"));
        registerResponseTeam(vol1);

        // Preload Incidents
        try {
            FireIncident inc1 = new FireIncident(
                    "INC-101",
                    "Korail Slum, Mohakhali, Dhaka",
                    4,
                    Severity.CRITICAL,
                    "citizen1",
                    75,
                    "Dense residential fire spreading towards gas cylinder storage.",
                    4,
                    true
            );
            inc1.incrementReportCount();
            inc1.incrementReportCount();
            inc1.setStatus(IncidentStatus.VERIFIED);
            allIncidents.put(inc1.getId(), inc1);
            urgentQueue.offer(inc1);

            FloodIncident inc2 = new FloodIncident(
                    "INC-102",
                    "Companyganj, Sylhet",
                    1,
                    Severity.MODERATE,
                    "volunteer1",
                    85,
                    "Surma river overflowed dyke; over 80 people stranded in school rooftop.",
                    2.8,
                    80
            );
            allIncidents.put(inc2.getId(), inc2);
            urgentQueue.offer(inc2);

            AccidentIncident inc3 = new AccidentIncident(
                    "INC-103",
                    "Dhaka-Chittagong Highway, Feni",
                    8,
                    Severity.CRITICAL,
                    "citizen1",
                    75,
                    "Head-on collision between passenger bus and freight trailer.",
                    3,
                    true
            );
            allIncidents.put(inc3.getId(), inc3);
            urgentQueue.offer(inc3);

            EarthquakeIncident inc4 = new EarthquakeIncident(
                    "INC-104",
                    "Armanitola, Old Dhaka",
                    0,
                    Severity.LOW,
                    "citizen1",
                    75,
                    "Minor structural fissure observed on old 4-story masonry wall after 4.2 tremor.",
                    4.2,
                    0
            );
            allIncidents.put(inc4.getId(), inc4);
            urgentQueue.offer(inc4);

            // Preload 999 Escalations for the critical incidents
            escalationNotifier.onIncidentReported(inc1);
            escalationNotifier.onIncidentReported(inc3);

            // Preload 1 sample Post-Disaster Relief Drive with an initial donation
            ReliefNeed sampleNeed = new ReliefNeed(
                    "RELIEF-001",
                    "INC-099",
                    "Sitakunda Industrial Zone, Chittagong",
                    DisasterType.FIRE,
                    "Rehabilitation and medical burn aid following resolved container chemical blaze.",
                    50000.0,
                    "Sterile Burn Dressing & Saline Packs",
                    50
            );
            sampleNeed.recordDonation(new DonationRecord("DON-01", "RELIEF-001", "volunteer1", 12000.0, "Cash Aid"));
            sampleNeed.recordDonation(new DonationRecord("DON-02", "RELIEF-001", "citizen1", 5000.0, "Sterile Burn Dressing"));
            reliefNeeds.add(sampleNeed);

        } catch (Exception e) {
            System.err.println("Error initializing demo incidents: " + e.getMessage());
        }
    }
}

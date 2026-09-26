package com.disaster.config;

import com.disaster.models.*;
import com.disaster.repository.*;
import com.disaster.service.DisasterManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * DataInitializer preloads realistic Bangladesh disaster baseline records,
 * pre-configured response teams with equipment (Composition), demo user credentials,
 * and post-disaster campaigns upon Spring Boot startup into the H2 database.
 */
@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ResponseTeamRepository responseTeamRepository;
    private final DisasterManagementService disasterService;
    private final ReliefNeedRepository reliefNeedRepository;
    private final com.disaster.repository.SafeLocationRepository safeLocationRepository;

    @Autowired
    public DataInitializer(UserRepository userRepository,
                           ResponseTeamRepository responseTeamRepository,
                           DisasterManagementService disasterService,
                           ReliefNeedRepository reliefNeedRepository,
                           com.disaster.repository.SafeLocationRepository safeLocationRepository) {
        this.userRepository = userRepository;
        this.responseTeamRepository = responseTeamRepository;
        this.disasterService = disasterService;
        this.reliefNeedRepository = reliefNeedRepository;
        this.safeLocationRepository = safeLocationRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("=============================================================");
        log.info(" 🚀 PRELOADING BANGLADESH DISASTER RESPONSE SYSTEM DEMO DATA");
        log.info("=============================================================");

        // 1. Preload Users
        if (userRepository.count() == 0) {
            userRepository.save(new User("admin", "admin123", Role.ADMIN, 100));
            userRepository.save(new User("citizen1", "pass123", Role.CITIZEN, 75));
            User vol1 = new User("volunteer1", "pass123", Role.VOLUNTEER, 85);
            vol1.setVolunteerTeamType(VolunteerTeamType.RESCUE);
            userRepository.save(vol1);
            User vol2 = new User("volunteer2", "pass123", Role.VOLUNTEER, 70);
            vol2.setVolunteerTeamType(VolunteerTeamType.MEDICAL_AID);
            userRepository.save(vol2);
            userRepository.save(new User("team1", "pass123", Role.RESPONSE_TEAM, 90));
            log.info(" -> Users preloaded: admin, citizen1, volunteer1 (RESCUE), volunteer2 (MEDICAL_AID), team1");
        }

        // 2. Preload Response Teams with Composed Resources (Composition)
        if (responseTeamRepository.count() == 0) {
            FireBrigade fb1 = new FireBrigade("TEAM-FB01", "Dhaka Central Fire Unit 5", "+8801711000101", "Sadarghat Fire Station", 45);
            fb1.addResource(new Resource("RES-01", "Water Foam Tender Truck", Resource.Type.VEHICLE, 2, "Optimal"));
            fb1.addResource(new Resource("RES-02", "High-Pressure Hydraulic Hose", Resource.Type.HEAVY_EQUIPMENT, 6, "Optimal"));
            fb1.addResource(new Resource("RES-03", "Thermal Heat Imaging Drone", Resource.Type.EQUIPMENT, 1, "Optimal"));
            responseTeamRepository.save(fb1);

            MedicalTeam med1 = new MedicalTeam("TEAM-MED01", "Red Crescent Trauma Unit Alpha", "+8801811000202", "Dhaka Medical College Hub", 6, true);
            med1.addResource(new Resource("RES-04", "Advanced Life Support Ambulance", Resource.Type.VEHICLE, 2, "Optimal"));
            med1.addResource(new Resource("RES-05", "Emergency Portable Defibrillator", Resource.Type.MEDICAL_UNIT, 3, "Optimal"));
            med1.addResource(new Resource("RES-06", "Triage Trauma Stabilization Kit", Resource.Type.RESCUE_GEAR, 10, "Optimal"));
            responseTeamRepository.save(med1);

            RescueTeam rsc1 = new RescueTeam("TEAM-RSC01", "Coast Guard & Rapid Disaster Response", "+8801911000303", "Sylhet Sadar Station", true, true);
            rsc1.addResource(new Resource("RES-07", "Inflatable Motorized Rescue Boat", Resource.Type.VEHICLE, 4, "Optimal"));
            rsc1.addResource(new Resource("RES-08", "Hydraulic Concrete Jaws & Cutter", Resource.Type.HEAVY_EQUIPMENT, 2, "Optimal"));
            rsc1.addResource(new Resource("RES-09", "Buoyancy Vests & Life Rings", Resource.Type.RESCUE_GEAR, 50, "Optimal"));
            responseTeamRepository.save(rsc1);

            Volunteer vol1 = new Volunteer("TEAM-VOL01", "Mirpur Community First Responders", "+8801611000404", "Mirpur-10 Sector Ward", 28, "Mirpur, Dhaka");
            vol1.setTeamType(VolunteerTeamType.RESCUE);
            vol1.setLeaderName("Kamal Hossain");
            vol1.setMemberCount(28);
            vol1.setSkillTags("swift-water, first-aid, evacuation");
            vol1.setBaseLatitude(23.8042);
            vol1.setBaseLongitude(90.3667);
            vol1.setLastLatitude(23.8042);
            vol1.setLastLongitude(90.3667);
            vol1.setGeofenceRadiusMeters(2000);
            vol1.addResource(new Resource("RES-10", "Community First Aid Packs", Resource.Type.RESCUE_GEAR, 15, "Optimal"));
            vol1.addResource(new Resource("RES-11", "Megaphones & Night Torches", Resource.Type.COMMUNICATION_KIT, 8, "Optimal"));
            responseTeamRepository.save(vol1);

            Volunteer vol2 = new Volunteer("TEAM-VOL02", "Dhanmondi Medical Volunteers", "+8801611000505", "Dhanmondi 27 Ward", 16, "Dhanmondi, Dhaka");
            vol2.setTeamType(VolunteerTeamType.MEDICAL_AID);
            vol2.setLeaderName("Dr. Sultana Rahman");
            vol2.setMemberCount(16);
            vol2.setSkillTags("first-aid, triage, medicine-distribution");
            vol2.setBaseLatitude(23.7461);
            vol2.setBaseLongitude(90.3742);
            vol2.setLastLatitude(23.7461);
            vol2.setLastLongitude(90.3742);
            responseTeamRepository.save(vol2);

            Volunteer vol3 = new Volunteer("TEAM-VOL03", "Uttara Logistics Corps", "+8801611000606", "Uttara Sector 4", 22, "Uttara, Dhaka");
            vol3.setTeamType(VolunteerTeamType.LOGISTICS);
            vol3.setLeaderName("Rafiq Islam");
            vol3.setMemberCount(22);
            vol3.setSkillTags("relief-packing, transport, warehouse");
            vol3.setBaseLatitude(23.8759);
            vol3.setBaseLongitude(90.3795);
            vol3.setLastLatitude(23.8759);
            vol3.setLastLongitude(90.3795);
            responseTeamRepository.save(vol3);

            log.info(" -> Response teams with composed resources preloaded.");
        }

        // 3. Preload Realistic Incidents
        if (disasterService.getAllIncidents().isEmpty()) {
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
            disasterService.reportIncident(inc1);

            FloodIncident inc2 = new FloodIncident(
                    "INC-102",
                    "Companyganj, Sylhet",
                    1,
                    Severity.MODERATE,
                    "volunteer1",
                    85,
                    "Surma river overflowed dyke; over 80 people stranded on rooftop.",
                    2.8,
                    80
            );
            disasterService.reportIncident(inc2);

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
            disasterService.reportIncident(inc3);

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
            disasterService.reportIncident(inc4);

            log.info(" -> Sample Bangladesh incidents preloaded and queued in PriorityQueue.");
        }

        // 4. Preload Post-Disaster Relief Drive
        if (reliefNeedRepository.count() == 0) {
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
            reliefNeedRepository.save(sampleNeed);
            log.info(" -> Sample post-disaster rehabilitation drive preloaded.");
        }

        // 5. Preload Shelters & Safe Areas (for public post-disaster community reports)
        if (safeLocationRepository.count() == 0) {
            safeLocationRepository.save(new SafeLocation("SAFE-01", "Mohakhali Municipal Shelter", "Bondor Thana Road, Mohakhali, Dhaka", SafeLocation.LocationType.SHELTER, 23.7827, 90.4004, 800, "+8801711000999", true, true));
            safeLocationRepository.save(new SafeLocation("SAFE-02", "Mohakhali Government High School", "Ges 27, Mohakhali DOHS", SafeLocation.LocationType.SCHOOL, 23.7888, 90.3973, 1200, "+8801711000998", true, false));
            safeLocationRepository.save(new SafeLocation("SAFE-03", "Gulshan Azad Mosque Complex", "Road 50, Gulshan 2", SafeLocation.LocationType.MOSQUE, 23.7925, 90.4078, 1500, "+8801711000997", true, false));
            safeLocationRepository.save(new SafeLocation("SAFE-04", "Dhaka Medical College Hospital", "Secretariat Road, Bakshibazar", SafeLocation.LocationType.HOSPITAL, 23.7259, 90.3973, 2600, "+8801711000996", true, true));
            safeLocationRepository.save(new SafeLocation("SAFE-05", "Ramna Elevated Park Ground", "Minto Road, Ramna", SafeLocation.LocationType.OPEN_FIELD, 23.7333, 90.3985, 3000, "+8801711000995", false, false));
            safeLocationRepository.save(new SafeLocation("SAFE-06", "Mirpur Community Center", "Mirpur-10 Roundabout", SafeLocation.LocationType.COMMUNITY_CENTER, 23.8067, 90.3686, 600, "+8801711000994", true, true));
            log.info(" -> Shelters & safe areas preloaded (6 locations around Dhaka).");
        }

        log.info("=============================================================");
        log.info(" ✅ DATABASE SEEDING COMPLETED SUCCESSFULLY");
        log.info("=============================================================");
    }
}

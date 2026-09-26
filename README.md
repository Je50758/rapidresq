# RapidResQ — Rapid Disaster Response System
> An academic, crowdsourced emergency triage, citizen verification, limited-resource optimization, and post-disaster rehabilitation Web platform built with Java, Spring Boot, Spring Data JPA, and H2 in-memory database.

---

## 📌 Project Overview
Unlike conventional centralized government disaster management tools, **RapidResQ** is designed around **citizen crowdsourcing and limited-resource optimization**, tailored specifically for the disaster landscape of **Bangladesh** (such as high-density urban fires, seasonal river flooding, highway collisions, and seismic tremors).

Key innovations preserved from the core OOP domain:
1. **Dynamic Priority Queue Triage**: Real-time evaluation of disaster urgency based on polymorphic domain threat formulas (chemical hazards, flood levels, passenger vehicles, collapsed buildings).
2. **Crowdsourced Citizen Verification**: Mitigates hoax reports by requiring 3+ citizen confirmations to automatically elevate an incident to `VERIFIED` status and reward initial reporter (+5 Trust).
3. **Reporter Trust Score System**:
   - Evaluates citizen reporters with a trust rating (0–100%).
   - Verified reports reward the reporter with **+5 Trust Score**.
   - Hoax or spam reports penalized by **-20 Trust Score**.
   - Low-trust users (<20%) receive an automatic `[UNVERIFIED - LOW TRUST REPORTER]` flag.
4. **Emergency Escalation Simulation (National 999 Integration)**:
   - When an incident severity reaches `CRITICAL` or extreme priority, the system generates an automated dispatch record to the **National Emergency Service (999)** and Armed Forces Disaster Wing.
5. **Post-Disaster Rehabilitation & Donation Tracker**:
   - Resolving an incident automatically generates a rehabilitation campaign for that specific location with targeted funding and medical/relief supplies.
6. **Observer Pattern Notifications**: Alerts community volunteers when emergencies strike and logs immutable system events.
7. **Spring Data JPA & H2 In-Memory Database**: All OOP inheritance, polymorphism, and composition are mapped natively to database tables without losing encapsulation or methods.

---

## 🔑 Preloaded Demo Credentials

| Role | Username | Password | Default Trust Score | Accessible Interface |
|---|---|---|---|---|
| **Administrator** | `admin` | `admin123` | 100% | Full Administrative Command, Overrides, User Control, Analytics |
| **Response Team** | `team1` | `pass123` | 90% | Operational Dispatch, Resource Fleet, 999 Escalations |
| **Volunteer** | `volunteer1` | `pass123` | 85% | Volunteer Alert Feed, Verification Staging, Relief Drives |
| **Citizen** | `citizen1` | `pass123` | 75% | Incident Reporting, Crowd Verification, Donations |

---

## 🏛️ OOP Pillars & Design Patterns Used in Spring Boot

| Principle / Pattern | Where & How It Is Implemented |
|---|---|
| **Encapsulation** | All entities (`Incident`, `ResponseTeam`, `Resource`, `User`, `ReliefNeed`, `DonationRecord`, `EscalationLog`) enforce `private` fields with getters, setters, and boundary validations. |
| **Inheritance & Polymorphism** | - Base abstract class `Incident` mapped via JPA Single-Table Inheritance (`@Inheritance(strategy = InheritanceType.SINGLE_TABLE)`), extended by `FireIncident`, `FloodIncident`, `AccidentIncident`, and `EarthquakeIncident`.<br>- Polymorphic `calculatePriority()` method evaluates urgency using subclass-specific threat formulas.<br>- Base abstract class `ResponseTeam` extended by `FireBrigade`, `MedicalTeam`, `RescueTeam`, and `Volunteer` with polymorphic `canHandle(Incident incident)`. |
| **Composition** | `ResponseTeam` owns a composed collection of `Resource` entities (`@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)`). |
| **Service Layer** | `DisasterManagementService` acts as the unified orchestrator managing the `PriorityQueue<Incident>` triage and JPA persistence. |
| **Observer Pattern** | `IncidentObserver` interface is implemented by `VolunteerNotifier`, `StatusLogger`, and `EmergencyEscalationNotifier` (999 escalations). |
| **Exception Handling** | `InvalidIncidentException` and `NoTeamAvailableException` handled via `@RestControllerAdvice` (`GlobalExceptionHandler`) returning structured JSON error payloads. |

---

## 📂 Web Backend Directory Structure (Maven)

```text
DisasterResponseManagementSystem/
├── src/
│   ├── main/
│   │   ├── java/com/disaster/
│   │   │   ├── config/
│   │   │   │   └── DataInitializer.java               # CommandLineRunner seeding demo users, teams, incidents
│   │   │   ├── exceptions/
│   │   │   │   ├── GlobalExceptionHandler.java        # @RestControllerAdvice for consistent JSON errors
│   │   │   │   ├── InvalidIncidentException.java
│   │   │   │   ├── NoTeamAvailableException.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   ├── models/
│   │   │   │   ├── AccidentIncident.java              # @Entity subclass
│   │   │   │   ├── DisasterType.java                  # Enum (FIRE, FLOOD, ACCIDENT, EARTHQUAKE)
│   │   │   │   ├── DonationRecord.java                # @Entity
│   │   │   │   ├── EarthquakeIncident.java            # @Entity subclass
│   │   │   │   ├── EscalationLog.java                 # @Entity
│   │   │   │   ├── FireBrigade.java                   # @Entity subclass
│   │   │   │   ├── FireIncident.java                  # @Entity subclass
│   │   │   │   ├── FloodIncident.java                 # @Entity subclass
│   │   │   │   ├── Incident.java                      # @Entity abstract base class
│   │   │   │   ├── IncidentStatus.java                # Enum (REPORTED, VERIFIED, IN_PROGRESS, RESOLVED)
│   │   │   │   ├── MedicalTeam.java                   # @Entity subclass
│   │   │   │   ├── ReliefNeed.java                    # @Entity
│   │   │   │   ├── RescueTeam.java                    # @Entity subclass
│   │   │   │   ├── Resource.java                      # @Entity (Composition)
│   │   │   │   ├── ResponseTeam.java                  # @Entity abstract base class
│   │   │   │   ├── Role.java                          # Enum (ADMIN, RESPONSE_TEAM, VOLUNTEER, CITIZEN)
│   │   │   │   ├── Severity.java                      # Enum (LOW, MODERATE, CRITICAL)
│   │   │   │   ├── TeamStatus.java                    # Enum (AVAILABLE, ASSIGNED, BUSY, STANDBY)
│   │   │   │   ├── User.java                          # @Entity (SHA-256 hashed credentials)
│   │   │   │   └── Volunteer.java                     # @Entity subclass
│   │   │   ├── observers/
│   │   │   │   ├── EmergencyEscalationNotifier.java   # Automated 999 escalation observer
│   │   │   │   ├── IncidentObserver.java              # Observer interface
│   │   │   │   ├── StatusLogger.java                  # Audit log observer
│   │   │   │   └── VolunteerNotifier.java             # Volunteer notification observer
│   │   │   ├── repository/
│   │   │   │   ├── DonationRecordRepository.java
│   │   │   │   ├── EscalationLogRepository.java
│   │   │   │   ├── IncidentRepository.java
│   │   │   │   ├── ReliefNeedRepository.java
│   │   │   │   ├── ResourceRepository.java
│   │   │   │   ├── ResponseTeamRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   ├── service/
│   │   │   │   ├── DisasterManagementService.java     # PriorityQueue triage & dispatch service
│   │   │   │   └── UserAuthenticationService.java     # Authentication & role service
│   │   │   └── DisasterManagementApplication.java     # Spring Boot Main Entry Point
│   │   └── resources/
│   │       ├── application.properties                 # H2 in-memory DB & server configuration
│   │       └── static/                                # Web Frontend static resources (Phase 5)
│   │           ├── css/
│   │           └── js/
│   └── test/
│       └── java/com/disaster/
│           └── Phase1BackendTest.java                 # Automated Spring Boot test suite
├── legacy_swing/                                      # Backup of legacy Swing GUI implementation
├── pom.xml                                            # Maven configuration with Spring Boot 3.3.4
├── mvn.cmd                                            # Embedded Maven command runner
└── mvnw.cmd                                           # Maven wrapper runner
```

---

## 🚀 How to Run Phase 1

### 1. Run Automated Backend Tests:
```powershell
.\mvn.cmd test
```

### 2. Run the Spring Boot Server:
```powershell
.\mvn.cmd spring-boot:run
```
Once started, the application is accessible at:
- **Application URL**: `http://localhost:8080/`
- **H2 In-Memory DB Console**: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:disasterdb`
  - Username: `sa`
  - Password: *(leave blank)*

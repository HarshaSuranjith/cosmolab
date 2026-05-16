# CosmoLab — Domain Aggregates

> Detailed reference for each aggregate in `com.arcticsurge.cosmolab.domain`.
> The domain layer has zero framework dependencies beyond JPA annotations and Lombok.
> It is the innermost ring of the DDD-lite architecture — no Spring beans, no service calls, no DTOs.

---

## Architecture Pattern

```
interfaces/rest  (controllers, DTOs)
      │
application      (use-case services, @Transactional)
      │
domain           ◄── this document
      │
infrastructure   (JPA repositories, messaging adapters)
```

Each subdomain (`patient`, `ehr`, `composition`, `observation`, `evaluation`) contains:
- One entity class (the aggregate root)
- Value-object enums for constrained fields
- A repository **interface** (port) — implemented in `infrastructure/persistence`

Repository interfaces accept and return domain objects only. No JPA `Specification`, no JPQL, no SQL in the domain layer.

---

## Base Types

### `BaseEntity`

Abstract superclass for all aggregates that use a surrogate primary key.

```
id : UUID   @Id @GeneratedValue @UuidGenerator
            columnDefinition = "UNIQUEIDENTIFIER"
```

Uses Hibernate's `@UuidGenerator` to generate `UUID` values on the Java side before insert, matching SQL Server's `NEWSEQUENTIALID()` column default. Sequential UUIDs cluster new rows at the end of the clustered index — important for write-heavy tables like `vital_signs`.

`EhrRecord` does **not** extend `BaseEntity` — its PK column is named `ehr_id` rather than `id`, reflecting the openEHR convention that an EHR is identified by its `ehrId`, not a generic surrogate.

### `EntityNotFoundException`

Abstract base for domain-specific not-found exceptions. Subclasses (`PatientNotFoundException`, `EhrNotFoundException`, etc.) are thrown by application services and caught by `GlobalExceptionHandler`, which maps them to HTTP 404 `ProblemDetail`.

---

## Aggregate: Patient

**Package**: `com.arcticsurge.cosmolab.domain.patient`
**Table**: `patients`
**Aggregate root**: yes — the entry point for all patient demographic data.

### Fields

| Field | Type | Column | Constraint |
|---|---|---|---|
| `id` | `UUID` | `id` UNIQUEIDENTIFIER PK | `NEWSEQUENTIALID()` |
| `firstName` | `String` | `first_name` NVARCHAR(100) | NOT NULL |
| `lastName` | `String` | `last_name` NVARCHAR(100) | NOT NULL |
| `personalNumber` | `String` | `personal_number` NVARCHAR(13) | NOT NULL, UNIQUE |
| `dateOfBirth` | `LocalDate` | `date_of_birth` DATE | NOT NULL |
| `gender` | `Gender` | `gender` NVARCHAR(20) | NOT NULL, CHECK |
| `ward` | `String` | `ward` NVARCHAR(100) | NOT NULL |
| `status` | `PatientStatus` | `status` NVARCHAR(20) | NOT NULL, CHECK |
| `createdAt` | `Instant` | `created_at` DATETIMEOFFSET(6) | NOT NULL, not updatable |
| `updatedAt` | `Instant` | `updated_at` DATETIMEOFFSET(6) | NOT NULL, auto-updated |

### Lifecycle

`@EntityListeners(AuditingEntityListener.class)` with `@CreatedDate` and `@LastModifiedDate`. Spring Data JPA populates `createdAt` on insert and `updatedAt` on every update automatically. Requires `@EnableJpaAuditing` on the application configuration.

### Enums

**`PatientStatus`**
```
ACTIVE       — currently admitted and receiving care
DISCHARGED   — care episode complete; soft-delete equivalent
TRANSFERRED  — moved to another facility or ward
```

DELETE operations on `Patient` are soft-deletes: the service sets `status = DISCHARGED` rather than removing the row. This preserves referential integrity with `ehr_records` and the full audit trail.

**`Gender`**
```
MALE | FEMALE | OTHER
```

### Repository interface

```java
Patient              save(Patient patient)
Optional<Patient>    findById(UUID id)
Page<Patient>        findAll(Specification<Patient> spec, Pageable pageable)
void                 deleteById(UUID id)
boolean              existsById(UUID id)
```

`findAll` accepts a JPA `Specification` to support dynamic filter combinations (ward + status + search term) without a proliferation of query methods. The implementation in `infrastructure/persistence` uses Spring Data JPA's `JpaSpecificationExecutor`.

### Key invariants
- `personalNumber` is the Swedish national ID (personnummer), format `YYYYMMDD-XXXX`. UNIQUE constraint prevents duplicate registrations.
- NVARCHAR is mandatory — Swedish names contain å, ä, ö which corrupt silently in VARCHAR on SQL Server's default collation.
- `ward` is a free-text string, not a foreign key. Ward names are a configuration concern, not a relational entity in this domain.

---

## Aggregate: EhrRecord

**Package**: `com.arcticsurge.cosmolab.domain.ehr`
**Table**: `ehr_records`
**Aggregate root**: yes — the EHR is the top of the openEHR containment hierarchy beneath the patient.

### Fields

| Field | Type | Column | Constraint |
|---|---|---|---|
| `ehrId` | `UUID` | `ehr_id` UNIQUEIDENTIFIER PK | `NEWSEQUENTIALID()` |
| `subjectId` | `UUID` | `subject_id` UNIQUEIDENTIFIER | NOT NULL, FK → patients, UNIQUE |
| `systemId` | `String` | `system_id` NVARCHAR(100) | NOT NULL, default `cosmolab-v1` |
| `createdAt` | `Instant` | `created_at` DATETIMEOFFSET(6) | NOT NULL, not updatable |
| `status` | `EhrStatus` | `status` NVARCHAR(20) | NOT NULL, CHECK |

### Lifecycle

Does not extend `BaseEntity` — uses its own `@Id` declaration on `ehrId` to match the openEHR naming convention. `@PrePersist` sets default values:
```java
void onCreate() {
    if (systemId == null) systemId = "cosmolab-v1";
    if (status == null) status = EhrStatus.ACTIVE;
}
```

### Enums

**`EhrStatus`**
```
ACTIVE    — EHR is in use; patient is in the system
INACTIVE  — EHR archived (patient deceased or long-term discharged)
```

### Repository interface

```java
EhrRecord            save(EhrRecord ehr)
Optional<EhrRecord>  findById(UUID ehrId)
Optional<EhrRecord>  findBySubjectId(UUID patientId)
boolean              existsBySubjectId(UUID patientId)
```

### Key invariants
- One EHR per patient for life — enforced by `UNIQUE (subject_id)`. The EHR is never deleted; only its `status` changes.
- `subjectId` is not a JPA `@ManyToOne` — the domain layer holds UUIDs, not entity references. This prevents accidental cross-aggregate lazy loading. The application layer loads `Patient` and `EhrRecord` separately when both are needed.
- `systemId` identifies the source system that created the EHR. In a multi-system federation this would be a URL or URN. For CosmoLab it is a fixed string.

---

## Aggregate: Composition

**Package**: `com.arcticsurge.cosmolab.domain.composition`
**Table**: `compositions`
**Aggregate root**: yes — a Composition is the clinical document that groups all entries from a single encounter.

### Fields

| Field | Type | Column | Constraint |
|---|---|---|---|
| `id` | `UUID` | `id` UNIQUEIDENTIFIER PK | `NEWSEQUENTIALID()` |
| `ehrId` | `UUID` | `ehr_id` UNIQUEIDENTIFIER | NOT NULL, FK → ehr_records |
| `type` | `CompositionType` | `composition_type` NVARCHAR(50) | NOT NULL, CHECK |
| `authorId` | `UUID` | `author_id` UNIQUEIDENTIFIER | NOT NULL |
| `startTime` | `Instant` | `start_time` DATETIMEOFFSET(6) | NOT NULL |
| `commitTime` | `Instant` | `commit_time` DATETIMEOFFSET(6) | NOT NULL |
| `facilityName` | `String` | `facility_name` NVARCHAR(200) | nullable |
| `status` | `CompositionStatus` | `status` NVARCHAR(20) | NOT NULL, CHECK |

### Lifecycle

`@PrePersist` sets defaults on insert:
```java
void onCreate() {
    commitTime = Instant.now();          // system timestamp, always set at persist time
    if (startTime == null) startTime = commitTime;
    if (status == null) status = CompositionStatus.COMPLETE;
}
```

`startTime` is the **clinical event time** — when the encounter occurred. `commitTime` is the **system write time** — when the record was persisted. These can differ significantly: a ward round note written at 09:00 about a patient assessment at 06:30 has `startTime = 06:30`, `commitTime = 09:00`. Separating these two timestamps is an openEHR requirement.

### Enums

**`CompositionType`**
```
ENCOUNTER_NOTE      — general clinical encounter; most common type
ADMISSION           — initial admission document
PROGRESS_NOTE       — follow-up during an inpatient stay
DISCHARGE_SUMMARY   — end-of-episode summary
```

**`CompositionStatus`**
```
COMPLETE    — document is finalised
INCOMPLETE  — draft, not yet finalised
AMENDED     — a correction was made after initial completion
```

### Repository interface

```java
Composition            save(Composition composition)
Optional<Composition>  findById(UUID id)
Page<Composition>      findByEhrId(UUID ehrId, Pageable pageable)
Page<Composition>      findByEhrIdAndType(UUID ehrId, CompositionType type, Pageable pageable)
boolean                existsByIdAndEhrId(UUID id, UUID ehrId)
```

`existsByIdAndEhrId` is an ownership check — used by the application layer to verify that a composition belongs to a given EHR before allowing a `PUT`. Without this, any client knowing a composition UUID could modify records across EHRs.

### Key invariants
- A composition belongs to exactly one EHR and cannot be reassigned.
- `authorId` is a UUID reference to the clinician. In CosmoLab there is no users table — `authorId` is set by the application service from the authenticated user context (or a hardcoded demo UUID).
- `facilityName` is nullable — encounters may not always have a named facility.

---

## Aggregate: VitalSigns

**Package**: `com.arcticsurge.cosmolab.domain.observation`
**Table**: `vital_signs`
**openEHR archetype**: `OBSERVATION` — directly measured physiological data.
**Aggregate root**: yes within the observation subdomain, but always accessed through a Composition.

### Fields

| Field | Type | Column | Notes |
|---|---|---|---|
| `id` | `UUID` | `id` UNIQUEIDENTIFIER PK | |
| `compositionId` | `UUID` | `composition_id` UNIQUEIDENTIFIER | FK → compositions |
| `recordedAt` | `Instant` | `recorded_at` DATETIMEOFFSET(6) | defaults to `now()` on persist |
| `recordedBy` | `UUID` | `recorded_by` UNIQUEIDENTIFIER | clinician reference |
| `systolicBp` | `Integer` | `systolic_bp` INT | nullable — not always measured |
| `diastolicBp` | `Integer` | `diastolic_bp` INT | nullable |
| `heartRate` | `Integer` | `heart_rate` INT | nullable |
| `respiratoryRate` | `Integer` | `respiratory_rate` INT | nullable |
| `temperature` | `BigDecimal` | `temperature` DECIMAL(4,1) | nullable; e.g. `37.5` |
| `oxygenSaturation` | `BigDecimal` | `oxygen_saturation` DECIMAL(5,2) | nullable; e.g. `98.50` |
| `weight` | `BigDecimal` | `weight` DECIMAL(5,2) | nullable; kg |

### Lifecycle

`@PrePersist` defaults `recordedAt` to `Instant.now()` if not supplied by the caller.

All clinical measurement fields are nullable — a vital signs record captures whatever was measured at that time. It is valid to record only temperature and SpO₂ without blood pressure.

`BigDecimal` is used for `temperature`, `oxygenSaturation`, and `weight` to preserve precision and scale without floating-point rounding errors. `DECIMAL(4,1)` stores e.g. `37.5`; `DECIMAL(5,2)` stores e.g. `98.50` and `75.00`.

### Repository interface

```java
VitalSigns              save(VitalSigns vitalSigns)
Optional<VitalSigns>    findById(UUID id)
List<VitalSigns>        findByCompositionId(UUID compositionId)
List<VitalSigns>        findByEhrIdBetween(UUID ehrId, Instant from, Instant to)
Optional<VitalSigns>    findLatestByEhrId(UUID ehrId)
```

`findLatestByEhrId` is the hot path for the ward overview — it needs the single most recent vitals record per patient without loading all historical readings. The implementation uses a `TOP 1 ... ORDER BY recorded_at DESC` query joined through compositions.

`findByEhrIdBetween` supports the Vitals tab time-range filter — fetches all readings for an EHR within a `[from, to]` window for charting.

### Normal range reference (for UI flagging)

The frontend `VitalSignsChartComponent` colours cells outside these ranges amber or red. These are standard adult clinical reference ranges:

| Measurement | Normal range | Unit |
|---|---|---|
| Systolic BP | 90 – 140 | mmHg |
| Diastolic BP | 60 – 90 | mmHg |
| Heart rate | 60 – 100 | bpm |
| Respiratory rate | 12 – 20 | breaths/min |
| Temperature | 36.1 – 37.2 | °C |
| SpO₂ | ≥ 95 | % |

---

## Aggregate: ProblemDiagnosis

**Package**: `com.arcticsurge.cosmolab.domain.evaluation`
**Table**: `problem_list_entries`
**openEHR archetype**: `EVALUATION` — a clinical assessment or diagnosis, as opposed to a raw measurement.
**Aggregate root**: yes — represents a single diagnosis on the patient's problem list.

### Fields

| Field | Type | Column | Notes |
|---|---|---|---|
| `id` | `UUID` | `id` UNIQUEIDENTIFIER PK | |
| `compositionId` | `UUID` | `composition_id` UNIQUEIDENTIFIER | FK → compositions (authoring context) |
| `ehrId` | `UUID` | `ehr_id` UNIQUEIDENTIFIER | FK → ehr_records (query shortcut) |
| `icd10Code` | `String` | `icd10_code` NVARCHAR(20) | e.g. `I10`, `J18.9` |
| `displayName` | `String` | `display_name` NVARCHAR(200) | human-readable diagnosis name |
| `severity` | `Severity` | `severity` NVARCHAR(20) | CHECK |
| `status` | `ProblemStatus` | `status` NVARCHAR(20) | CHECK, default ACTIVE |
| `onsetDate` | `LocalDate` | `onset_date` DATE | nullable — patient-reported or estimated |
| `resolvedDate` | `LocalDate` | `resolved_date` DATE | nullable — set when status → RESOLVED |
| `recordedAt` | `Instant` | `recorded_at` DATETIMEOFFSET(6) | system time of entry |
| `recordedBy` | `UUID` | `recorded_by` UNIQUEIDENTIFIER | clinician reference |

### Lifecycle

`@PrePersist` sets defaults:
```java
void onCreate() {
    if (recordedAt == null) recordedAt = Instant.now();
    if (status == null) status = ProblemStatus.ACTIVE;
}
```

`resolvedDate` should be set by the application service when `status` transitions to `RESOLVED`. There is no enforcement of this in the domain class — it is an application-layer convention.

### Dual foreign key design

`ProblemDiagnosis` carries both `compositionId` and `ehrId`. This is intentional:

- **`compositionId`** records the clinical context — the encounter during which the problem was identified, who authored it, when it happened.
- **`ehrId`** is a query shortcut — to find all problems for a patient, the query is `WHERE ehr_id = ?` instead of `WHERE composition_id IN (SELECT id FROM compositions WHERE ehr_id = ?)`. The latter is a three-table join that becomes expensive as compositions accumulate. The direct `ehr_id` column with an index on `(ehr_id, status)` makes the active-problem lookup a single table scan.

### Enums

**`Severity`**
```
MILD      — manageable, low immediate risk
MODERATE  — requires monitoring and active management
SEVERE    — high immediate risk, affects care decisions
```

**`ProblemStatus`**
```
ACTIVE    — current, ongoing problem
INACTIVE  — temporarily not relevant; may recur
RESOLVED  — problem has been resolved; resolvedDate should be set
REFUTED   — originally recorded in error; equivalent to a retraction
```

### Repository interface

```java
ProblemDiagnosis            save(ProblemDiagnosis entry)
Optional<ProblemDiagnosis>  findById(UUID id)
List<ProblemDiagnosis>      findByEhrId(UUID ehrId)
List<ProblemDiagnosis>      findByEhrIdAndStatus(UUID ehrId, ProblemStatus status)
long                        countByEhrIdAndStatus(UUID ehrId, ProblemStatus status)
```

`countByEhrIdAndStatus` is used by the ward overview aggregation — specifically `countByEhrIdAndStatus(ehrId, ACTIVE)` per patient. In practice this count is computed in SQL by the `vw_ward_patient_summary` view rather than issuing N individual count queries.

### Key invariants
- `icd10Code` is validated at the REST layer (pattern check) but is free-text in the domain — the system does not maintain an ICD-10 code table.
- `onsetDate` may predate system entry — a patient may report a problem that began years ago.
- Transitioning to `RESOLVED` without setting `resolvedDate` is permitted by the schema but should be prevented by the application layer.

---

## Cross-Aggregate Relationships

```
Patient ──(1:1)── EhrRecord
                     │
                   (1:N)
                     │
               Composition
                  /       \
              (1:N)      (1:N)
                /           \
         VitalSigns    ProblemDiagnosis ──(N:1, shortcut)── EhrRecord
```

Foreign key relationships use raw `UUID` fields rather than JPA `@ManyToOne` references. This is a deliberate DDD boundary decision:

- **No cross-aggregate lazy loading** — loading a `Composition` never triggers an implicit load of its parent `EhrRecord`. Each load is explicit in the application layer.
- **No cascades** — deleting a `Composition` does not cascade to `VitalSigns`. Cascade deletes in a clinical system are a data integrity risk.
- **Simpler transactions** — each aggregate save is its own `@Transactional` boundary in the application service.

The cost: the application layer must do two round-trips when it needs both a composition and its EHR. The benefit: the domain is easy to reason about, and the aggregate boundaries are explicit.

---
description: openEHR clinical domain — EHR containment hierarchy, selected features, entity definitions, interview talking points
globs: cosmolab-backend/src/main/java/com/cosmolab/domain/**,cosmolab-backend/src/main/java/com/cosmolab/application/**
alwaysApply: false
---

# Clinical Domain (openEHR-aligned)

## openEHR Containment Hierarchy

```
EHR                         ← one per patient; permanent; never deleted
├── EHR_STATUS              ← one per EHR; queryability, modifiability, subject ref
├── EHR_ACCESS              ← access control settings for this EHR
├── DIRECTORY (FOLDER)      ← hierarchical folder tree; organises compositions by episode
└── VERSIONED_COMPOSITION   ← append-only version container (one per logical composition)
    └── VERSION             ← a single immutable committed snapshot
        ├── AUDIT_DETAILS   ← committer, time_committed, change_type
        ├── CONTRIBUTION    ← groups all VERSIONs committed in one transaction
        └── COMPOSITION     ← one clinical document / encounter
            └── ENTRY (4 subtypes)
                ├── OBSERVATION   raw measurements (vital signs, test results)  ✓ VitalSigns
                ├── EVALUATION    clinical interpretation (diagnosis, risk)      ✓ ProblemDiagnosis
                ├── INSTRUCTION   order or plan (medication, referral)           ✗ deferred
                ├── ACTION        what was done (linked to INSTRUCTION)          ✗ deferred
                └── ADMIN_ENTRY   administrative facts (admission, transfer)     ✗ deferred
```

CosmoLab implements: EHR, COMPOSITION, OBSERVATION (VitalSigns), EVALUATION (ProblemDiagnosis).
INSTRUCTION, ACTION, and ADMIN_ENTRY are deferred to phase 2.
EHR_STATUS, versioning, FOLDER, and the demographic service are deliberately out of scope — see §Missing Aggregate Roots.

COSMIC uses its own template format that maps bi-directionally to openEHR archetypes.
CosmoLab mirrors the containment structure without implementing the full archetype engine.

## Feature 1 — EhrRecord

One EHR per patient. Permanent. The clinical container — separate from demographics.
Demographics (name, personnummer) live in `Patient`. The EHR references Patient by `subjectId`.
This separation allows demographics to change without affecting the integrity of the clinical record.

```java
// domain/ehr/EhrRecord.java
UUID ehrId;           // openEHR EHR identifier
UUID subjectId;       // FK → Patient (demographics separate concern)
String systemId;      // "cosmolab-v1"
Instant createdAt;
String status;        // ACTIVE | INACTIVE
```

**Interview**: "In openEHR, the EHR and patient demographics are intentionally decoupled.
The EHR references the patient by subject ID. This allows name changes without affecting
the clinical record's integrity."

## Feature 2 — Composition

Unit of committal to the EHR. Every clinical recording goes into a Composition.
Has a clinical time (`startTime`) separate from the system commit time (`commitTime`).
A nurse records at 06:00 but commits at 08:30 — both facts must be preserved.

```java
// domain/composition/Composition.java
UUID id;
UUID ehrId;                  // FK → EhrRecord
CompositionType type;        // ENCOUNTER_NOTE | ADMISSION | PROGRESS_NOTE | DISCHARGE_SUMMARY
UUID authorId;
Instant startTime;           // clinical time — when the encounter happened
Instant commitTime;          // system time — when it was saved
String facilityName;
CompositionStatus status;    // COMPLETE | INCOMPLETE | AMENDED
```

**Interview**: "startTime vs commitTime is fundamental in openEHR. They are two different
facts about the same record and must never be conflated."

## Feature 3 — VitalSigns (OBSERVATION)

Raw measurements — not interpreted. The same reading means different things for different patients.
Interpretation belongs in an EVALUATION, not here.
All measurement fields are nullable — not every vital is taken every time.

```java
// domain/observation/VitalSigns.java
UUID id;
UUID compositionId;
Instant recordedAt;
UUID recordedBy;
Integer systolicBP;          // mmHg     normal: 90-140
Integer diastolicBP;         // mmHg     normal: 60-90
Integer heartRate;           // bpm      normal: 60-100
Integer respiratoryRate;     // /min     normal: 12-20
BigDecimal temperature;      // °C  (4,1) normal: 36.1-37.2
BigDecimal oxygenSaturation; // %   (5,2) flag below 95
BigDecimal weight;           // kg  (5,2) no range
```

WardOverview uses `/vitals/latest` — implemented as a window function query, not a load-all-filter.

**Interview**: "OBSERVATION stores what was measured. It never stores what it means.
Storing temperature as DECIMAL(4,1) with an implicit °C unit is the pragmatic equivalent
of openEHR's DV_QUANTITY type."

## Feature 4 — ProblemDiagnosis (EVALUATION)

Clinical interpretation — a diagnosis applied after assessing observations.
Has a direct FK to both Composition (where it was recorded) and EhrRecord
(for efficient problem list queries without joining through Composition).

```java
// domain/evaluation/ProblemDiagnosis.java
UUID id;
UUID compositionId;          // FK → Composition (where first recorded)
UUID ehrId;                  // FK → EhrRecord (direct, for efficient queries)
String icd10Code;            // e.g. "I10", "E11", "J45.9" — plain string
String displayName;          // e.g. "Essential Hypertension"
Severity severity;           // MILD | MODERATE | SEVERE
ProblemStatus status;        // ACTIVE | INACTIVE | RESOLVED | REFUTED
LocalDate onsetDate;
LocalDate resolvedDate;      // nullable
Instant recordedAt;
UUID recordedBy;
```

Common seed ICD-10 codes: I10 (hypertension), E11 (T2DM), J45.9 (asthma),
N18.3 (CKD stage 3), I50.9 (heart failure), F32.1 (depression), M54.5 (low back pain).

**Interview**: "OBSERVATION is what was measured; EVALUATION is what a clinician decided
it means. They can be separated in time — observation by a nurse, diagnosis by a physician
two hours later. The data model reflects this clinical reality."

## Feature 5 — WardOverview

Not a new entity — a read-only aggregation query across EHRs. Maps to COSMIC's Overviews.
Answers: "Who is in my ward, what are their latest vitals, and what are their active problems?"

```java
// application/ward/WardOverviewService.java
// Joins: patients + ehr_records + latest vital_signs (window function) + problem count
// Returns: WardOverviewResponse with nested PatientSummary + LatestVitals + flags[]
```

`flags[]` is derived server-side from the normal ranges table. Example:
```json
"flags": ["systolicBP:HIGH", "temperature:HIGH"]
```

This is the **primary JMeter/Gatling/k6 load test target** — 4-table join with window function.
It will expose N+1 risks and missing indexes faster than any other endpoint.

## Missing Aggregate Roots

### EHR_STATUS

Every EHR owns exactly one `EHR_STATUS`. It is a special persistent composition that openEHR creates automatically on EHR creation. CosmoLab's `EhrRecord` has no equivalent.

```
EHR_STATUS fields:
  is_queryable    Boolean   — whether this EHR appears in population queries
  is_modifiable   Boolean   — whether new content can be added
  subject         PARTY_SELF — reference back to the patient demographic record
  other_details   ITEM_STRUCTURE — any additional patient-level clinical context
```

**Interview**: "EHR_STATUS is the EHR's own metadata record. The `is_queryable` flag is how COSMIC excludes deceased or opted-out patients from population queries without deleting their records. We don't implement it in CosmoLab but the concept is fundamental — every openEHR server creates it automatically."

---

### Versioning Layer (VERSIONED_COMPOSITION → VERSION → CONTRIBUTION → AUDIT_DETAILS)

This is the **biggest structural gap**. openEHR is an **append-only, immutable** system. You never update a composition — you commit a new `VERSION` of it. CosmoLab's `PUT /compositions/{id}` mutates in place, which violates this guarantee.

```
VERSIONED_COMPOSITION
  uid                OBJECT_VERSION_ID   — stable identifier across all versions
  owner_id           EHR_ID
  time_created       DV_DATE_TIME

VERSION<COMPOSITION>
  uid                OBJECT_VERSION_ID   — version_tree_id::creating_system_id::version_id
  data               COMPOSITION         — the actual content
  lifecycle_state    DV_CODED_TEXT       — complete | incomplete | deleted
  commit_audit       AUDIT_DETAILS
  contribution       CONTRIBUTION        — the commit batch this version belongs to
  preceding_version_uid                  — links version chain

AUDIT_DETAILS
  system_id          String
  committer          PARTY_IDENTIFIED    — who committed (not who authored)
  time_committed     DV_DATE_TIME
  change_type        DV_CODED_TEXT       — creation | modification | deleted | attestation

CONTRIBUTION
  uid                UUID
  versions           Set<OBJECT_REF>     — all versions committed together
  audit              AUDIT_DETAILS
```

In a real COSMIC deployment, `PUT /compositions/{id}` would instead call `POST /compositions/{uid}/versions` — creating a new version with `change_type = modification` and linking it to the preceding version's UID. The old version remains readable.

**Interview**: "openEHR's immutability is non-negotiable in clinical systems. A doctor amending a note does not overwrite the original — both versions are stored. The audit trail is structural, not an afterthought. CosmoLab uses a RabbitMQ audit log as a pragmatic substitute, but in production COSMIC the VERSION chain is the audit trail."

---

### FOLDER / DIRECTORY

A hierarchical folder structure inside the EHR for organising compositions. Not a clinical concept — purely organisational.

```
FOLDER
  name        DV_TEXT              — e.g. "Cardiology", "Episode 2024-03"
  items       List<OBJECT_REF>     — references to VERSIONED_COMPOSITIONs
  folders     List<FOLDER>         — recursive sub-folders
```

COSMIC uses FOLDER to group compositions by episode of care, referral, or clinical service. A patient discharged from cardiology and re-admitted later has two folders, each containing the compositions for that episode.

**Interview**: "FOLDER is how openEHR avoids a flat list of hundreds of compositions. In COSMIC you navigate to a patient → select episode → see the compositions for that episode. Without FOLDER the client has to filter by date or type, which loses the episode grouping."

---

### INSTRUCTION and ACTION

The order/workflow half of clinical informatics — absent from CosmoLab.

```
INSTRUCTION (what should happen)
  narrative         DV_TEXT           — human-readable description of the order
  activities        List<ACTIVITY>
    action_archetype_id               — links to the ACTION archetype that will fulfil this
    timing            DV_PARSABLE     — iCalendar RRULE syntax; e.g. "FREQ=DAILY;COUNT=7"
    description       ITEM_STRUCTURE

ACTION (what was done — one per activity execution)
  time              DV_DATE_TIME      — when the action was performed
  ism_transition    ISM_TRANSITION    — state machine transition
    current_state   DV_CODED_TEXT     — planned | scheduled | active | completed | aborted | expired
    transition      DV_CODED_TEXT     — initiate | schedule | do | finish | abort | etc.
  instruction_details
    instruction_id  LOCATABLE_REF     — back-reference to the originating INSTRUCTION
    activity_id     String
```

The `ISM_TRANSITION` (Instruction State Machine) is the workflow engine. A medication order (INSTRUCTION) moves through: `planned → scheduled → active → completed`. Every state change creates a new ACTION. This is how COSMIC tracks whether a prescribed drug was actually administered.

**Interview**: "OBSERVATION and EVALUATION are the recording side — what was seen and what it means. INSTRUCTION and ACTION are the workflow side — what was ordered and what was done. COSMIC's medication management, referrals, and care plans all run through this model. We deferred it because it requires a full workflow engine, not just a REST endpoint."

---

### ADMIN_ENTRY

Administrative facts that belong in the EHR but are not clinical observations, assessments, or orders. Fourth entry subtype alongside OBSERVATION, EVALUATION, and INSTRUCTION/ACTION.

```
ADMIN_ENTRY
  data    ITEM_STRUCTURE    — free structure; content determined by archetype

Common archetypes:
  openEHR-EHR-ADMIN_ENTRY.admission.v1    — ward, bed, admission reason
  openEHR-EHR-ADMIN_ENTRY.discharge.v1   — discharge destination, reason
  openEHR-EHR-ADMIN_ENTRY.transfer.v1    — from ward, to ward, reason
```

CosmoLab currently models admission/discharge/transfer as `CompositionType` enum values (`ADMISSION`, `DISCHARGE_SUMMARY`). In openEHR, the administrative facts of the admission (which ward, which bed, under which consultant) belong in an `ADMIN_ENTRY`, not in the composition header.

---

### Demographic Service (ORGANISATION, ROLE, PARTY_RELATIONSHIP)

In openEHR, patient demographics live in a completely separate **Demographic Service** — not in the EHR. The EHR references the patient only by a `PARTY_REF` (an external ID pointer). CosmoLab collapses demographics into the `Patient` entity for pragmatic simplicity.

```
Missing from the demographic model:
  ORGANISATION       — hospital, department, ward as first-class entities (not just strings)
  ROLE               — clinician role (GP, consultant, nurse) — a person holds multiple roles
  PARTY_RELATIONSHIP — patient–GP, patient–next-of-kin, clinician–organisation
  ADDRESS / CONTACT  — contact details (out of scope for clinical backend)
```

The `facilityName` string on `Composition` and the `ward` string on `Patient` are the pragmatic substitute for a proper ORGANISATION reference.

**Interview**: "openEHR separates the EHR from demographics by design — a name change or address update never touches the clinical record. We've kept that boundary: Patient holds demographics, EhrRecord holds clinical context, and they're joined by subjectId. The full demographic service with ORGANISATION and ROLE is out of scope but the structural separation is correct."

---

## Deliberately Excluded from CosmoLab

| Excluded | Reason |
|---|---|
| Archetype engine (ADL parsing, runtime constraint validation) | Requires dedicated archetype server; COSMIC has its own |
| GDL / Clinical Decision Support | Separate Cambio product |
| SNOMED CT / ICD-10 terminology service | External dependency; codes used as plain strings |
| HL7 FHIR API | Phase 2 |
| EHR versioning (VERSIONED_COMPOSITION / VERSION / CONTRIBUTION) | RabbitMQ audit trail is the pragmatic substitute; full versioning requires an openEHR server |
| EHR_STATUS | Auto-managed by openEHR server; no equivalent needed in a simplified model |
| FOLDER / DIRECTORY | Episode grouping deferred; CompositionType partially compensates |
| INSTRUCTION + ACTION | Requires ISM workflow engine; deferred to phase 2 |
| ADMIN_ENTRY | Admission/discharge represented as CompositionType values |
| Full demographic service (ORGANISATION, ROLE, PARTY_RELATIONSHIP) | facilityName string + ward string are the pragmatic substitute |

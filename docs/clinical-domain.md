---
description: openEHR clinical domain — EHR containment hierarchy, selected features, entity definitions, interview talking points
globs: cosmolab-backend/src/main/java/com/cosmolab/domain/**,cosmolab-backend/src/main/java/com/cosmolab/application/**
alwaysApply: false
---

# Clinical Domain (openEHR-aligned)

## openEHR Containment Hierarchy

```
EHR                     ← one per patient; permanent; never deleted
└── COMPOSITION         ← one clinical document / encounter
    └── ENTRY (4 subtypes)
        ├── OBSERVATION  raw measurements (vital signs, test results)
        ├── EVALUATION   clinical interpretation (diagnosis, risk)
        ├── INSTRUCTION  order or plan (medication, referral)
        └── ACTION       what was done (administered, completed)
```

CosmoLab implements: EHR, COMPOSITION, OBSERVATION (VitalSigns), EVALUATION (ProblemListEntry).
INSTRUCTION and ACTION are deferred to phase 2.

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

## Feature 4 — ProblemListEntry (EVALUATION)

Clinical interpretation — a diagnosis applied after assessing observations.
Has a direct FK to both Composition (where it was recorded) and EhrRecord
(for efficient problem list queries without joining through Composition).

```java
// domain/evaluation/ProblemListEntry.java
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

## Deliberately Excluded from CosmoLab

| Excluded | Reason |
|---|---|
| Archetype engine (ADL parsing, runtime constraint validation) | Requires dedicated archetype server |
| GDL / Clinical Decision Support | Separate Cambio product |
| SNOMED CT / ICD-10 terminology service | External dependency; codes used as plain strings |
| HL7 FHIR API | Phase 2 |
| EHR versioning (change_control) | RabbitMQ audit trail is sufficient |

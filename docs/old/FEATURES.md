# CosmoLab — Clinical Features (openEHR-aligned)

> **Scope**: Defines the clinical domain model for CosmoLab, grounded in openEHR
> Reference Model concepts as implemented by Cambio COSMIC.
> This is not a full openEHR implementation — it is a deliberate subset that
> reflects how COSMIC stores and structures clinical data, enough to make
> CosmoLab a credible domain simulation for interview preparation.
>
> Parent document: [CLAUDE.md](../CLAUDE.md)

---

## 1. openEHR Primer (what matters for CosmoLab)

### 1.1 The containment hierarchy

Every piece of clinical data in openEHR lives inside this hierarchy:

```
EHR                         ← one per patient, permanent, never deleted
└── COMPOSITION             ← one clinical document / encounter
    └── SECTION             ← organisational grouping (optional in CosmoLab)
        └── ENTRY           ← the actual clinical content (4 subtypes below)
```

### 1.2 The four ENTRY subtypes

| Subtype | What it represents | Example |
|---|---|---|
| **OBSERVATION** | Raw, uninterpreted data — measurements, symptoms, test results | Blood pressure 130/85, Temperature 38.2°C |
| **EVALUATION** | Interpreted / assessed data — clinical judgement applied | Diagnosis: Hypertension (ICD-10: I10), Risk: High |
| **INSTRUCTION** | An order or plan — something that should happen | Administer 500mg Paracetamol every 6h |
| **ACTION** | What was actually done — execution of an instruction | Paracetamol administered at 14:30 by Nurse Svensson |

### 1.3 How COSMIC relates to openEHR

Cambio COSMIC uses its own internal template format that maps bi-directionally to openEHR archetypes. COSMIC does not expose raw openEHR API externally, but internally the data model follows the same containment hierarchy. CosmoLab mirrors this internal structure — EHR → Composition → Entry — without implementing the full archetype engine or ADL schema validation.

### 1.4 What we deliberately exclude

| Feature | Why excluded |
|---|---|
| Full archetype engine (ADL parsing, runtime constraint validation) | Requires a dedicated archetype server; months of work |
| GDL / Clinical Decision Support rules | Cambio's CDS platform is a separate product |
| SNOMED CT / ICD-10 full terminology service | External dependency; ICD-10 codes used as plain strings |
| HL7 FHIR API layer | Out of scope for this sprint |
| EHR versioning (change_control semantics) | Simplified audit trail via RabbitMQ is sufficient |
| Multi-tenancy / organisation hierarchy | Single-organisation CosmoLab |

---

## 2. Selected Features

### Feature 1 — EHR Record

**openEHR concept**: `EHR`

The EHR is the top-level container for all clinical data belonging to one patient. It is permanent — it is never deleted, only marked inactive. In COSMIC, every patient registered in the system has exactly one EHR.

**CosmoLab implementation**:

```
EhrRecord
├── ehrId           UUID        PK  — the openEHR EHR identifier
├── subjectId       UUID        FK → Patient (demographics, separate concern)
├── systemId        VARCHAR     — identifies the creating system ("cosmolab-v1")
├── createdAt       DATETIME2   — when the EHR was first created
├── status          VARCHAR     — ACTIVE | INACTIVE
└── compositions    List<Composition>
```

The `Patient` entity from the original design becomes the **demographics** record. The `EhrRecord` is the clinical container. This separation mirrors COSMIC's architecture: demographics (name, personnummer, address) are managed by a separate demographic service; the EHR holds only clinical data linked to a subject ID.

**REST endpoints**:
```
POST   /api/v1/ehr                          # create EHR for a patient
GET    /api/v1/ehr/{ehrId}                  # get EHR summary
GET    /api/v1/ehr/subject/{subjectId}      # get EHR by patient ID
```

**Interview talking point**: In openEHR, the EHR and the patient's demographics are intentionally decoupled. The EHR references the patient by a subject ID, not by embedding demographic data. This allows the demographic record to change (name changes, corrections) without affecting the integrity of the clinical record.

---

### Feature 2 — Composition

**openEHR concept**: `COMPOSITION`

A Composition is the unit of committal to the EHR — it represents one complete clinical document or encounter. Every time a clinician records something, it goes into a Composition. A Composition has a type (determined by its archetype), a context (who, when, where), and contains one or more Entries.

**CosmoLab implementation**:

```
Composition
├── id              UUID        PK
├── ehrId           UUID        FK → EhrRecord
├── compositionType VARCHAR     — ENCOUNTER_NOTE | ADMISSION | PROGRESS_NOTE | DISCHARGE_SUMMARY
├── authorId        UUID        — clinician who created this composition
├── startTime       DATETIME2   — clinical time (when the encounter happened)
├── commitTime      DATETIME2   — system time (when it was saved)
├── facilityName    VARCHAR     — e.g. "Umeå University Hospital - ICU"
├── status          VARCHAR     — COMPLETE | INCOMPLETE | AMENDED
└── entries         List<Entry> — polymorphic (see Features 3 and 4)
```

This replaces the old `ClinicalNote` entity. A Composition is richer: it has a clinical time separate from the commit time, a status (AMENDED allows corrections while preserving the original), and can contain multiple typed entries.

**REST endpoints**:
```
POST   /api/v1/ehr/{ehrId}/compositions
GET    /api/v1/ehr/{ehrId}/compositions         ?type=ENCOUNTER_NOTE&page=0&size=20
GET    /api/v1/ehr/{ehrId}/compositions/{id}
PUT    /api/v1/ehr/{ehrId}/compositions/{id}    # amend (creates amended version)
```

**Interview talking point**: The separation of `startTime` (clinical time) from `commitTime` (system time) is fundamental in openEHR. A nurse might record an observation made at 06:00 but not commit it to the EHR until 08:30. The clinical timeline must reflect 06:00, but the audit trail captures 08:30. These are two different facts.

---

### Feature 3 — Vital Signs (OBSERVATION)

**openEHR concept**: `OBSERVATION` — archetype `openEHR-EHR-OBSERVATION.vital_signs.v1`

Vital signs are the canonical OBSERVATION in any EHR. They are raw measurements — not interpreted, not diagnosed. The same reading (BP 140/90) might be normal context in one patient and a crisis in another. That interpretation belongs in an EVALUATION, not here.

**CosmoLab implementation**:

```
VitalSigns  (stored as an Entry inside a Composition)
├── id                  UUID        PK
├── compositionId       UUID        FK → Composition
├── recordedAt          DATETIME2   — when the measurement was taken
├── recordedBy          UUID        — clinician / device

── Measurements (all nullable — not every vital is taken every time)
├── systolicBP          INTEGER     mmHg
├── diastolicBP         INTEGER     mmHg
├── heartRate           INTEGER     bpm
├── respiratoryRate     INTEGER     breaths/min
├── temperature         DECIMAL     °C  (DECIMAL(4,1) — e.g. 38.2)
├── oxygenSaturation    DECIMAL     %   (DECIMAL(5,2) — e.g. 97.50)
└── weight              DECIMAL     kg  (DECIMAL(5,2) — e.g. 72.50)
```

**Normal ranges** (used in UI to highlight abnormal values — no CDS logic, just display):

| Measurement | Low | High | Unit |
|---|---|---|---|
| Systolic BP | 90 | 140 | mmHg |
| Diastolic BP | 60 | 90 | mmHg |
| Heart rate | 60 | 100 | bpm |
| Respiratory rate | 12 | 20 | breaths/min |
| Temperature | 36.1 | 37.2 | °C |
| SpO2 | 95 | — | % |
| Weight | — | — | kg (no range) |

**REST endpoints**:
```
POST   /api/v1/ehr/{ehrId}/compositions/{cid}/vitals
GET    /api/v1/ehr/{ehrId}/vitals                     ?from=2024-01-01&to=2024-01-31
GET    /api/v1/ehr/{ehrId}/vitals/latest
```

`GET /vitals/latest` is used by the Ward Overview (Feature 5) to show current patient status.

**Angular component**: `VitalSignsChartComponent` — a small sparkline per measurement showing trend over the last 5 recordings. Abnormal values highlighted in amber/red using the ranges table above.

**Interview talking point**: In openEHR, a VitalSigns OBSERVATION stores measurements as `DV_QUANTITY` data types, which include both the magnitude and the unit. CosmoLab simplifies this to typed columns, but the principle — structured, unit-aware storage rather than free text — is the same. Storing "38.2" as a `DECIMAL` and "°C" as the column's implicit unit is the pragmatic equivalent.

---

### Feature 4 — Problem List (EVALUATION)

**openEHR concept**: `EVALUATION` — archetype `openEHR-EHR-EVALUATION.problem_diagnosis.v1`

The Problem List is an EVALUATION because it represents a clinician's interpretation — a diagnosis applied to a patient after assessing observations. The same measurement that triggers an OBSERVATION (BP 160/100) leads to a problem list EVALUATION (Diagnosis: Essential Hypertension, ICD-10: I10).

**CosmoLab implementation**:

```
ProblemListEntry  (stored as an Entry inside a Composition, also queryable standalone)
├── id              UUID        PK
├── compositionId   UUID        FK → Composition (where it was first recorded)
├── ehrId           UUID        FK → EhrRecord   (for direct querying without Composition join)
├── icd10Code       VARCHAR     — e.g. "I10", "J45.9", "E11"   (plain string, no terminology service)
├── displayName     VARCHAR     — e.g. "Essential Hypertension", "Moderate persistent asthma"
├── severity        VARCHAR     — MILD | MODERATE | SEVERE
├── status          VARCHAR     — ACTIVE | INACTIVE | RESOLVED | REFUTED
├── onsetDate       DATE        — when the problem started (clinical time)
├── resolvedDate    DATE        — nullable; when it resolved
├── recordedAt      DATETIME2   — when entered into the EHR
└── recordedBy      UUID        — clinician
```

**Common ICD-10 codes** pre-seeded in `V2__seed_data.sql` for realistic testing:

| Code | Name |
|---|---|
| I10 | Essential hypertension |
| E11 | Type 2 diabetes mellitus |
| J45.9 | Asthma, unspecified |
| N18.3 | Chronic kidney disease, stage 3 |
| I50.9 | Heart failure, unspecified |
| F32.1 | Moderate depressive episode |
| M54.5 | Low back pain |

**REST endpoints**:
```
POST   /api/v1/ehr/{ehrId}/problems
GET    /api/v1/ehr/{ehrId}/problems                   ?status=ACTIVE
GET    /api/v1/ehr/{ehrId}/problems/{id}
PUT    /api/v1/ehr/{ehrId}/problems/{id}              # update status, resolvedDate
```

**Interview talking point**: The distinction between OBSERVATION and EVALUATION is one of the most important concepts in openEHR. An OBSERVATION records what was measured. An EVALUATION records what a clinician decided it means. They can be separated in time — an observation might be recorded by a nurse, and the diagnosis added by a physician two hours later. Separating them in the data model reflects this clinical reality.

---

### Feature 5 — Ward Overview

**openEHR concept**: Cross-EHR query — no single archetype; maps to COSMIC's "Overviews" feature

The Ward Overview is not a new entity — it is an aggregation query across multiple EHRs. It answers the question a ward nurse asks every morning: "Who is in my ward, what are their latest vitals, and what are their active problems?"

In COSMIC, overviews are views that span multiple areas, each with different requirements that influence their properties, content, and functionality.

**CosmoLab implementation**:

The Ward Overview is a read-only query endpoint. No new table — it joins across:
- `Patient` (name, bed, admission date)
- `EhrRecord` (ehrId, status)
- `VitalSigns` (latest per EHR — via window function or subquery)
- `ProblemListEntry` (count of ACTIVE problems per EHR)
- `Composition` (latest composition type and time)

```
GET /api/v1/ward/{wardId}/overview

Response:
{
  "wardId": "ICU",
  "generatedAt": "2024-11-13T07:30:00Z",
  "patients": [
    {
      "patientId": "...",
      "ehrId": "...",
      "displayName": "Eriksson, Lars",
      "personalNumber": "19540312-1234",
      "bedNumber": "ICU-03",
      "admissionDate": "2024-11-10",
      "latestVitals": {
        "recordedAt": "2024-11-13T06:15:00Z",
        "heartRate": 92,
        "systolicBP": 148,
        "diastolicBP": 94,
        "temperature": 37.8,
        "oxygenSaturation": 96.5,
        "flags": ["systolicBP:HIGH", "diastolicBP:HIGH"]   ← derived from normal ranges
      },
      "activeProblemCount": 3,
      "latestComposition": {
        "type": "PROGRESS_NOTE",
        "committedAt": "2024-11-12T16:45:00Z"
      }
    }
  ]
}
```

**Performance note**: This query is the primary JMeter/Gatling target. It joins 4 tables, uses a window function for latest vitals per patient, and returns a variable-width payload. Under load with 20 patients per ward and 50 concurrent requests, it will expose N+1 risks and missing index issues faster than any other endpoint. That is by design.

**Angular component**: `WardOverviewComponent` — replaces the old `DashboardComponent`. A dense table with one row per patient. Vital sign cells colour-coded (green/amber/red) from the `flags` array. Click a row to navigate to `PatientDetailComponent`.

---

## 3. Revised Domain Model

### 3.1 Entity relationship

```
Patient (demographics)
  │
  └──► EhrRecord (1:1)
            │
            └──► Composition (1:many)
                      │
                      ├──► VitalSigns      (OBSERVATION entry)
                      └──► ProblemListEntry (EVALUATION entry)

ProblemListEntry also has direct FK to EhrRecord
(for efficient problem list queries without joining through Composition)
```

### 3.2 Revised Java package additions

```
com.cosmolab
├── domain/
│   ├── ehr/
│   │   ├── EhrRecord.java
│   │   └── EhrRepository.java
│   ├── composition/
│   │   ├── Composition.java
│   │   ├── CompositionType.java       # Enum
│   │   ├── CompositionStatus.java     # Enum
│   │   └── CompositionRepository.java
│   ├── observation/
│   │   ├── VitalSigns.java
│   │   └── VitalSignsRepository.java
│   └── evaluation/
│       ├── ProblemListEntry.java
│       ├── ProblemStatus.java         # Enum
│       ├── Severity.java              # Enum
│       └── ProblemListRepository.java
│
├── application/
│   ├── ehr/
│   │   └── EhrService.java
│   ├── composition/
│   │   └── CompositionService.java
│   ├── observation/
│   │   └── VitalSignsService.java
│   ├── evaluation/
│   │   └── ProblemListService.java
│   └── ward/
│       └── WardOverviewService.java   # aggregation query, no new domain entity
│
└── interfaces/rest/
    ├── EhrController.java
    ├── CompositionController.java
    ├── VitalSignsController.java
    ├── ProblemListController.java
    └── WardOverviewController.java
```

### 3.3 Revised Flyway migrations

```
V1__init_schema.sql          ← patients, ehr_records tables + indexes
V2__compositions.sql         ← compositions table
V3__vital_signs.sql          ← vital_signs table
V4__problem_list.sql         ← problem_list_entries table
V5__seed_patients.sql        ← 20 patients with EHR records, 3 wards
V6__seed_compositions.sql    ← compositions + vital signs + problems per patient
```

### 3.4 Revised REST API surface

```
# Demographics
GET    /api/v1/patients                    ?ward=ICU&status=ACTIVE&search=Eriksson
GET    /api/v1/patients/{id}
POST   /api/v1/patients
PUT    /api/v1/patients/{id}

# EHR
POST   /api/v1/ehr
GET    /api/v1/ehr/{ehrId}
GET    /api/v1/ehr/subject/{patientId}

# Compositions
POST   /api/v1/ehr/{ehrId}/compositions
GET    /api/v1/ehr/{ehrId}/compositions    ?type=ENCOUNTER_NOTE&page=0&size=20
GET    /api/v1/ehr/{ehrId}/compositions/{id}
PUT    /api/v1/ehr/{ehrId}/compositions/{id}

# Vital Signs
POST   /api/v1/ehr/{ehrId}/compositions/{cid}/vitals
GET    /api/v1/ehr/{ehrId}/vitals          ?from=2024-01-01
GET    /api/v1/ehr/{ehrId}/vitals/latest

# Problem List
POST   /api/v1/ehr/{ehrId}/problems
GET    /api/v1/ehr/{ehrId}/problems        ?status=ACTIVE
GET    /api/v1/ehr/{ehrId}/problems/{id}
PUT    /api/v1/ehr/{ehrId}/problems/{id}

# Ward Overview (aggregation query)
GET    /api/v1/ward/{wardId}/overview
```

---

## 4. Angular Feature Modules — Revised

The old `features/patients/patient-notes/` is replaced by richer clinical components.

```
features/
├── ward/                        # /ward/:wardId — WardOverviewComponent
│   └── ward-overview/
├── patients/                    # /patients, /patients/:id
│   ├── patient-list/            # Unchanged — demographics list
│   └── patient-detail/          # Tabs: Overview | Vitals | Problems | Compositions
│       ├── patient-overview/    # EHR summary, latest vitals snapshot
│       ├── vital-signs/         # Trend chart + latest readings table
│       │   └── vital-signs-chart/   # Sparkline per measurement (shared component)
│       ├── problem-list/        # Active + resolved problems, add problem form
│       └── compositions/        # Composition list + composition detail view
└── admin/                       # Unchanged
```

---

## 5. Impact on Sprint Plan

The openEHR features add scope but not additional days — they replace the simpler domain, not add on top of it. Days 1 and 2 absorb the extra entities; Days 3 and 4 reflect the richer Angular components.

| Day | Original scope | Revised scope |
|---|---|---|
| 1 | Patient + ClinicalNote entities, Flyway V1–V2 | Patient + EhrRecord + Composition entities, Flyway V1–V4 |
| 2 | Patient API, Note API, Kafka events | EHR API, Composition API, VitalSigns API, ProblemList API, RabbitMQ events |
| 3 | 4 pages: Dashboard, PatientList, PatientDetail, Admin | 4 pages: WardOverview, PatientList, PatientDetail (richer tabs), Admin |
| 4 | Containerisation + Playwright | Unchanged — Playwright test scenarios updated to match new routes |
| 5–7 | Jenkins, Observability, Load tests, Optimisation | Unchanged — WardOverview endpoint becomes primary load test target |

The WardOverview query (joining 4 tables, window function for latest vitals) is a significantly better load test target than a simple patient list. It will expose real performance issues at lower concurrency.

---

## 6. Deferred Features (future phases)

| Feature | openEHR concept | Why deferred |
|---|---|---|
| Medication orders | INSTRUCTION | Requires dispensing workflow, drug database |
| Medication administration record | ACTION | Depends on INSTRUCTION being implemented first |
| Care plan | INSTRUCTION + ACTION | Scope; can be added as Phase 2 |
| Clinical Decision Support | GDL rules | Separate Cambio product; requires rule engine |
| FHIR R4 API | HL7 FHIR | Separate API layer on top of openEHR model; Phase 3 |
| EHR versioning | change_control | Full audit trail via RabbitMQ is sufficient for now |
| Referrals | INSTRUCTION | Requires inter-department routing |

---

*Last updated: Day 0 — features defined. Update §5 impact table if sprint scope changes.*

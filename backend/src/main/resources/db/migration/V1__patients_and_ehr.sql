CREATE TABLE patients (
    id              UNIQUEIDENTIFIER NOT NULL DEFAULT NEWSEQUENTIALID() PRIMARY KEY,
    first_name      NVARCHAR(100)    NOT NULL,
    last_name       NVARCHAR(100)    NOT NULL,
    personal_number NVARCHAR(13)     NOT NULL,
    date_of_birth   DATE             NOT NULL,
    gender          NVARCHAR(20)     NOT NULL,
    ward            NVARCHAR(100)    NOT NULL,
    status          NVARCHAR(20)     NOT NULL,
    created_at      DATETIMEOFFSET(6)        NOT NULL,
    updated_at      DATETIMEOFFSET(6)        NOT NULL,

    CONSTRAINT uq_patients_personal_number UNIQUE (personal_number),
    CONSTRAINT chk_patients_status CHECK (status IN ('ACTIVE','DISCHARGED','TRANSFERRED')),
    CONSTRAINT chk_patients_gender CHECK (gender IN ('MALE','FEMALE','OTHER'))
);

CREATE NONCLUSTERED INDEX idx_patients_ward_status ON patients (ward, status);

CREATE TABLE ehr_records (
    ehr_id     UNIQUEIDENTIFIER NOT NULL DEFAULT NEWSEQUENTIALID() PRIMARY KEY,
    subject_id UNIQUEIDENTIFIER NOT NULL,
    system_id  NVARCHAR(100)    NOT NULL DEFAULT 'cosmolab-v1',
    created_at DATETIMEOFFSET(6)        NOT NULL,
    status     NVARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT fk_ehr_patient FOREIGN KEY (subject_id) REFERENCES patients(id),
    CONSTRAINT uq_ehr_subject UNIQUE (subject_id),
    CONSTRAINT chk_ehr_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

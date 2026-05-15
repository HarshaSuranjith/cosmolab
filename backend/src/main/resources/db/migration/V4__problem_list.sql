CREATE TABLE problem_list_entries (
    id            UNIQUEIDENTIFIER NOT NULL DEFAULT NEWSEQUENTIALID() PRIMARY KEY,
    composition_id UNIQUEIDENTIFIER NOT NULL,
    ehr_id        UNIQUEIDENTIFIER NOT NULL,
    icd10_code    NVARCHAR(20)     NOT NULL,
    display_name  NVARCHAR(200)    NOT NULL,
    severity      NVARCHAR(20)     NOT NULL,
    status        NVARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    onset_date    DATE             NULL,
    resolved_date DATE             NULL,
    recorded_at   DATETIME2        NOT NULL,
    recorded_by   UNIQUEIDENTIFIER NOT NULL,

    CONSTRAINT fk_problem_composition FOREIGN KEY (composition_id) REFERENCES compositions(id),
    CONSTRAINT fk_problem_ehr         FOREIGN KEY (ehr_id) REFERENCES ehr_records(ehr_id),
    CONSTRAINT chk_problem_severity   CHECK (severity IN ('MILD','MODERATE','SEVERE')),
    CONSTRAINT chk_problem_status     CHECK (status IN ('ACTIVE','INACTIVE','RESOLVED','REFUTED'))
);

CREATE NONCLUSTERED INDEX idx_problems_ehr_status ON problem_list_entries (ehr_id, status);

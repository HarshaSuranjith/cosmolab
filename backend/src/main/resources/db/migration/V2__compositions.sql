CREATE TABLE compositions (
    id               UNIQUEIDENTIFIER NOT NULL DEFAULT NEWSEQUENTIALID() PRIMARY KEY,
    ehr_id           UNIQUEIDENTIFIER NOT NULL,
    composition_type NVARCHAR(50)     NOT NULL,
    author_id        UNIQUEIDENTIFIER NOT NULL,
    start_time       DATETIMEOFFSET(6)        NOT NULL,
    commit_time      DATETIMEOFFSET(6)        NOT NULL,
    facility_name    NVARCHAR(200)    NULL,
    status           NVARCHAR(20)     NOT NULL DEFAULT 'COMPLETE',

    CONSTRAINT fk_composition_ehr FOREIGN KEY (ehr_id) REFERENCES ehr_records(ehr_id),
    CONSTRAINT chk_composition_type CHECK (composition_type IN ('ENCOUNTER_NOTE','ADMISSION','PROGRESS_NOTE','DISCHARGE_SUMMARY')),
    CONSTRAINT chk_composition_status CHECK (status IN ('COMPLETE','INCOMPLETE','AMENDED'))
);

CREATE NONCLUSTERED INDEX idx_compositions_ehr_commit ON compositions (ehr_id, commit_time DESC);

CREATE TABLE vital_signs (
    id                 UNIQUEIDENTIFIER NOT NULL DEFAULT NEWSEQUENTIALID() PRIMARY KEY,
    composition_id     UNIQUEIDENTIFIER NOT NULL,
    recorded_at        DATETIME2        NOT NULL,
    recorded_by        UNIQUEIDENTIFIER NOT NULL,
    systolic_bp        INT              NULL,
    diastolic_bp       INT              NULL,
    heart_rate         INT              NULL,
    respiratory_rate   INT              NULL,
    temperature        DECIMAL(4,1)     NULL,
    oxygen_saturation  DECIMAL(5,2)     NULL,
    weight             DECIMAL(5,2)     NULL,

    CONSTRAINT fk_vitals_composition FOREIGN KEY (composition_id) REFERENCES compositions(id)
);

CREATE NONCLUSTERED INDEX idx_vital_signs_composition_recorded ON vital_signs (composition_id, recorded_at DESC);

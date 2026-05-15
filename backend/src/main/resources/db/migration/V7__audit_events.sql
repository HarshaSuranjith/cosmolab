-- event_id UNIQUE enforces idempotency: a redelivered message with the same eventId is ignored.
CREATE TABLE audit_events (
    id           UNIQUEIDENTIFIER  NOT NULL CONSTRAINT PK_audit_events PRIMARY KEY DEFAULT NEWID(),
    event_id     NVARCHAR(36)      NOT NULL,
    event_type   NVARCHAR(100)     NOT NULL,
    aggregate_id UNIQUEIDENTIFIER  NOT NULL,
    payload      NVARCHAR(MAX),
    occurred_at  DATETIMEOFFSET(6) NOT NULL,
    processed_at DATETIMEOFFSET(6) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT UQ_audit_events_event_id UNIQUE (event_id)
);

CREATE NONCLUSTERED INDEX IX_audit_events_aggregate ON audit_events (aggregate_id, occurred_at DESC);

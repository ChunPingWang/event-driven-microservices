-- Create outbox_events table for Outbox Pattern implementation
CREATE TABLE outbox_events (
    event_id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    metadata TEXT,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processing_attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    last_attempt_at TIMESTAMP,
    next_attempt_at TIMESTAMP,
    error_message TEXT
);

-- Create indexes for efficient querying
CREATE INDEX idx_outbox_events_processed ON outbox_events(processed);
CREATE INDEX idx_outbox_events_created_at ON outbox_events(created_at);
CREATE INDEX idx_outbox_events_event_type ON outbox_events(event_type);
CREATE INDEX idx_outbox_events_aggregate_id ON outbox_events(aggregate_id);
CREATE INDEX idx_outbox_events_aggregate_type ON outbox_events(aggregate_type);
CREATE INDEX idx_outbox_events_next_attempt_at ON outbox_events(next_attempt_at);
CREATE INDEX idx_outbox_events_processing_attempts ON outbox_events(processing_attempts);

-- Create composite index for outbox publisher queries
CREATE INDEX idx_outbox_events_unprocessed ON outbox_events(processed, next_attempt_at, processing_attempts) 
WHERE processed = FALSE;

-- Add comments
COMMENT ON TABLE outbox_events IS 'Outbox pattern events table for reliable message publishing';
COMMENT ON COLUMN outbox_events.event_id IS 'Unique event identifier';
COMMENT ON COLUMN outbox_events.event_type IS 'Type of domain event';
COMMENT ON COLUMN outbox_events.aggregate_id IS 'Identifier of the aggregate that generated the event';
COMMENT ON COLUMN outbox_events.aggregate_type IS 'Type of the aggregate (e.g., Payment, Order)';
COMMENT ON COLUMN outbox_events.payload IS 'Event payload in JSON format';
COMMENT ON COLUMN outbox_events.metadata IS 'Additional event metadata in JSON format';
COMMENT ON COLUMN outbox_events.processed IS 'Whether the event has been successfully published';
COMMENT ON COLUMN outbox_events.processing_attempts IS 'Number of processing attempts made';
COMMENT ON COLUMN outbox_events.max_attempts IS 'Maximum allowed processing attempts';
COMMENT ON COLUMN outbox_events.created_at IS 'Event creation timestamp';
COMMENT ON COLUMN outbox_events.processed_at IS 'Event processing completion timestamp';
COMMENT ON COLUMN outbox_events.last_attempt_at IS 'Last processing attempt timestamp';
COMMENT ON COLUMN outbox_events.next_attempt_at IS 'Next scheduled processing attempt timestamp';
COMMENT ON COLUMN outbox_events.error_message IS 'Error message from last failed processing attempt';
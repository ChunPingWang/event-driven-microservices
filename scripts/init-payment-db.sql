-- Payment Service Database Initialization Script

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_payment_records_transaction_id ON payment_records(transaction_id);
CREATE INDEX IF NOT EXISTS idx_payment_records_order_id ON payment_records(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_records_status ON payment_records(status);
CREATE INDEX IF NOT EXISTS idx_payment_records_processed_at ON payment_records(processed_at);

-- Create indexes for outbox events
CREATE INDEX IF NOT EXISTS idx_outbox_events_processed ON outbox_events(processed);
CREATE INDEX IF NOT EXISTS idx_outbox_events_created_at ON outbox_events(created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_events_event_type ON outbox_events(event_type);
CREATE INDEX IF NOT EXISTS idx_outbox_events_aggregate_id ON outbox_events(aggregate_id);

-- Create indexes for API request logs
CREATE INDEX IF NOT EXISTS idx_api_request_logs_operation ON api_request_logs(operation);
CREATE INDEX IF NOT EXISTS idx_api_request_logs_timestamp ON api_request_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_api_request_logs_status ON api_request_logs(status);

-- Create indexes for message event logs
CREATE INDEX IF NOT EXISTS idx_message_event_logs_event_type ON message_event_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_message_event_logs_timestamp ON message_event_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_message_event_logs_status ON message_event_logs(status);

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO paymentuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO paymentuser;
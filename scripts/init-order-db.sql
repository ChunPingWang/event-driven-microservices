-- Order Service Database Initialization Script

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);

-- Create indexes for payment requests
CREATE INDEX IF NOT EXISTS idx_payment_requests_order_id ON payment_requests(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_requests_status ON payment_requests(status);
CREATE INDEX IF NOT EXISTS idx_payment_requests_sent_at ON payment_requests(sent_at);

-- Create indexes for retry history
CREATE INDEX IF NOT EXISTS idx_retry_history_order_id ON retry_history(order_id);
CREATE INDEX IF NOT EXISTS idx_retry_history_created_at ON retry_history(created_at);

-- Create indexes for retry attempts
CREATE INDEX IF NOT EXISTS idx_retry_attempts_retry_history_id ON retry_attempts(retry_history_id);
CREATE INDEX IF NOT EXISTS idx_retry_attempts_attempted_at ON retry_attempts(attempted_at);

-- Create indexes for API request logs
CREATE INDEX IF NOT EXISTS idx_api_request_logs_operation ON api_request_logs(operation);
CREATE INDEX IF NOT EXISTS idx_api_request_logs_timestamp ON api_request_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_api_request_logs_status ON api_request_logs(status);

-- Create indexes for message event logs
CREATE INDEX IF NOT EXISTS idx_message_event_logs_event_type ON message_event_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_message_event_logs_timestamp ON message_event_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_message_event_logs_status ON message_event_logs(status);

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO orderuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO orderuser;
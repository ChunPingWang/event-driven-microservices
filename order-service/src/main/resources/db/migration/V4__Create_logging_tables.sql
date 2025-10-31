-- Create api_request_logs table
CREATE TABLE api_request_logs (
    request_id VARCHAR(255) PRIMARY KEY,
    operation VARCHAR(255) NOT NULL,
    class_name VARCHAR(255) NOT NULL,
    method_name VARCHAR(255) NOT NULL,
    request_payload TEXT,
    response_payload TEXT,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    execution_time_ms BIGINT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create message_event_logs table
CREATE TABLE message_event_logs (
    message_id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    class_name VARCHAR(255) NOT NULL,
    method_name VARCHAR(255) NOT NULL,
    payload TEXT,
    headers TEXT,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for api_request_logs
CREATE INDEX idx_api_request_logs_operation ON api_request_logs(operation);
CREATE INDEX idx_api_request_logs_timestamp ON api_request_logs(timestamp);
CREATE INDEX idx_api_request_logs_status ON api_request_logs(status);
CREATE INDEX idx_api_request_logs_class_name ON api_request_logs(class_name);
CREATE INDEX idx_api_request_logs_execution_time ON api_request_logs(execution_time_ms);

-- Create indexes for message_event_logs
CREATE INDEX idx_message_event_logs_event_type ON message_event_logs(event_type);
CREATE INDEX idx_message_event_logs_timestamp ON message_event_logs(timestamp);
CREATE INDEX idx_message_event_logs_status ON message_event_logs(status);
CREATE INDEX idx_message_event_logs_class_name ON message_event_logs(class_name);

-- Add comments
COMMENT ON TABLE api_request_logs IS 'API request logging table for AOP logging starter';
COMMENT ON COLUMN api_request_logs.request_id IS 'Unique request identifier';
COMMENT ON COLUMN api_request_logs.operation IS 'API operation name';
COMMENT ON COLUMN api_request_logs.class_name IS 'Controller class name';
COMMENT ON COLUMN api_request_logs.method_name IS 'Controller method name';
COMMENT ON COLUMN api_request_logs.request_payload IS 'Request payload JSON';
COMMENT ON COLUMN api_request_logs.response_payload IS 'Response payload JSON';
COMMENT ON COLUMN api_request_logs.status IS 'Request status (SUCCESS, ERROR)';
COMMENT ON COLUMN api_request_logs.error_message IS 'Error message if request failed';
COMMENT ON COLUMN api_request_logs.execution_time_ms IS 'Request execution time in milliseconds';

COMMENT ON TABLE message_event_logs IS 'Message event logging table for AOP logging starter';
COMMENT ON COLUMN message_event_logs.message_id IS 'Unique message identifier';
COMMENT ON COLUMN message_event_logs.event_type IS 'Message event type';
COMMENT ON COLUMN message_event_logs.class_name IS 'Handler class name';
COMMENT ON COLUMN message_event_logs.method_name IS 'Handler method name';
COMMENT ON COLUMN message_event_logs.payload IS 'Message payload JSON';
COMMENT ON COLUMN message_event_logs.headers IS 'Message headers JSON';
COMMENT ON COLUMN message_event_logs.status IS 'Message processing status (SUCCESS, ERROR)';
COMMENT ON COLUMN message_event_logs.error_message IS 'Error message if processing failed';
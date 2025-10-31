-- Create retry_history table
CREATE TABLE retry_history (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL,
    payment_request_id VARCHAR(255) NOT NULL,
    total_attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 5,
    next_retry_at TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create retry_attempts table
CREATE TABLE retry_attempts (
    id BIGSERIAL PRIMARY KEY,
    retry_history_id BIGINT NOT NULL,
    attempt_number INTEGER NOT NULL,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    result VARCHAR(50) NOT NULL,
    error_message TEXT,
    next_attempt_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_retry_history_order_id ON retry_history(order_id);
CREATE INDEX idx_retry_history_payment_request_id ON retry_history(payment_request_id);
CREATE INDEX idx_retry_history_status ON retry_history(status);
CREATE INDEX idx_retry_history_next_retry_at ON retry_history(next_retry_at);
CREATE INDEX idx_retry_history_created_at ON retry_history(created_at);

CREATE INDEX idx_retry_attempts_retry_history_id ON retry_attempts(retry_history_id);
CREATE INDEX idx_retry_attempts_attempted_at ON retry_attempts(attempted_at);
CREATE INDEX idx_retry_attempts_result ON retry_attempts(result);

-- Add foreign key constraints
ALTER TABLE retry_history 
ADD CONSTRAINT fk_retry_history_order_id 
FOREIGN KEY (order_id) REFERENCES orders(order_id);

ALTER TABLE retry_history 
ADD CONSTRAINT fk_retry_history_payment_request_id 
FOREIGN KEY (payment_request_id) REFERENCES payment_requests(request_id);

ALTER TABLE retry_attempts 
ADD CONSTRAINT fk_retry_attempts_retry_history_id 
FOREIGN KEY (retry_history_id) REFERENCES retry_history(id);

-- Add comments
COMMENT ON TABLE retry_history IS 'Payment retry history tracking';
COMMENT ON COLUMN retry_history.order_id IS 'Associated order identifier';
COMMENT ON COLUMN retry_history.payment_request_id IS 'Associated payment request identifier';
COMMENT ON COLUMN retry_history.total_attempts IS 'Total number of retry attempts made';
COMMENT ON COLUMN retry_history.max_attempts IS 'Maximum allowed retry attempts';
COMMENT ON COLUMN retry_history.next_retry_at IS 'Next scheduled retry timestamp';
COMMENT ON COLUMN retry_history.status IS 'Retry status (ACTIVE, COMPLETED, FAILED, EXHAUSTED)';

COMMENT ON TABLE retry_attempts IS 'Individual retry attempt records';
COMMENT ON COLUMN retry_attempts.retry_history_id IS 'Associated retry history identifier';
COMMENT ON COLUMN retry_attempts.attempt_number IS 'Attempt sequence number';
COMMENT ON COLUMN retry_attempts.attempted_at IS 'Attempt execution timestamp';
COMMENT ON COLUMN retry_attempts.result IS 'Attempt result (SUCCESS, FAILURE, TIMEOUT)';
COMMENT ON COLUMN retry_attempts.error_message IS 'Error details if attempt failed';
COMMENT ON COLUMN retry_attempts.next_attempt_at IS 'Next scheduled attempt timestamp';
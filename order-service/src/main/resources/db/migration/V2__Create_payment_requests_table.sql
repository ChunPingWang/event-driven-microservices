-- Create payment_requests table
CREATE TABLE payment_requests (
    request_id VARCHAR(255) PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_retry_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_payment_requests_order_id ON payment_requests(order_id);
CREATE INDEX idx_payment_requests_status ON payment_requests(status);
CREATE INDEX idx_payment_requests_sent_at ON payment_requests(sent_at);
CREATE INDEX idx_payment_requests_transaction_id ON payment_requests(transaction_id);

-- Add foreign key constraint
ALTER TABLE payment_requests 
ADD CONSTRAINT fk_payment_requests_order_id 
FOREIGN KEY (order_id) REFERENCES orders(order_id);

-- Add comments
COMMENT ON TABLE payment_requests IS 'Payment request tracking table';
COMMENT ON COLUMN payment_requests.request_id IS 'Unique payment request identifier';
COMMENT ON COLUMN payment_requests.order_id IS 'Associated order identifier';
COMMENT ON COLUMN payment_requests.transaction_id IS 'Payment transaction identifier';
COMMENT ON COLUMN payment_requests.status IS 'Request status (SENT, CONFIRMED, FAILED, RETRY_PENDING)';
COMMENT ON COLUMN payment_requests.sent_at IS 'Request sent timestamp';
COMMENT ON COLUMN payment_requests.confirmed_at IS 'Confirmation received timestamp';
COMMENT ON COLUMN payment_requests.retry_count IS 'Number of retry attempts';
COMMENT ON COLUMN payment_requests.last_retry_at IS 'Last retry attempt timestamp';
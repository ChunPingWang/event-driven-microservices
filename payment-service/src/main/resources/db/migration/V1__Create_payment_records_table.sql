-- Create payment_records table
CREATE TABLE payment_records (
    payment_id VARCHAR(255) PRIMARY KEY,
    transaction_id VARCHAR(255) NOT NULL UNIQUE,
    order_id VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(50) NOT NULL,
    card_number_masked VARCHAR(20),
    card_holder_name VARCHAR(255),
    gateway_response TEXT,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_payment_records_transaction_id ON payment_records(transaction_id);
CREATE INDEX idx_payment_records_order_id ON payment_records(order_id);
CREATE INDEX idx_payment_records_customer_id ON payment_records(customer_id);
CREATE INDEX idx_payment_records_status ON payment_records(status);
CREATE INDEX idx_payment_records_processed_at ON payment_records(processed_at);
CREATE INDEX idx_payment_records_created_at ON payment_records(created_at);

-- Add comments
COMMENT ON TABLE payment_records IS 'Payment processing records table';
COMMENT ON COLUMN payment_records.payment_id IS 'Unique payment identifier';
COMMENT ON COLUMN payment_records.transaction_id IS 'Unique transaction identifier';
COMMENT ON COLUMN payment_records.order_id IS 'Associated order identifier';
COMMENT ON COLUMN payment_records.customer_id IS 'Customer identifier';
COMMENT ON COLUMN payment_records.amount IS 'Payment amount';
COMMENT ON COLUMN payment_records.currency IS 'Currency code (ISO 4217)';
COMMENT ON COLUMN payment_records.status IS 'Payment status (PENDING, SUCCESS, FAILED, CANCELLED)';
COMMENT ON COLUMN payment_records.card_number_masked IS 'Masked credit card number for security';
COMMENT ON COLUMN payment_records.card_holder_name IS 'Credit card holder name';
COMMENT ON COLUMN payment_records.gateway_response IS 'Payment gateway response details';
COMMENT ON COLUMN payment_records.processed_at IS 'Payment processing completion timestamp';
COMMENT ON COLUMN payment_records.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN payment_records.updated_at IS 'Last update timestamp';
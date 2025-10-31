-- Create orders table
CREATE TABLE orders (
    order_id VARCHAR(255) PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index for better query performance
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);

-- Add comments
COMMENT ON TABLE orders IS 'Order aggregate root table';
COMMENT ON COLUMN orders.order_id IS 'Unique order identifier';
COMMENT ON COLUMN orders.customer_id IS 'Customer identifier';
COMMENT ON COLUMN orders.amount IS 'Order amount';
COMMENT ON COLUMN orders.currency IS 'Currency code (ISO 4217)';
COMMENT ON COLUMN orders.status IS 'Order status (CREATED, PAYMENT_PENDING, PAYMENT_CONFIRMED, PAYMENT_FAILED, CANCELLED)';
COMMENT ON COLUMN orders.created_at IS 'Order creation timestamp';
COMMENT ON COLUMN orders.updated_at IS 'Last update timestamp';
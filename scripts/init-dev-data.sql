-- Development environment sample data
-- This script is for development/testing purposes only

-- Order Service Sample Data
INSERT INTO orders (order_id, customer_id, amount, currency, status, created_at, updated_at) VALUES
('order-001', 'customer-001', 99.99, 'USD', 'CREATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('order-002', 'customer-002', 149.50, 'USD', 'PAYMENT_PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('order-003', 'customer-001', 75.25, 'USD', 'PAYMENT_CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO payment_requests (request_id, order_id, transaction_id, status, sent_at, retry_count) VALUES
('req-001', 'order-002', 'txn-001', 'SENT', CURRENT_TIMESTAMP, 0),
('req-002', 'order-003', 'txn-002', 'CONFIRMED', CURRENT_TIMESTAMP, 0);

-- Payment Service Sample Data
INSERT INTO payment_records (payment_id, transaction_id, order_id, customer_id, amount, currency, status, card_number_masked, card_holder_name, processed_at, created_at, updated_at) VALUES
('pay-001', 'txn-002', 'order-003', 'customer-001', 75.25, 'USD', 'SUCCESS', '**** **** **** 1234', 'John Doe', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sample outbox events (already processed)
INSERT INTO outbox_events (event_id, event_type, aggregate_id, aggregate_type, payload, processed, created_at, processed_at) VALUES
('evt-001', 'PaymentProcessedEvent', 'pay-001', 'Payment', '{"paymentId":"pay-001","orderId":"order-003","amount":75.25,"currency":"USD","status":"SUCCESS"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
--liquibase formatted sql

--changeset awesome-pizza:1
--comment: Create orders and order_items tables

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_code VARCHAR(255) NOT NULL UNIQUE,
    customer_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED')),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    pizza_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    order_id BIGINT NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- Indici per performance
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);

--rollback DROP INDEX idx_order_items_order_id;
--rollback DROP INDEX idx_orders_created_at;
--rollback DROP INDEX idx_orders_status;
--rollback DROP TABLE order_items;
--rollback DROP TABLE orders;

--liquibase formatted sql

--changeset awesome-pizza:2
--comment: Add composite index for FIFO queue queries (status + created_at)

CREATE INDEX idx_orders_status_created_at ON orders(status, created_at);

--rollback DROP INDEX idx_orders_status_created_at;

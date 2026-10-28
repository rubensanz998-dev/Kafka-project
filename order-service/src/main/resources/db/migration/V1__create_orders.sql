CREATE TABLE orders (
    id          VARCHAR(36)    PRIMARY KEY,
    customer_id VARCHAR(100)   NOT NULL,
    product_id  VARCHAR(100)   NOT NULL,
    quantity    INTEGER        NOT NULL,
    amount      NUMERIC(12, 2) NOT NULL,
    status      VARCHAR(20)    NOT NULL,
    reason      VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    version     BIGINT         NOT NULL
);

CREATE INDEX idx_orders_customer ON orders (customer_id, created_at);

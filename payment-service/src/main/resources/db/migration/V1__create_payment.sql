-- La PK sobre order_id garantiza que un pedido no se cobra dos veces.
CREATE TABLE payment (
    order_id    VARCHAR(36)    PRIMARY KEY,
    customer_id VARCHAR(100)   NOT NULL,
    amount      NUMERIC(12, 2) NOT NULL,
    status      VARCHAR(20)    NOT NULL,
    reason      VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    version     BIGINT         NOT NULL
);

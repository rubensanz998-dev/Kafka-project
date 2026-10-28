CREATE TABLE product (
    id      VARCHAR(100) PRIMARY KEY,
    stock   INTEGER      NOT NULL CHECK (stock >= 0),
    version BIGINT       NOT NULL DEFAULT 0
);

-- La PK sobre order_id hace de guardia de idempotencia: un pedido solo se reserva una vez.
-- Sin FK a product: también se guardan los rechazos de productos que no existen.
CREATE TABLE reservation (
    order_id   VARCHAR(36)  PRIMARY KEY,
    product_id VARCHAR(100) NOT NULL,
    quantity   INTEGER      NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    reason     VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version    BIGINT       NOT NULL
);

INSERT INTO product (id, stock) VALUES ('LAPTOP', 10), ('PHONE', 5), ('MOUSE', 100);

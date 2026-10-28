# Contexto del proyecto

Documento para quien vaya a trabajar en este repo: qué es, cómo está montado, qué reglas no hay que romper y en qué estado está. Para usarlo como demo, mira el [README](README.md).

## Propósito

Proyecto de portfolio para demostrar conocimientos de **Apache Kafka** en un escenario empresarial realista: una saga de pedidos (pedido → inventario → pago → notificación) con compensación, outbox, DDD, arquitectura hexagonal y OAuth2. Debe poder arrancarse con un solo `docker compose up --build` y probarse con el usuario demo `user123` / `pass123`.

## Stack

| Pieza | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.6 (Spring Kafka, Data JPA, Security OAuth2 Resource Server) |
| Kafka | 3.8 en modo KRaft (imagen `apache/kafka`) |
| PostgreSQL | 16, una base de datos por servicio (`orders`, `inventory`, `payments`) |
| Flyway | migraciones en `src/main/resources/db/migration` de cada servicio |
| Keycloak | 26, realm `shop` importado desde `docker/keycloak/shop-realm.json` |
| Tests | JUnit 5, AssertJ, Mockito, Spring Security Test, ArchUnit 1.3, H2 (modo PostgreSQL) |
| Build | Maven multi-módulo; un único `Dockerfile` multi-stage con `--build-arg SERVICE=<módulo>` |

## Módulos

| Módulo | Qué contiene |
|---|---|
| `common` | **Contratos públicos**: eventos de integración (`OrderCreated`, `InventoryResult`, `PaymentResult`, `OrderStatusChanged`), nombres de topics (`Topics`), `kafka-common.yml` y `KafkaCommonConfig` (creación de topics + DLT y reintentos) |
| `outbox` | Transactional Outbox reutilizable (`Outbox`, `OutboxPublisher`, tabla `outbox_event` en la migración `V0_1`), `PersistenceConfig` (escaneo de entidades y repositorios) y `persistence-common.yml` |
| `order-service` | API REST protegida con JWT y agregado `Order` |
| `inventory-service` | Agregados `Product` y `Reservation`, endpoint público `/stock` |
| `payment-service` | Agregado `Payment` y puerto `PaymentGateway` con un adaptador simulado |
| `notification-service` | Solo escucha `order.status` y escribe en el log. Sin base de datos ni hexagonal (no tiene lógica) |

## Convenciones de código

**Paquetes por servicio** (hexagonal):
- `domain`: Java puro. Agregados, value objects, eventos de dominio e interfaces de repositorio (puertos). **Prohibido** depender de Spring, `jakarta.*`, Kafka, `application` o `infrastructure`. Lo verifica `ArchitectureTest` (ArchUnit).
- `application`: casos de uso (`@Service @Transactional`) y puertos de salida (`*EventPublisher`, `PaymentGateway`). No conoce JPA, Kafka ni adaptadores.
- `infrastructure/{web,messaging,persistence,gateway}`: adaptadores. Las clases son package-private cuando nadie de fuera las necesita.

**Reglas de diseño:**
- Las reglas de negocio viven en los agregados (`Order.confirm()`, `Product.reserve()`, `Payment.authorize()`). Los servicios de aplicación solo orquestan: cargar → llamar al dominio → guardar → publicar.
- Las entidades JPA (`*JpaEntity`) están separadas del dominio. Se mapean con `copyFrom(domain)` y `toDomain()`.
- Los eventos de dominio (`OrderEvent`) se traducen a eventos de integración (`common.events`) **solo** en el adaptador `Outbox*EventPublisher`.
- Los comentarios explican el *porqué* y están en español, igual que los mensajes de log.

## Invariantes que no hay que romper

1. **Nunca publicar en Kafka directamente desde un caso de uso.** Siempre a través de `Outbox.save(...)`, que exige una transacción (`Propagation.MANDATORY`). Así el cambio de estado y el evento son atómicos.
2. **La clave de todos los mensajes es el `orderId`.** Garantiza el orden por pedido dentro de la partición.
3. **Todos los consumidores son idempotentes** (Kafka entrega *at-least-once*):
   - `order`: un pedido solo sale de `PENDING` una vez (el agregado devuelve `false` si ya está finalizado).
   - `inventory`: PK `reservation.order_id` y comprobación `exists` antes de reservar. `release`/`confirm` solo actúan si el estado es `RESERVED`.
   - `payment`: PK `payment.order_id` y comprobación `exists`. `@Version` fuerza un INSERT, de modo que un duplicado choca con la PK en vez de sobrescribir.
4. **Error de negocio ≠ error técnico.** Los rechazos de negocio se publican como eventos normales. Solo los fallos técnicos lanzan una excepción, y de ahí pasan a 2 reintentos y al DLT `<topic>-dlt`. `payment-service` consume `inventory.result-dlt` (`DeadLetterListener`, bytes en crudo para que un poison pill se registre y se salte) y, si el mensaje es `RESERVED`, publica `PaymentResult` FAILED para que la saga compense.
5. **Optimistic locking** (`@Version`) en `orders`, `product` y `reservation`: ante un conflicto se lanza una excepción y Kafka reintenta.
6. **El cliente de un pedido sale del JWT** (`preferred_username`), nunca del body. Cada usuario solo ve sus propios pedidos: si el pedido es de otro, se devuelve 404.

## Flujo de eventos

| Topic | Productor | Consumidores | Evento |
|---|---|---|---|
| `order.created` | order | inventory | `OrderCreated` |
| `inventory.result` | inventory | order (si REJECTED cancela), payment (si RESERVED cobra) | `InventoryResult` (incluye `customerId` y `amount` para el siguiente paso) |
| `payment.result` | payment | order (confirma o cancela), inventory (confirma la reserva o libera el stock) | `PaymentResult` |
| `order.status` | order | notification | `OrderStatusChanged` |

Todos los topics, DLT incluidos, se crean con 3 particiones desde `KafkaCommonConfig`.

## Entorno local

| Puerto | Servicio |
|---|---|
| 8081 | order-service (JWT) |
| 8082 | inventory-service (`/stock` público) |
| 8080 | Kafka UI |
| 8085 | Adminer (servidor `postgres`, `shop`/`shop`) |
| 8180 | Keycloak (admin `admin`/`admin`; usuarios demo `user123`/`pass123` y `boom`/`boom123`) |
| 5433 | PostgreSQL en el host (5433 para no chocar con un Postgres local) |
| 9092 | Kafka desde el host (`kafka:29092` dentro de Docker) |

**Keycloak y el issuer:** `KC_HOSTNAME=http://localhost:8180` hace que todos los tokens tengan `iss=http://localhost:8180/realms/shop`, se pidan desde el host o desde Docker. order-service valida ese issuer (`KEYCLOAK_ISSUER`) pero descarga las claves desde `KEYCLOAK_JWKS`, que dentro de Docker apunta a `keycloak:8080`.

**Variables de entorno de los servicios:** `KAFKA_BOOTSTRAP`, `DB_HOST`, `DB_USER`, `DB_PASSWORD`, `KEYCLOAK_ISSUER`, `KEYCLOAK_JWKS`. Todas tienen valores por defecto para ejecutar en local.

## Build y tests

```bash
mvn verify
```

- Necesita JDK 21. No necesita Docker: los tests usan el perfil `test` (`src/test/resources/application-test.yml`), con H2 en modo PostgreSQL, los listeners de Kafka desactivados y sin scheduler de la outbox. Flyway aplica las mismas migraciones que en producción.
- 41 tests: dominio puro, casos de uso con adaptadores reales, seguridad (MockMvc + `jwt()`), `OutboxPublisher` con `KafkaTemplate` mockeado y ArchUnit.
- Las migraciones `V1__*.sql` aún no se han publicado nunca, así que se pueden editar. **En cuanto haya un despliegue, los cambios de esquema van en nuevas `V2__...`.**

## Cómo extender

- **Nuevo evento:** record en `common/events`, constante en `Topics` (añádela a `Topics.ALL` para que se cree el topic con su DLT), traducción en el `Outbox*EventPublisher` del productor y `@KafkaListener` en el adaptador de mensajería del consumidor, que llama a un caso de uso.
- **Nuevo servicio con estado:** depende de `common` y `outbox`, usa `scanBasePackages = "com.ruben.shop"`, importa `kafka-common.yml` y `persistence-common.yml`, define `spring.datasource.url` y `group-id`, añade su base de datos a `docker/postgres-init.sql`, copia `ArchitectureTest` y `application-test.yml`, y añade el módulo al `pom.xml` raíz y al `docker-compose.yml`.

## Estado

**Verificado** (28/10/2026) con `docker compose up --build` (Docker 29.9, Compose 5.6) y también con Kafka 3.8, PostgreSQL 16 y Keycloak 26 en local sin Docker:
- Los 4 escenarios de la saga, con login real en Keycloak.
- 401 sin token o con token inválido, y cada usuario ve solo sus pedidos.
- La compensación devuelve el stock.
- El pedido del usuario `boom` llega al DLT y `payment-service` lo compensa: queda `CANCELLED` y el stock vuelve. Un poison pill en `inventory.result-dlt` se registra y se salta (sin crear `-dlt-dlt`).
- Outbox: con Kafka apagado el pedido se acepta y, al volver Kafka, se completa.
- Kafka UI (8080) y Adminer (8085) responden, y `notification-service` registra un "email" por cada cambio de estado.

**Pendiente / ideas:**
- Timeout general de la saga: un fallo técnico fuera del pago (p. ej. en `order-service`) aún deja el pedido `PENDING` en su DLT.
- Purga de `outbox_event`.
- CDC con Debezium en lugar de polling.
- Testcontainers.
- Prometheus + Grafana (consumer lag).
- Avro + Schema Registry.
- Frontend con Authorization Code + PKCE.

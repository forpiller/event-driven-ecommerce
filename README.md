# event-driven-ecommerce

Quattro microservizi Spring Boot (ordini, pagamenti, magazzino, notifiche) che comunicano esclusivamente via Kafka, dimostrando i pattern classici dell'architettura event-driven.

---

## Cosa dimostra

| Pattern | Problema risolto | Dove nel codice |
|---|---|---|
| **Outbox** | Garantisce che nessun evento vada perso anche in caso di crash post-DB | `ordini/.../service/OutboxRelay.java` |
| **Saga Choreography** | Rollback distribuito senza coordinatore centrale | `pagamenti/.../consumer/MerceConsumer.java`, `ordini/.../consumer/EventiProcessor.java` |
| **Idempotenza** | Stesso messaggio Kafka consegnato due volte → zero effetti doppi | `*/service/ProcessedEventInserter.java` |
| **Resilience4j** | Gateway bancario che fallisce → retry, poi circuit breaker aperto | `pagamenti/.../service/GatewayBancaClient.java` |

---

## Quick start

```bash
docker compose up --build -d
```

Dopo ~30 secondi, poi:

```bash
# Imposta stock disponibile
curl -X PUT http://localhost:8083/internal/stock \
  -H "Content-Type: application/json" \
  -d '{"prodottoId":"PROD-1","quantita":10}'

# Crea un ordine
curl -X POST http://localhost:8081/api/ordini \
  -H "Content-Type: application/json" \
  -d '{"prodottoId":"PROD-1","quantita":1,"importoTotale":29.99}'
```

Su **Kafka UI** su [http://localhost:8090](http://localhost:8090) si può osservare la catena di eventi sui topic.

---

## Architettura

```
POST /api/ordini
  └─► ordini salva ordine + outbox (stessa transazione)
        └─► outbox relay pubblica ordine.creato
              └─► pagamenti → chiama gateway banca (Resilience4j)
                    └─► pubblica pagamento.eseguito
                          └─► magazzino → scala merce
                                └─► pubblica merce.riservata
                                      └─► notifiche → email "confermato"

[merce non disponibile]
magazzino pubblica merce.non.disponibile
  └─► pagamenti → rimborsa → pubblica pagamento.rimborsato
        └─► ordini → stato CANCELLATO
              └─► notifiche → email "annullato"

[gateway fallisce]
pagamenti → retry (3x) → circuit breaker aperto → pubblica pagamento.fallito
  └─► ordini → stato FALLITO
        └─► notifiche → email "fallimento"
```

### Topic Kafka

| Topic | Produttore | Consumatori |
|---|---|---|
| `ordine.creato` | ordini | pagamenti |
| `pagamento.eseguito` | pagamenti | magazzino |
| `pagamento.fallito` | pagamenti | ordini, notifiche |
| `merce.riservata` | magazzino | ordini, notifiche |
| `merce.non.disponibile` | magazzino | pagamenti |
| `pagamento.rimborsato` | pagamenti | ordini, notifiche |

---

## Pattern implementati

### 1. Outbox Pattern

**Problema:** Pubblicare un evento Kafka dopo aver scritto nel DB — se il servizio crasha tra le due operazioni, l'evento va perso.

**Soluzione:** L'evento viene scritto nella tabella `outbox_events` nella stessa transazione dell'entità principale. Un `@Scheduled` relay legge e pubblica le righe non ancora pubblicate con `SELECT ... FOR UPDATE SKIP LOCKED` (sicuro per più istanze in parallelo).

**Nel codice:**
- `ordini/.../service/OrdineService.java` — salva ordine + outbox in `@Transactional`
- `ordini/.../service/OutboxRelay.java` — relay con query nativa SKIP LOCKED

### 2. Saga Choreography

**Problema:** Una transazione di business attraversa 4 DB diversi — non esiste una transazione ACID distribuita.

**Soluzione:** Ogni servizio ascolta eventi e reagisce pubblicandone di nuovi. Il rollback è un compensating event: `merce.non.disponibile` → `pagamento.rimborsato` → `ordine CANCELLATO`.

**Nel codice:**
- `pagamenti/.../consumer/MerceConsumer.java` — ascolta `merce.non.disponibile`, pubblica `pagamento.rimborsato`
- `ordini/.../consumer/EventiProcessor.java` — gestisce le transizioni di stato dell'ordine
- `ordini/.../service/OrdineService.java:aggiornaStato()` — state machine: solo transizioni valide (CONFERMATO non diventa CANCELLATO)

### 3. Idempotenza

**Problema:** Kafka garantisce "at-least-once" — lo stesso messaggio può arrivare due volte. Due addebiti per lo stesso ordine sarebbero un disastro.

**Soluzione:** Ogni consumer tiene una tabella `processed_events(event_id UUID PRIMARY KEY)`. Prima di processare: `INSERT ... ON CONFLICT DO NOTHING`. Se ritorna 0 righe, l'evento è già stato processato → skip. Nessuna eccezione, nessuna transazione annidata.

**Nel codice:**
- `*/service/ProcessedEventInserter.java` — `JdbcTemplate` + `ON CONFLICT DO NOTHING`

### 4. Resilience4j: Retry + Circuit Breaker

**Problema:** Il gateway bancario è un servizio esterno che può essere temporaneamente irraggiungibile.

**Soluzione:** `@Retry` (3 tentativi, backoff esponenziale) + `@CircuitBreaker` (si apre dopo 5 fallimenti consecutivi, attende 30s prima di ritentare). Se il CB è aperto, va direttamente al fallback che pubblica `pagamento.fallito`.

**Nel codice:**
- `pagamenti/.../service/GatewayBancaClient.java` — `@CircuitBreaker` + `@Retry`
- `pagamenti/.../service/GatewayBancaSimulator.java` — endpoint `/internal/gateway?fail=true` per simulare un fallimento

---

## Scenari di guasto

### Scenario 1: Merce finita

```bash
# Azzera lo stock
curl -X PUT http://localhost:8083/internal/stock \
  -H "Content-Type: application/json" \
  -d '{"prodottoId":"PROD-1","quantita":0}'

# Crea un ordine per PROD-1
curl -X POST http://localhost:8081/api/ordini \
  -H "Content-Type: application/json" \
  -d '{"prodottoId":"PROD-1","quantita":1,"importoTotale":9.99}'
```

**Sequenza attesa in Kafka UI:** `ordine.creato` → `pagamento.eseguito` → `merce.non.disponibile` → `pagamento.rimborsato`

L'ordine finisce in stato `CANCELLATO`.

### Scenario 2: Gateway banca che fallisce

```bash
# Forza il fallimento del gateway (3 retry, poi pagamento.fallito)
curl -X POST "http://localhost:8082/internal/gateway?fail=true"

# Oppure: modifica temporaneamente il docker-compose GATEWAY_URL con un host inesistente
```

**Sequenza attesa:** `ordine.creato` → 3 retry sul gateway → `pagamento.fallito`

L'ordine finisce in stato `FALLITO`. Il circuit breaker si apre dopo 5 fallimenti consecutivi.

### Scenario 3: Messaggio duplicato (idempotenza)

```bash
# Trova un messaggio su ordine.creato nella Kafka UI (http://localhost:8090)
# Copia il value JSON e re-pubblicalo sullo stesso topic
# Verifica nei log di pagamenti: "Evento XXX già processato, skip"
```

Nessun doppio addebito. L'`event_id` è già in `processed_events`.

### Scenario 4: Magazzino temporaneamente giù

```bash
docker compose stop magazzino-service

# Crea un ordine — il pagamento avverrà ma merce.riservata non arriverà
curl -X POST http://localhost:8081/api/ordini \
  -H "Content-Type: application/json" \
  -d '{"prodottoId":"PROD-1","quantita":1,"importoTotale":29.99}'

# Riavvia magazzino — Kafka ha accumulato i messaggi
docker compose start magazzino-service
# Osserva nei log: magazzino legge i messaggi pendenti e li processa
```

---

## Stack tecnico

- Java 17 (Amazon Corretto), Spring Boot 3.3.5
- Apache Kafka (via `spring-kafka`)
- PostgreSQL 15 (un'istanza per servizio)
- Flyway 10.x per le migrazioni DB
- Resilience4j 2.x (`resilience4j-spring-boot3`)
- Testcontainers 1.21.x + Awaitility (test E2E in `integration-tests/`)
- Lombok, Jackson
- Docker Compose + Kafka UI (provectus/kafka-ui)

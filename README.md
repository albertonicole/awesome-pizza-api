# Awesome Pizza API

Sistema di gestione ordini per la pizzeria **Awesome Pizza**. API REST per la creazione e gestione degli ordini, con coda di lavorazione per il pizzaiolo.

---

## Indice

- [Panoramica](#panoramica)
  - [Descrizione](#descrizione)
  - [Funzionalità Principali](#funzionalità-principali)
  - [Attori del Sistema](#attori-del-sistema)
  - [Stati dell'Ordine](#stati-dellordine)
- [Requisiti di Sistema](#requisiti-di-sistema)
  - [Prerequisiti Obbligatori](#prerequisiti-obbligatori)
- [Installazione e Avvio](#installazione-e-avvio)
  - [1. Clonazione Repository](#1-clonazione-repository)
  - [2. Avvio Database con Docker Compose](#2-avvio-database-con-docker-compose)
  - [3. Build del Progetto](#3-build-del-progetto)
  - [4. Avvio dell'Applicazione](#4-avvio-dellapplicazione)
- [API Reference](#api-reference)
- [Flusso Operativo Completo](#flusso-operativo-completo)
- [Gestione Concorrenza](#gestione-concorrenza)
   - [Requisito](#requisito)
   - [Problema](#problema)
   - [Soluzione](#soluzione)
   - [Perchè funziona](#perché-funziona)
- [Schema Database](#schema-database)
  - [Diagramma Entity-Relationship](#diagramma-entity-relationship)
  - [Tabella: orders](#tabella-orders)
  - [Tabella: order_items](#tabella-order_items)
  - [Indici](#indici)
- [Testing](#testing)
  - [Esecuzione Test](#esecuzione-test)
  - [Copertura Test](#copertura-test)
  - [Configurazione Test](#configurazione-test)

---

## Panoramica

### Descrizione

Awesome Pizza API è un backend REST che permette ai clienti di ordinare pizze senza registrazione e ai pizzaioli di gestire la coda degli ordini. Il sistema implementa una logica FIFO (First In, First Out) per garantire che gli ordini vengano evasi nell'ordine di arrivo.

### Funzionalità Principali

- **Creazione ordini** - I clienti possono creare ordini con una o più pizze
- **Tracking ordini** - I clienti possono seguire lo stato del proprio ordine tramite codice univoco
- **Coda ordini** - Il pizzaiolo visualizza tutti gli ordini in attesa
- **Gestione lavorazione** - Il pizzaiolo prende in carico un ordine alla volta (FIFO)
- **Completamento ordini** - Il pizzaiolo segna l'ordine come completato quando la pizza è pronta

### Attori del Sistema

| Attore | Descrizione | Operazioni |
|--------|-------------|------------|
| **Cliente** | Utente che ordina pizze | Crea ordine, controlla stato |
| **Pizzaiolo** | Operatore che prepara le pizze | Visualizza coda, prende ordini, completa ordini |

### Stati dell'Ordine

```
┌─────────┐         ┌─────────────┐         ┌───────────┐
│ PENDING │ ──────▶ │ IN_PROGRESS │ ──────▶ │ COMPLETED │
└─────────┘         └─────────────┘         └───────────┘
   Cliente             Pizzaiolo              Pizzaiolo
   ordina           prende ordine          completa ordine
```

| Stato | Descrizione |
|-------|-------------|
| `PENDING` | Ordine ricevuto, in attesa di essere preso in carico |
| `IN_PROGRESS` | Ordine in preparazione dal pizzaiolo |
| `COMPLETED` | Pizza pronta, ordine completato |

## Requisiti di Sistema

### Prerequisiti Obbligatori

- **Java 17** o superiore
- **Maven 3.9+** (oppure usare il wrapper `./mvnw` incluso)
- **Docker** e **Docker Compose** (per il database PostgreSQL)

---

## Installazione e Avvio

### 1. Clonazione Repository

```bash
git clone https://github.com/your-username/awesome-pizza-api.git
cd awesome-pizza-api
```

### 2. Avvio Database con Docker Compose

Il progetto include un file `docker-compose.yml` per avviare PostgreSQL:

```bash
# Avvia il database in background
docker compose up -d

# Verifica che il container sia in esecuzione
docker compose ps

# Output atteso:
# NAME                    STATUS    PORTS
# awesome-pizza-api-db    Up        0.0.0.0:5437->5432/tcp
```

#### Dettagli Connessione Database

| Parametro | Valore |
|-----------|--------|
| Host | `localhost` |
| Porta | `5437` |
| Database | `awesome-pizza` |
| Username | `awesome-pizza` |
| Password | `awesome-pizza` |

### 3. Build del Progetto

```bash
# Build con Maven Wrapper (consigliato)
./mvnw clean install

# Oppure salta i test per build più veloce
./mvnw clean install -DskipTests
```

### 4. Avvio dell'Applicazione

```bash
# Avvia l'applicazione
./mvnw spring-boot:run
```

L'applicazione sarà disponibile su: **http://localhost:8080**


---

## API Reference

Consultare lo Swagger UI per la documentazione interattiva degli endpoint accessibile su: **http://localhost:8080/swagger-ui.html**

---

## Flusso Operativo Completo

### Scenario: Ordine dalla Creazione al Completamento

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           FLUSSO OPERATIVO                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  CLIENTE                                       PIZZAIOLO                        │
│  ───────                                       ─────────                        │
│                                                                                 │
│  1. Crea ordine ──────────────────────────▶ 2. Visualizza coda                  │
│     POST /api/orders                           GET /api/orders/queue            │
│     ↓                                          ↓                                │
│     Riceve orderCode                           Vede ordini PENDING              │
│     (es: 550e8400-e29b-41d4)                                                          │
│                                                                                 │
│  4. Controlla stato                         3. Prende ordine                    │
│     GET /api/orders/{code}/status              POST /api/orders/next            │
│     ↓                                          ↓                                │
│     Vede: IN_PROGRESS                          Ordine diventa IN_PROGRESS       │
│                                                                                 │
│                                             5. Prepara pizza...                 │
│                                                                                 │
│  7. Controlla stato ◀─────────────────────  6. Completa ordine                  │
│     GET /api/orders/{code}                     PUT /api/orders/{code}/complete  │
│     ↓                                          ↓                                │
│     Vede: COMPLETED                            Ordine diventa COMPLETED         │
│     Pizza pronta!                                                               │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```
---

## Gestione Concorrenza



### Requisito

Un solo ordine **IN_PROGRESS** può esistere in qualsiasi momento.

### Problema

Senza protezione, due chiamate concorrenti a `POST /api/orders/next` potrebbero mettere in stato IN_PROGRESS due ordini diversi contemporaneamente.

```
Thread A: Check IN_PROGRESS → vuoto ✓
Thread B: Check IN_PROGRESS → vuoto ✓  (entrambi passano!)
Thread A: Prende Order1, imposta IN_PROGRESS
Thread B: Prende Order2, imposta IN_PROGRESS  ← BUG: 2 ordini IN_PROGRESS!
```

### Soluzione

Invertire l'ordine delle operazioni:
1. **Prima** acquisire il lock pessimistico sul primo ordine PENDING
2. **Poi** verificare (double-check) che non esista un ordine IN_PROGRESS

```java
@Transactional
public OrderResponse takeNextOrder() {
   // STEP 1: Acquisisce lock pessimistico sul primo ordine PENDING (FIFO)
   // Questo serializza l'accesso: altri thread aspettano qui
   Order order = orderRepository.findFirstByStatusOrderByCreatedAtAsc(OrderStatus.PENDING)
           .orElseThrow(NoOrdersInQueueException::new);

   // STEP 2: Double-check - ora che abbiamo il lock, verifichiamo
   // Se un altro thread ha completato prima di noi, esiste già un IN_PROGRESS
   if (orderRepository.existsByStatus(OrderStatus.IN_PROGRESS)) {
      log.warn("Tentativo di prendere un ordine mentre un altro è già in lavorazione");
      throw new OrderAlreadyInProgressException();
   }

   // STEP 3: Sicuri di essere l'unico, procediamo
   order.setStatus(OrderStatus.IN_PROGRESS);
   Order savedOrder = orderRepository.save(order);
   log.info("Ordine {} preso in carico (PENDING -> IN_PROGRESS)", savedOrder.getOrderCode());
   return orderMapper.toOrderResponse(savedOrder);
}
```

### Perché Funziona

```
Tempo    Thread A                              Thread B
─────────────────────────────────────────────────────────────────────
T1       findFirst(PENDING) → Order1 (LOCK) ✓
T2                                             findFirst(PENDING) → WAIT...
T3       existsByStatus(IN_PROGRESS) → false ✓
T4       Set IN_PROGRESS, COMMIT
T5       Lock released
T6                                             Query rieseguita → Order2 (LOCK)
T7                                             existsByStatus(IN_PROGRESS) → true ❌
T8                                             Throw OrderAlreadyInProgressException
─────────────────────────────────────────────────────────────────────
Risultato: Solo UN ordine IN_PROGRESS ✓
```

- Il lock pessimistico su `findFirstByStatusOrderByCreatedAtAsc(PENDING)` serializza l'accesso
- Quando Thread B ottiene finalmente il lock, la sua query viene **rieseguita** dal database
- Il double-check con `existsByStatus(IN_PROGRESS)` protegge da race condition

---

## Schema Database

### Diagramma Entity-Relationship

```
┌─────────────────────────────┐       ┌─────────────────────────────┐
│          orders             │       │       order_items           │
├─────────────────────────────┤       ├─────────────────────────────┤
│ id            BIGSERIAL  PK │───┐   │ id            BIGSERIAL  PK │
│ order_code    VARCHAR   UNQ │   │   │ pizza_name    VARCHAR       │
│ customer_name VARCHAR       │   │   │ quantity      INTEGER       │
│ status        VARCHAR       │   └──▶│ order_id      BIGINT     FK │
│ created_at    TIMESTAMP     │       └─────────────────────────────┘
└─────────────────────────────┘
         │
         │ Relazione: 1 ordine → N items
         │ ON DELETE CASCADE
         ▼
```

### Tabella: orders

| Colonna | Tipo | Vincoli | Descrizione |
|---------|------|---------|-------------|
| `id` | `BIGSERIAL` | PRIMARY KEY | Identificativo univoco |
| `order_code` | `VARCHAR(255)` | NOT NULL, UNIQUE | Codice ordine (es: 550e8400-e29b-41d4-a716-446655440000) |
| `customer_name` | `VARCHAR(255)` | NOT NULL | Nome del cliente |
| `status` | `VARCHAR(50)` | NOT NULL, CHECK | Stato ordine (PENDING, IN_PROGRESS, COMPLETED) |
| `created_at` | `TIMESTAMP` | NOT NULL | Data/ora creazione |

### Tabella: order_items

| Colonna | Tipo | Vincoli | Descrizione |
|---------|------|---------|-------------|
| `id` | `BIGSERIAL` | PRIMARY KEY | Identificativo univoco |
| `pizza_name` | `VARCHAR(255)` | NOT NULL | Nome della pizza |
| `quantity` | `INTEGER` | NOT NULL, DEFAULT 1 | Quantità |
| `order_id` | `BIGINT` | NOT NULL, FOREIGN KEY | Riferimento all'ordine |

### Indici

| Nome Indice | Tabella | Colonna | Scopo                                        |
|-------------|---------|---------|----------------------------------------------|
| `idx_orders_status` | orders | status | Filtro per stato (coda PENDING)              |
| `idx_orders_created_at` | orders | created_at | Ordinamento FIFO                             |
| `idx_order_items_order_id` | order_items | order_id | Join efficienti                              |
| `idx_orders_status_created_at` | orders | status, created_at | Filtro per stato con ordinamento cronologico |
---


## Testing

### Esecuzione Test

```bash
# Esegui tutti i test
./mvnw test
```

### Copertura Test

| Classe Test | Descrizione |
|-------------|-------------|
| `OrderServiceTest` | Test del service con Mockito |
| `OrderServiceConcurrencyTest` | Test concorrenza con Testcontainers/PostgreSQL |
| `OrderRepositoryTest` | Test repository JPA con @DataJpaTest |
| `OrderControllerTest` | Test REST endpoints con @WebMvcTest |
| `GlobalExceptionHandlerTest` | Test exception handler globale |
| `OrderMapperTest` | Test MapStruct mapper |
| `AwesomePizzaApiApplicationTests` | Context load test |


### Configurazione Test

Il file `src/test/resources/application.properties` configura H2 per i test:

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.liquibase.enabled=false
spring.jpa.hibernate.ddl-auto=create-drop
```

---

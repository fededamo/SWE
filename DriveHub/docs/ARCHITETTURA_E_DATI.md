# Architettura e modello dei dati

## 1. Obiettivo architetturale

DriveHub adotta un'applicazione desktop JavaFX a strati. La presentazione
traduce eventi UI in richieste applicative; i servizi coordinano casi d'uso e
transazioni; il dominio protegge invarianti e transizioni; le porte DAO isolano
PostgreSQL. La direzione ordinaria è:

`presentation → business → domain`

`business → dao.interfaces ← dao.postgres`

Il dominio non dipende da JavaFX, JDBC o PostgreSQL. I controller non devono
contenere SQL né ricostruire regole di business.

## 2. Tecnologie e stato dell'evidenza

| Elemento | Scelta | Fonte/stato |
|---|---|---|
| Linguaggio | Java 21 | `pom.xml`, verificato staticamente |
| UI | JavaFX 21/FXML/CSS | `pom.xml` e risorse, verificato staticamente |
| Build/test | Maven, JUnit 5, Mockito, JaCoCo | `pom.xml`, verificato staticamente |
| Database | PostgreSQL 16, JDBC | Compose, configurazione e migration, da provare in esecuzione |
| Test DB | H2 disponibile per i test | `pom.xml`; compatibilità SQL da verificare |
| Configurazione | variabili d'ambiente e `.env.example` | verificato staticamente |

Le note del corso non impongono queste versioni: sono decisioni del progetto.

## 3. Package e responsabilità

| Package | Responsabilità | Non deve contenere |
|---|---|---|
| `domain.users` | `User`, `Role`, identità e autorizzazioni di base | JavaFX, SQL |
| `domain.vehicles` | catalogo, modello, veicolo, stock order | navigazione UI |
| `domain.rentals` | `Rental`, `TestDrive`, macchine a stati | query JDBC |
| `domain.sales` | `SaleOrder`, `PurchaseProposal`, `Payment`, `Discount` | controller |
| `domain.observer` | eventi/observer di inventario | accesso DB diretto |
| `business` | Auth, Catalog, Rental, TestDrive, Sales, Inventory, Pricing, Dashboard e Payment service | widget JavaFX, SQL letterale |
| `dao.interfaces` | porte DAO, factory e `UnitOfWork` | dipendenza da JavaFX |
| `dao.postgres` | adapter JDBC, bootstrap/migration, mapping | decisioni di flusso UI |
| `presentation.controller` | validazione sintattica, binding e invocazione servizi | invarianti persistenti |
| `presentation.navigation` | route e cambio scena/workspace | logica economica |
| `presentation.core` | contesto applicativo/sessione e modelli UI | SQL |

## 4. Pattern

- **Layered Architecture:** separa presentazione, coordinamento, dominio e dati.
- **Service Layer:** un servizio per area funzionale espone operazioni orientate
  ai casi d'uso.
- **DAO e Unit of Work:** le porte rendono sostituibile la persistenza; una
  unità di lavoro delimita commit e rollback multi-aggregato.
- **Factory/Composition Root:** la configurazione crea DAO, servizi e gateway
  una volta e li rende disponibili ai controller.
- **Observer:** gli eventi di inventario disaccoppiano variazioni del veicolo
  da reazioni secondarie; la sua utilità deve essere dimostrata da almeno un
  caso e non solo dalla presenza delle interfacce.
- **Gateway simulato:** il pagamento universitario emula successo/fallimento e
  non raccoglie dati finanziari reali.

## 5. Modello relazionale canonico

La seguente è la specifica cui migration e mapper devono allinearsi. I nomi
fisici possono variare solo mantenendo questa semantica.

| Tabella | Attributi principali | Vincoli essenziali |
|---|---|---|
| `users` | id, fiscal_code, first_name, last_name, email, phone, password_hash, role, manager_id, active, created_at | PK id; unici CF/email; role ammesso; manager solo per Salesman; FK ricorsiva |
| `brands` | id, name | nome unico |
| `vehicle_models` | id, brand_id, name, model_year | FK brand; identità modello unica |
| `vehicles` | id, plate, model_id, purpose, sale_price, daily_rental_rate, mileage, status, created_at | targa unica; prezzo pertinente al purpose; valori non negativi |
| `discounts` | id, name, vehicle_id, percentage, starts_on, ends_on, enabled | `0 < percentage ≤ 100`; periodo valido; più record non si cumulano |
| `rentals` | id, customer_id, salesman_id?, vehicle_id, starts_on, ends_on, total_price, status, timestamps | periodo e prezzo validi; assegnazione coerente con stato |
| `test_drives` | id, customer_id, salesman_id?, vehicle_id, scheduled_at, ends_at, status, created_at | slot unico per veicolo e Customer; `scheduled_at < ends_at`, durata massima quattro ore |
| `sale_orders` | id, customer_id, salesman_id?, vehicle_id, total_price, required_deposit, paid_amount, status, timestamps | importi validi; FK Customer/Salesman/Vehicle |
| `payments` | id, payer_id, rental_id?, sale_order_id?, purpose, method, amount, created_at, status, processor_reference?, failure_reason? | Rental XOR SaleOrder; purpose compatibile; importo positivo; risultato coerente |
| `purchase_proposals` | id, customer_id, vehicle_id, requested_amount, customer_notes, requested_at, salesman_id?, offered_amount?, offer_terms?, manager_id?, decision_reason?, status | attori, importi e dati di offerta/decisione coerenti con stato |
| `stock_orders` | id, manager_id, model_id, quantity, unit_cost, placed_on, status, received_on? | quantità/costo positivi; ricezione non anteriore all'ordine |

### Cardinalità

- un Brand identifica zero o più VehicleModel; ogni modello ha un Brand;
- un VehicleModel descrive zero o più Vehicle; ogni veicolo ha un modello;
- un Vehicle ha zero o più Discount e può comparire in più operazioni nel
  tempo, ma non in operazioni attive incompatibili;
- Customer e Vehicle hanno zero o più Rental/TestDrive/SaleOrder;
- un Salesman prende in carico zero o più Rental, TestDrive e proposte;
- un Manager supervisiona zero o più Salesman, decide proposte e crea ordini;
- ogni Payment riferisce esattamente una Rental o un SaleOrder mediante due FK
  opzionali protette da un vincolo XOR.

## 6. Macchine a stati

| Aggregato | Transizioni principali |
|---|---|
| TestDrive | `REQUESTED → CONFIRMED → IN_PROGRESS → COMPLETED`; cancellazione dove ammessa |
| Rental | `REQUESTED → ASSIGNED → CONFIRMED → ACTIVE → COMPLETED`; cancellazione dove ammessa |
| PurchaseProposal | `REQUESTED → OFFERED → APPROVED` oppure `REJECTED` |
| SaleOrder | `RESERVED → DEPOSIT_PAID` oppure `PAID → COMPLETED`; cancellazione dove ammessa |
| Payment | `PENDING → COMPLETED` oppure `FAILED` |
| StockOrder | `PLACED → CONFIRMED → RECEIVED`; cancellazione prima della ricezione |

Le migration SQL devono usare gli stessi literal degli enum. Questo controllo è
parte della checklist finale perché una versione intermedia dello schema usava
nomi precedenti (`SUBMITTED`, `PURCHASED`, ecc.).

## 7. Transazioni, concorrenza e query

Sono confini transazionali minimi: registrazione; conferma atomica di
TestDrive/Rental; pagamento più aggiornamento dell'operazione; revisione di una
proposta; creazione coordinata di modello e ordine stock. L'errore deve causare
rollback o una compensazione verificabile e non una conferma parziale.

Nella baseline statica esaminata, `ServiceUiGateway` crea Rental/SaleOrder,
invoca il pagamento in una seconda transazione e cancella l'operazione in una
terza se il pagamento è `FAILED`. Il comportamento è coerente a fine flusso, ma
introduce una finestra di crash tra i passi. È un limite esplicito (`A-13`), non
una atomicità distribuita dichiarata.

Le query critiche riguardano: catalogo filtrato; sovrapposizioni di noleggio;
slot test drive; prese in carico non assegnate; proposte `OFFERED`; dashboard.
Indici e piani vanno misurati su dati rappresentativi prima di dichiararne
l'efficacia. Gli adapter usano update condizionali compare-and-set sullo stato
atteso per veicoli, noleggi, test drive e proposte; zero righe aggiornate diventa
un conflitto applicativo. Non è usata una colonna di optimistic locking.

## 8. Diagrammi sorgente

I diagrammi PlantUML manuali sono in `docs/diagrams/`. Sono interpretazioni
della specifica, non reverse engineering automatico del codice, in accordo con
`NotesOnTheProjectWork_Jan2025.pdf`, p. 4.

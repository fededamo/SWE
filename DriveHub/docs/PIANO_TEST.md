# Piano di test

## 1. Obiettivo e criterio

Il piano verifica i requisiti dal punto di vista complementare di struttura,
comportamento e persistenza. L'oracolo non è la sola assenza di eccezioni:
ogni test controlla stato finale, scritture effettuate o negate, ownership e,
nei flussi concorrenti, il numero dei vincitori. La coverage orienta la review
ma non dimostra l'assenza di difetti.

## 2. Livelli e tecniche

| Livello | Tecnica | Confine e oracolo |
|---|---|---|
| Dominio e servizi | white-box unit test | invarianti, rami, transizioni e autorizzazioni con fake/mock di porte, clock e gateway |
| Funzionale | black-box attraverso API business | obiettivo dell'attore, flusso base e alternative osservando DTO/esito e stato persistito |
| DAO/schema | gray-box integration | mapping, query, CHECK/FK/UNIQUE, CAS, lock e rollback sapendo come è organizzato lo schema |
| Architettura/FXML | test strutturale e contract test | dipendenze ammesse, viste caricabili e handler esistenti |
| JavaFX | smoke end-to-end su stage reali | routing, validazione, dialog, doppio click, feedback e composizione JavaFX→PostgreSQL |
| Manuale | exploratory/UI | usabilità percepita, tastiera, accessibilità e ridimensionamento su desktop fisico |

H2 in modalità PostgreSQL è soltanto un doppio rapido. I comportamenti propri
di PostgreSQL sono verificati anche sul server 16 reale e non sono dedotti dai
test H2.

## 3. Catalogo funzionale e test eseguibili

Gli ID sono condivisi con requisiti, template e matrice. La colonna evidenza
indica il metodo o la classe che esercita effettivamente lo scenario.

| ID | Scenario/oracolo principale | Evidenza JUnit |
|---|---|---|
| FT-AUTH-01/02 | account valido; duplicati e dati invalidi senza scritture parziali | `AuthServiceTest`; `UserGoalsFunctionalTest#ftAuth02DuplicatesAndInvalidData` |
| FT-AUTH-03/04/05 | routing per ruolo; dinieghi indistinguibili; logout idempotente | `JavaFxSmokeTest#allRoutesLoadAndLogoutRemovesSession`; `UserGoalsFunctionalTest#ftAuth04LoginDenials`, `#ftAuth05SessionCleanup` |
| FT-CAT-01/02 | filtri combinati, vuoto e input non valido | `UserGoalsFunctionalTest#ftCat01CombinedFilters`; `JavaFxSmokeTest#emptyCatalogAndInvalidSelectionShowMessages` |
| FT-TD-01/02 | slot futuro libero; overlap/adiacenza e date errate | `UserGoalsFunctionalTest#ftTd01Slots`; `Postgres16ConstraintsTest#intervalCheckAndHalfOpenOverlapQuery` |
| FT-TD-03/04 | claim e lifecycle esclusivi; ownership e annullamento | `UserGoalsFunctionalTest#ftTd03WorkflowAndOwnership`, `#ftTd04Cancellation`; `PostgresConcurrencyTest#claimAndDecisionCas` |
| FT-RENT-01 | richiesta e pagamento approvato atomici | `CheckoutFunctionalTest#ftRent01AcceptedCheckout`; `JavaFxSmokeTest#confirmedRentalSubmitsOnceAndPreservesSuccess` |
| FT-RENT-02 | periodo invalido/overlap rifiutato, adiacenza ammessa | `CheckoutFunctionalTest#ftRent02Periods`; `PostgresConcurrencyTest#overlappingRentalRequests` |
| FT-RENT-03/04 | rifiuto registrato con pratica annullata; chiusura dialog senza scritture | `CheckoutFunctionalTest#ftRent03DeclinedCheckout`; `JavaFxSmokeTest#cancelledRentalAndReentrantClickNeverSubmit` |
| FT-RENT-05/06/08 | vista proprietaria, cancellazione e diniego cross-user/staff | `CheckoutFunctionalTest#ftRent05OwnershipAndCancellation`, `#ftRent07StaffWorkflow` |
| FT-RENT-07 | `ASSIGNED→CONFIRMED→ACTIVE→COMPLETED` e transizioni illegali | `CheckoutFunctionalTest#ftRent07StaffWorkflow`; `RentalWorkflowTest` |
| FT-SALE-01/02 | acconto esatto, saldo esatto e acquisto integrale con purpose coerente | `CheckoutFunctionalTest#ftSale01DepositBalanceDelivery`, `#ftSale03FullPurchaseOnce`; `SalesWorkflowTest#depositAndBalance` |
| FT-SALE-03 | rifiuto/race senza doppia vendita e rilascio del veicolo | `CheckoutFunctionalTest#ftSale03DeclinedDeposit`, `#ftSale03FullPurchaseOnce`; `PostgresConcurrencyTest#doubleSale` |
| FT-ACQ-01/02 | proposta cliente e rollback anche del catalogo appena creato | `UserGoalsFunctionalTest#ftAcq01RequestAndRollback`; `PurchaseProposalServiceTest#requestCreatesMissingBrandAndModel` |
| FT-ACQ-03..06 | offerta, approvazione/rifiuto, istante e decisione unica | `UserGoalsFunctionalTest#ftAcq03OfferAndBothDecisions`; `PurchaseProposalServiceTest#managerDecisionIsAtomic` |
| FT-INV-01/02 | evento solo dopo commit; prenotazioni bloccano transizioni incompatibili | `UserGoalsFunctionalTest#ftInv01CommittedObserver`, `#ftInv02CommittedBookingsBlockAvailabilityChanges`; `TransactionAndObserverTest` |
| FT-DASH-01/02 | aggregati noti, attività non inventate e ruolo corretto | `UserGoalsFunctionalTest#ftDash01KnownAggregates` |
| FT-STOCK-01/02 | ordine/costruzione modello atomici; input errato in rollback | `UserGoalsFunctionalTest#ftStock01CreationAndRollback`; `SalesWorkflowTest#stockOrderWorkflow` |
| FT-PRICE-01..03 | campo prezzo corretto; promozione sostituibile; invalidità in rollback | `UserGoalsFunctionalTest#ftPrice01PricingAndPromotion`; `PricingAndSecurityTest` |
| FT-PAY-01/02 | successo e rifiuto con stati terminali coerenti | `CheckoutFunctionalTest`; `JavaFxSmokeTest#confirmedRentalSubmitsOnceAndPreservesSuccess`, `#rentalRejectionRemainsVisible` |
| FT-PAY-03 | secondo addebito/saldo già coperto rifiutato | `CheckoutFunctionalTest#ftSale01DepositBalanceDelivery`, `#ftPay03DuplicateAndNonOwner`; `PostgresConcurrencyTest#doubleBalance` |
| FT-PAY-04/05 | preventivo scaduto prima del gateway; errore inatteso in rollback | `CheckoutFunctionalTest#ftPay04StaleQuote`, `#ftPay05GatewayExceptionRollback` |

I nomi abbreviati della tabella sono leggibili integralmente negli XML
Surefire conservati in `docs/evidence/full-stack/surefire-reports/`.

## 4. Persistenza, concorrenza e transazioni

Le prove PostgreSQL dedicate coprono:

- bootstrap da schema vuoto, cinque migration e seed idempotenti;
- corrispondenza degli undici enum Java con i literal dei CHECK;
- 22 foreign key, UNIQUE/CHECK, XOR dei riferimenti `Payment` e importi;
- range/overlap, query di ownership e round-trip di tutti i workflow;
- lock di riga, compare-and-set e indici unici parziali;
- competizioni su vendita, saldo, pagamento noleggio, slot, claim e decisione;
- rollback completo su errore SQL e su errore inatteso dopo il gateway simulato.

`DatabaseBootstrapIntegrationTest` e `PostgresDaoIntegrationTest` usano H2 e
rimangono test rapidi. `Postgres16ConstraintsTest`,
`Postgres16DaoIntegrationTest` e `PostgresConcurrencyTest` richiedono il tag
`postgres` e il container reale.

## 5. Comandi riproducibili

```bash
mvn clean test
bash scripts/test_postgres.sh
bash scripts/test_full_stack.sh
```

Il terzo comando costruisce un ambiente effimero con Temurin 21, Maven,
PostgreSQL 16 e Xvfb/GTK, esegue anche i test `gui` e `postgres`, produce gli
screenshot, archivia log/XML/JaCoCo e rimuove container e rete. Richiede Docker
e accesso iniziale alle immagini/dependency. Nessuna credenziale di produzione
è utilizzata.

## 6. Registro risultati dell'11 settembre 2026

| Ambiente | Test | Failure / error / skipped | Esito |
|---|---:|---:|---|
| OpenJDK host 25.0.4.1, suite standard con target 21 | 81 | 0 / 0 / 0 | `BUILD SUCCESS` |
| Temurin 21.0.9, suite standard | 81 | 0 / 0 / 0 | `BUILD SUCCESS` |
| Temurin 21 + PostgreSQL 16.15 | 101 | 0 / 0 / 0 | `BUILD SUCCESS` |
| Temurin 21 + Xvfb/GTK, 7 smoke JavaFX | 88 | 0 / 0 / 0 | `BUILD SUCCESS` |
| full stack: Temurin 21 + PostgreSQL 16.15 + 8 smoke JavaFX | **109** | **0 / 0 / 0** | **`BUILD SUCCESS`, 29,062 s Maven** |

Ripartizione full-stack: 6 architettura, 4 pricing/security, 4 auth service,
1 payment service, 2 purchase proposal service, 2 rental service, 3
transazione/Observer, 4 bootstrap H2, 7 constraint PostgreSQL, 5 DAO
PostgreSQL reali, 5 DAO H2, 8 concorrenza PostgreSQL, 4 rental domain, 4 sales
domain, 6 user/vehicle domain, 12 checkout funzionali, 20 user-goal
funzionali, 2 contratti FXML, 8 smoke JavaFX e 2 session context.

JaCoCo full-stack: 78,37% istruzioni, 62,74% branch, 80,77% linee, 80,49%
metodi e 94,00% classi. I contatori completi e il fingerprint della baseline
sono in `VERIFICA_FINALE.md` e `docs/evidence/full-stack/summary.json`.

## 7. Criteri di uscita

- build e package su JDK 21 senza test falliti;
- flusso base e alternative critiche di ogni UC significativo esercitati;
- migration applicabili da zero e vincoli/query verificati su PostgreSQL 16;
- transazioni e race critiche con oracolo esplicito;
- viste FXML caricabili e percorso JavaFX reale fino a PostgreSQL;
- diagrammi renderizzabili e ID coerenti tra requisiti, UC, matrice e test;
- nessun segreto o dato personale reale nelle evidenze.

I criteri tecnici risultano soddisfatti nella baseline identificata in
`VERIFICA_FINALE.md`. Resta deliberatamente manuale la valutazione percettiva
su desktop fisico (accessibilità, tastiera e dimensioni di monitor diverse).

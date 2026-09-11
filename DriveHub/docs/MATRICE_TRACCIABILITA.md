# Matrice di tracciabilità

## 1. Regole di lettura

La catena è: requisito → caso d'uso/passo → pagina/controller → servizio →
dominio/porta → tabella → test. `V` significa che il collegamento è stato
verificato sulla baseline e che il relativo test è presente nell'esecuzione
full-stack; non significa validazione delle decisioni commerciali da parte del
docente. Le alternative sono identificate nel template con il passo di origine
(per esempio `4a`).

## 2. Tracciabilità end-to-end

| Requisito / UC e passi | Pagina e controller | Servizio | Dominio / DAO e tabella | Evidenza eseguita | Stato |
|---|---|---|---|---|---|
| RF-AUTH-01 / UC-AUTH-01, 1–7 e alt. 2a–5a | P-02 `RegistrationController` | `AuthService` | `User` / `UserDao` → `users` | `UserGoalsFunctionalTest#ftAuth01RegisterAndLogin`, `#ftAuth02DuplicatesAndInvalidData`; `AuthServiceTest` | V |
| RF-AUTH-02 / UC-AUTH-02, 1–5 e alt. 2a–4a | P-01 `LoginController`, `NavigationManager` | `AuthService` | `User` / `UserDao` → `users` | `UserGoalsFunctionalTest#ftAuth04LoginDenials`; `JavaFxSmokeTest#allRoutesLoadAndLogoutRemovesSession` | V |
| RF-AUTH-03 / UC-AUTH-03, 1–3 | P-10/P-20/P-30, `NavigationManager` | `SessionContext` | sessione in memoria, nessuna tabella | `UserGoalsFunctionalTest#ftAuth05SessionCleanup`; `SessionContextTest`; smoke routing JavaFX | V |
| RF-CAT-01 / UC-CAT-01, 1–5 e alt. 2a/3a/4a | P-10 `CustomerWorkspaceController` | `CatalogService` | `Brand`, `VehicleModel`, `Vehicle`, `Discount` / relative porte → `brands`, `vehicle_models`, `vehicles`, `discounts` | `UserGoalsFunctionalTest#ftCat01CombinedFilters`; `Postgres16DaoIntegrationTest`; smoke stato vuoto | V |
| RF-TD-01 / UC-TD-01, 1–5 e alt. 3a/4a | P-10/P-11 `CustomerWorkspaceController` | `TestDriveService` | `TestDrive` / `TestDriveDao` → `test_drives` | `UserGoalsFunctionalTest#ftTd01Slots`; `Postgres16ConstraintsTest#intervalCheckAndHalfOpenOverlapQuery`; `PostgresConcurrencyTest#sameCustomerDifferentVehicles` | V |
| RF-RENT-01 + RF-PAY-01 / UC-RENT-01, 1–9 e alt. 3a/4a/6a/8a | P-10/P-12/P-40 `CustomerWorkspaceController`, `PaymentDialogController` | `CheckoutService`, `RentalService`, `PaymentService`, `PricingService` | `Rental`, `Payment`, `Vehicle` / `RentalDao`, `PaymentDao`, `VehicleDao` → `rentals`, `payments`, `vehicles` | `CheckoutFunctionalTest#ftRent01AcceptedCheckout`, `#ftRent02Periods`, `#ftRent03DeclinedCheckout`; smoke dialog conferma/annulla/rifiuto; `PostgresConcurrencyTest#persistenceFailureAfterApproval` | V |
| RF-RENT-02 / UC-RENT-02, 1–5 e alt. 1a/3a/4a | P-13 `CustomerWorkspaceController` | `RentalService` | `Rental` / `RentalDao` → `rentals` | `CheckoutFunctionalTest#ftRent05OwnershipAndCancellation`, `#ftPay03DuplicateAndNonOwner` | V |
| RF-SALE-01 + RF-PAY-01 / UC-SALE-01, 1–7 e alt. 3a/4a/6a | P-10/P-40 `CustomerWorkspaceController`, `PaymentDialogController` | `CheckoutService`, `SalesService`, `PaymentService`, `PricingService` | `SaleOrder`, `Payment`, `Vehicle`, `Discount` / relative porte → `sale_orders`, `payments`, `vehicles`, `discounts` | `CheckoutFunctionalTest#ftSale01DepositBalanceDelivery`, `#ftSale03DeclinedDeposit`, `#ftSale03FullPurchaseOnce`; `PostgresConcurrencyTest#doubleSale`, `#doubleBalance`; smoke vendita/dialog | V |
| RF-ACQ-01 / UC-ACQ-01, 1–5 e alt. 2a/3a | P-14 `CustomerWorkspaceController` | `PurchaseProposalService` | `Vehicle`, `PurchaseProposal` / relative porte → `vehicles`, `purchase_proposals` | `UserGoalsFunctionalTest#ftAcq01RequestAndRollback`; `PurchaseProposalServiceTest#requestCreatesMissingBrandAndModel` | V |
| RF-TD-02 / UC-TD-02, 1–5 e alt. 1a/2a/3a/5a | P-20 `SalesmanWorkspaceController` | `TestDriveService` | `TestDrive`, `Vehicle` / `TestDriveDao`, `VehicleDao` → `test_drives`, `vehicles` | `UserGoalsFunctionalTest#ftTd03WorkflowAndOwnership`, `#ftTd04Cancellation`; `PostgresConcurrencyTest#claimAndDecisionCas` | V |
| RF-RENT-03 / UC-RENT-03, 1–6 e alt. 1a/2a/4a | P-21 `SalesmanWorkspaceController` | `RentalService` | `Rental`, `Vehicle` / `RentalDao`, `VehicleDao` → `rentals`, `vehicles` | `CheckoutFunctionalTest#ftRent07StaffWorkflow`; `RentalServiceTest`; `PostgresConcurrencyTest#claimAndDecisionCas` | V |
| RF-ACQ-02 / UC-ACQ-02, 1–6 e alt. 1a/3a/4a | P-22 `SalesmanWorkspaceController` | `PurchaseProposalService` | `PurchaseProposal` / `PurchaseProposalDao` → `purchase_proposals` | `UserGoalsFunctionalTest#ftAcq03OfferAndBothDecisions`; `Postgres16ConstraintsTest#proposalDecisionTimestampSurvivesReloadAndSecondDecisionLoses` | V |
| RF-INV-01 / UC-INV-01, 1–5 e alt. 1a/3a/4a | P-23 `SalesmanWorkspaceController` | `InventoryService`; `InventoryActivityFeed` Observer | `Vehicle` / `VehicleDao` → `vehicles` | `UserGoalsFunctionalTest#ftInv01CommittedObserver`, `#ftInv02CommittedBookingsBlockAvailabilityChanges`; `TransactionAndObserverTest` | V |
| RF-DASH-01 / UC-DASH-01, 1–4 e alt. 1a/2a | P-30 `ManagerWorkspaceController` | `DashboardService` | proiezioni su inventario, noleggi, pagamenti e proposte / relative porte | `UserGoalsFunctionalTest#ftDash01KnownAggregates`; smoke workspace Manager | V |
| RF-STOCK-01 / UC-STOCK-01, 1–4 e alt. 1a/2a/3a | P-31 `ManagerWorkspaceController` | `InventoryService` | `Brand`, `VehicleModel`, `StockOrder` / relative porte → `brands`, `vehicle_models`, `stock_orders` | `UserGoalsFunctionalTest#ftStock01CreationAndRollback`; `SalesWorkflowTest#stockOrderWorkflow` | V |
| RF-PRICE-01 / UC-PRICE-01, 1–6 e alt. 2a/3a/5a/6a | P-32 `ManagerWorkspaceController` | `PricingService`, `PricingStrategy` | `Vehicle`, `Discount` / `VehicleDao`, `DiscountDao` → `vehicles`, `discounts` | `UserGoalsFunctionalTest#ftPrice01PricingAndPromotion`; `PricingAndSecurityTest` | V |
| RF-ACQ-03 / UC-ACQ-03, 1–6 e alt. 1a/2a/3a/4a | P-33 `ManagerWorkspaceController` | `PurchaseProposalService` | `PurchaseProposal` / `PurchaseProposalDao` → `purchase_proposals` | `UserGoalsFunctionalTest#ftAcq03OfferAndBothDecisions`; `PurchaseProposalServiceTest#managerDecisionIsAtomic`; concorrenza CAS | V |
| RF-PAY-01 / UC-PAY-01, 1–6 e alt. 2a/3a/4a/5a | P-40 `PaymentDialogController`, invocato da `CustomerWorkspaceController` | `CheckoutService`, `PaymentService`, `PaymentGateway` | `Payment`, `Rental`, `SaleOrder` / relative porte → `payments`, `rentals`, `sale_orders` | `CheckoutFunctionalTest` (successo, decline, duplicato, quote scaduto, eccezione); `JavaFxSmokeTest`; `PostgresConcurrencyTest` | V |

## 3. Requisiti trasversali

| Requisito | Implementazione verificata | Evidenza |
|---|---|---|
| RF-ERR-01 | eccezioni business tradotte in messaggi correggibili, nessuno stack trace nei dialog | test funzionali delle alternative e smoke JavaFX |
| RNF-SEC-01 | PBKDF2 con salt; configurazione DB da ambiente; nessun plaintext persistito | `PricingAndSecurityTest`; review seed/config/evidenze |
| RNF-SEC-02 | ruolo e ownership controllati nei servizi, non soltanto dalla UI | `UserGoalsFunctionalTest#ftAuthz01RoleMatrix` e test cross-user |
| RNF-DATA-01 | invarianti dominio più vincoli SQL | test dominio; sette test `Postgres16ConstraintsTest` |
| RNF-DATA-02 | `TransactionRunner`/`UnitOfWork`; checkout e pagamento nello stesso confine | `TransactionAndObserverTest`; test rollback H2/PostgreSQL; sequenze |
| RNF-DATA-03 | lock di riga, CAS e indici unici parziali | otto `PostgresConcurrencyTest` |
| RNF-QUAL-01 | dipendenze concentriche e composition root | sei `LayeringTest`; package diagram |
| RNF-QUAL-02 | porte DAO, gateway e clock sostituibili | unit test con mock/fake; adapter PostgreSQL separato |
| RNF-UX-01 | successo, rifiuto e annullamento distinti; blocco re-entrancy | otto `JavaFxSmokeTest`; screenshot reali |
| RNF-PORT-01 | bytecode/JDK 21, Compose PostgreSQL 16 e script full-stack | 109 test full-stack; manifest/evidenze |
| RNF-PRIV-01 | seed fittizio, log sanitizzato, nessun dato di pagamento reale | review SQL; recorder delle evidenze |
| RNF-DOC-01 | ID e risultati condivisi fra documenti | questa matrice, `PIANO_TEST.md`, `VERIFICA_FINALE.md` |

## 4. Baseline dell'evidenza

L'11 settembre 2026 `bash scripts/test_full_stack.sh` ha eseguito 109 test con
0 failure, 0 error e 0 skipped su Temurin 21.0.9, PostgreSQL 16.15 e JavaFX
reale sotto Xvfb. Il commit base è `ed03c32289aa663dfc04b1bf0c8fda15da326b47`;
le modifiche non committate verificate sono identificate dal manifest SHA-256
`e7fb29af5332d2abf13939c12de0462b99c58573cc722cfceb78dba911aeec25`.
Log, XML Surefire e coverage sono in `docs/evidence/full-stack/`.

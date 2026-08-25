# Matrice di tracciabilità

## 1. Regole di lettura

La matrice collega gli obiettivi utente ai punti di ingresso UI, ai servizi
applicativi, agli aggregati persistiti e ai test pianificati. Il collegamento è
una specifica verificabile: non dimostra, da solo, che il percorso sia completo
o che il test sia stato eseguito.

| Stato | Significato |
|---|---|
| `S` | specificato in requisiti e caso d'uso |
| `I` | riferimento individuato nei sorgenti finali |
| `T` | scenario pianificato; la copertura JUnit effettiva è riepilogata sotto |

## 2. UC → UI → servizio → dati → test

| Caso d'uso | Requisito | Pagina/controller | Servizio applicativo | Entità/porta DAO | Test pianificati | Stato |
|---|---|---|---|---|---|---|
| UC-AUTH-01 | RF-AUTH-01 | P-02 `RegistrationController` | `AuthService` | `User`, `UserDao` | FT-AUTH-01/02; UT-AUTH-01; IT-USER-01 | S/I/T |
| UC-AUTH-02 | RF-AUTH-02 | P-01 `LoginController` | `AuthService` | `User`, `UserDao` | FT-AUTH-03/04; UT-AUTH-02 | S/I/T |
| UC-AUTH-03 | RF-AUTH-03 | workspace di ruolo, `NavigationManager` | sessione/UI | `User` (nessuna scrittura richiesta) | FT-AUTH-05; UT-NAV-01 | S/I/T |
| UC-CAT-01 | RF-CAT-01 | P-10 `CustomerWorkspaceController` | `CatalogService` | `Brand`, `VehicleModel`, `Vehicle`, `Discount`; relative DAO | FT-CAT-01/02; UT-CAT-01; IT-VEHICLE-01 | S/I/T |
| UC-TD-01 | RF-TD-01 | P-11 `CustomerWorkspaceController` | `TestDriveService` | `TestDrive`, `User`, `Vehicle`; `TestDriveDao` | FT-TD-01/02; UT-TD-01; IT-TD-01 | S/I/T |
| UC-RENT-01 | RF-RENT-01, RF-PAY-01 | P-12/P-40 `CustomerWorkspaceController` (`PaymentDialogController` non ancora cablato) | `RentalService`, `PaymentService` | `Rental`, `Payment`, `Vehicle`; relative DAO | FT-RENT-01..04; UT-RENT-01..03; IT-RENT-01; IT-PAY-01 | S/I/T |
| UC-RENT-02 | RF-RENT-02 | P-13 `CustomerWorkspaceController` | `RentalService` | `Rental`; `RentalDao` | FT-RENT-05/06; UT-RENT-04; IT-RENT-02 | S/I/T |
| UC-SALE-01 | RF-SALE-01, RF-PAY-01 | P-10/P-40 `CustomerWorkspaceController` (`PaymentDialogController` non ancora cablato) | `SalesService`, `PaymentService`, `PricingService` | `SaleOrder`, `Payment`, `Vehicle`, `Discount`; relative DAO | FT-SALE-01..03; UT-SALE-01..03; IT-SALE-01; IT-PAY-02 | S/I/T |
| UC-ACQ-01 | RF-ACQ-01 | P-14 `CustomerWorkspaceController` | `PurchaseProposalService` | `Vehicle`, `PurchaseProposal`; relative DAO | FT-ACQ-01/02; UT-SALES-01; IT-PROPOSAL-01 | S/I/T |
| UC-TD-02 | RF-TD-02 | P-20 `SalesmanWorkspaceController` | `TestDriveService` | `TestDrive`; `TestDriveDao` | FT-TD-03/04; UT-TD-02; IT-TD-02 | S/I/T |
| UC-RENT-03 | RF-RENT-03 | P-21 `SalesmanWorkspaceController` | `RentalService` | `Rental`, `Vehicle`; `RentalDao`, `VehicleDao` | FT-RENT-07/08; UT-RENT-05; IT-RENT-03 | S/I/T |
| UC-ACQ-02 | RF-ACQ-02 | P-22 `SalesmanWorkspaceController` | `PurchaseProposalService` | `PurchaseProposal`, `Vehicle`; relative DAO | FT-ACQ-03/04; UT-SALES-02; IT-PROPOSAL-02 | S/I/T |
| UC-INV-01 | RF-INV-01 | P-23 `SalesmanWorkspaceController` | `InventoryService` | `Vehicle`; `VehicleDao`; observer inventario | FT-INV-01/02; UT-INV-01; IT-VEHICLE-02 | S/I/T |
| UC-DASH-01 | RF-DASH-01 | P-30 `ManagerWorkspaceController` | `DashboardService` | viste/query su veicoli, noleggi, pagamenti, proposte | FT-DASH-01; UT-DASH-01; IT-DASH-01 | S/I/T |
| UC-STOCK-01 | RF-STOCK-01 | P-31 `ManagerWorkspaceController` | `InventoryService` | `Brand`, `VehicleModel`, `StockOrder`; relative DAO | FT-STOCK-01/02; UT-INV-02; IT-STOCK-01 | S/I/T |
| UC-PRICE-01 | RF-PRICE-01 | P-32 `ManagerWorkspaceController` | `PricingService` | `Vehicle`, `Discount`; relative DAO | FT-PRICE-01..03; UT-PRICE-01..03; IT-DISCOUNT-01 | S/I/T |
| UC-ACQ-03 | RF-ACQ-03 | P-33 `ManagerWorkspaceController` | `PurchaseProposalService` | `PurchaseProposal`; `PurchaseProposalDao` | FT-ACQ-05/06; UT-SALES-03; IT-PROPOSAL-03 | S/I/T |
| UC-PAY-01 | RF-PAY-01 | P-40 logica in `CustomerWorkspaceController`; `PaymentDialogController` disponibile ma non cablato | `PaymentService`, gateway simulato | `Payment`; `PaymentDao` | FT-PAY-01..03; UT-PAY-01..04; IT-PAY-01/02 | S/I/T |

I nomi dei servizi e controller corrispondono ai sorgenti della baseline finale.

### Evidenza di esecuzione

La suite `mvn -o clean test` del 23 agosto 2026 ha eseguito 43 test con 0
failure/error/skipped. Le classi effettive e la ripartizione sono riportate in
`PIANO_TEST.md`; gli ID `FT/UT/IT` restano il catalogo di copertura desiderata e
non implicano una corrispondenza uno-a-uno con i nomi dei metodi JUnit.

## 3. Requisiti trasversali

| Requisito | Evidenza progettuale | Verifica pianificata |
|---|---|---|
| RF-ERR-01 | alternative di ogni UC e gestione errori dei controller | FT-ERR-01; review messaggi UI |
| RNF-SEC-01 | hashing nel servizio autenticazione; password da ambiente | UT-AUTH-01; review log/config |
| RNF-SEC-02 | `Role` e controlli nei servizi/domain | UT-AUTHZ-01..03 |
| RNF-DATA-01 | entità con invarianti e constraint SQL | UT-DOM-*; IT-CONSTRAINT-* |
| RNF-DATA-02 | `UnitOfWork` | IT-TX-01/02 |
| RNF-DATA-03 | update CAS sullo stato atteso e conflitto su zero righe | IT-CONC-01..03 |
| RNF-QUAL-01 | package diagram e dipendenze | review ARCH-01 |
| RNF-QUAL-02 | porte DAO e gateway sostituibile | unit test con fake/mock |
| RNF-UX-01 | esiti success/failure/cancel in navigazione | FT-NAV-01; test manuali UI |
| RNF-PORT-01 | `pom.xml`, Compose e `.env.example` | build su ambiente pulito |
| RNF-PRIV-01 | seed e pagamenti dimostrativi | review dati/log |
| RNF-DOC-01 | ID condivisi in questi documenti | review DOC-01 |

## 4. Aggiornamento della matrice

Alla chiusura del progetto, sostituire `I/T` con collegamenti a metodi e classi
di test reali, riportare commit e data, ed eliminare solo le righe realmente
fuori scope motivandole nella relazione.

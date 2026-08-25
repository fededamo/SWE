<div align="center">

# DriveHub

## Analisi, progetto e realizzazione di un sistema informativo per concessionario

**Corso di Ingegneria del Software**  
**Anno Accademico 2025/2026**

Docente: **Prof. Enrico Vicario**

Autore/i: **[DA INSERIRE]**  
Matricola/e: **[DA INSERIRE]**

Versione relazione: **bozza tecnica — 23 agosto 2026**  
Commit di consegna: **[DA INSERIRE]**

</div>

---

## Indice

1. Dichiarazione e stato della relazione
2. Obiettivo e metodo
3. Fonti e requisiti didattici
4. Analisi del dominio
5. Requisiti e casi d'uso
6. Interfaccia e navigazione
7. Architettura
8. Progettazione di dettaglio
9. Persistenza
10. Scenari dinamici
11. Implementazione e configurazione
12. Strategia di test
13. Uso dell'intelligenza artificiale
14. Assunzioni, conflitti e limiti
15. Conclusioni e checklist

## 1. Dichiarazione e stato della relazione

### 1.1 Statement da completare prima della consegna

> Gli autori indicati nel frontespizio dichiarano di conoscere e comprendere
> l'intero elaborato, di avere indicato le fonti e gli strumenti di supporto
> utilizzati e di essere in grado di motivare requisiti, scelte progettuali,
> implementazione e test.

Sottoscrizione/autoverifica: **[DA COMPLETARE DAGLI AUTORI]**.

Questa formulazione risponde alla sezione “Statement” richiesta nelle note di
progetto (p. 11), ma non costituisce una dichiarazione firmata finché autori e
matricole non sono inseriti.

### 1.2 Stato delle evidenze

La relazione descrive una baseline progettuale e un repository in evoluzione.
Le scelte tecniche sono state riscontrate staticamente nei file presenti; build,
test, coverage e prova end-to-end non sono dichiarati eseguiti. I relativi
campi restano **DA AGGIORNARE**. I diagrammi sono sorgenti PlantUML redatti
manualmente dalla specifica, non prodotti mediante reverse engineering.

## 2. Obiettivo e metodo

DriveHub supporta le attività essenziali di un concessionario: consultazione e
gestione del parco veicoli, test drive, noleggi, prenotazione/acquisto, proposta
di veicoli da parte dei clienti, ordini di stock, prezzi, promozioni, pagamenti
dimostrativi e indicatori direzionali.

Il lavoro ha seguito quattro passi:

1. inventario in sola lettura delle fonti e individuazione dei conflitti;
2. normalizzazione del vocabolario e dei flussi significativi;
3. tracciamento requisito → caso d'uso → UI → servizio → dati → test;
4. progettazione a strati con dominio ricco e persistenza sostituibile.

Il progetto ChargeNet dei colleghi è stato usato esclusivamente come riferimento
di metodo e profondità documentale. Non sono state trasferite regole proprie
della ricarica elettrica. La matrice di trasformazione è riportata in
[`ANALISI_PRELIMINARE.md`](../docs/ANALISI_PRELIMINARE.md).

## 3. Fonti e requisiti didattici

### 3.1 Gerarchia delle fonti

In caso di incompatibilità si è adottato questo ordine: note specifiche di
progetto; diagrammi DriveHub; materiale ufficiale del corso; ChargeNet come
esempio; best practice generali. Le assunzioni sono dichiarate e non spacciate
per istruzioni originarie.

### 3.2 Fonti principali

| Fonte | Impiego |
|---|---|
| `NotesOnTheProjectWork_Jan2025.pdf`, 47 pp., v1.1 | struttura relazione, template UC, design dati, test e AI |
| `Users SWE.drawio` | attori e obiettivi utente |
| `Page Navigation Diagram.drawio` | routing e workspace |
| `Package diagram.drawio` | separazione presentation/business/domain/DAO |
| `SWE ER.drawio` | concetti e relazioni dati, dopo normalizzazione |
| `Login UI.png`, `Customer UI.png`, `Employee UI.png` | aspetto e contenuto di pagine esemplificative |
| `ChargeNet-master` | metodo, struttura e categorie di test |

### 3.3 Prescrizioni applicate

Le note chiedono frontespizio/indice (p. 10), statement (p. 11), package e
tecnologie (p. 12), template completi dei casi d'uso (pp. 13 e 22), ER e modello
relazionale (pp. 14, 30 e 45), class diagram per package (p. 35), test
funzionali tracciati e test su dominio/DAO/query (pp. 26–29) e documentazione
dell'AI (p. 4). Mockup e navigazione sono raccomandati, non sostituiscono i
template. Il PDF esaminato appartiene all'A.A. 2024/2025 ed è marcato come
bozza; in assenza di un disciplinare più recente è stato usato come riferimento,
mentre il frontespizio segue l'A.A. 2025/2026 richiesto.

## 4. Analisi del dominio

### 4.1 Attori

- **Customer:** consulta il catalogo, richiede test drive e noleggi, effettua
  pagamenti dimostrativi, riserva/acquista e propone un proprio veicolo.
- **Salesman:** prende in carico test drive e noleggi, gestisce inventario e
  formula offerte per acquisire i veicoli proposti.
- **Manager:** consulta la dashboard, ordina stock, gestisce pricing/promozioni
  e decide sulle offerte del Salesman.
- **User:** identità comune astratta ai tre ruoli, non attore operativo aggiuntivo.

### 4.2 Oggetti principali

`Brand`, `VehicleModel` e `Vehicle` costituiscono il catalogo. `VehiclePurpose`
distingue `FOR_SALE`, `RENTAL` e `ACQUISITION_REQUEST`; `VehicleStatus` descrive
la disponibilità operativa. `Rental` e `TestDrive` modellano i processi
temporali. `SaleOrder`, `Payment` e `Discount` modellano l'operazione cliente;
`PurchaseProposal` il percorso di acquisizione dal cliente; `StockOrder`
l'approvvigionamento. `User/Role` governano identità e autorizzazioni.

### 4.3 Regole essenziali

Identificativi utente e veicolo sono unici; prezzi e quantità sono validi e
coerenti col purpose; noleggi e test drive non si sovrappongono illegalmente;
le prese in carico sono atomiche; le transizioni seguono le macchine a stati;
un pagamento riferisce un solo contesto e non può eccedere il residuo; solo un Manager può
decidere una proposta `OFFERED`. L'elenco completo numerato è in
[`REQUISITI.md`](../docs/REQUISITI.md).

## 5. Requisiti e casi d'uso

### 5.1 Requisiti funzionali

| Area | Requisiti |
|---|---|
| Accesso | RF-AUTH-01 registrazione; RF-AUTH-02 login; RF-AUTH-03 logout |
| Customer | RF-CAT-01, RF-TD-01, RF-RENT-01/02, RF-SALE-01, RF-ACQ-01 |
| Salesman | RF-TD-02, RF-RENT-03, RF-ACQ-02, RF-INV-01 |
| Manager | RF-DASH-01, RF-STOCK-01, RF-PRICE-01, RF-ACQ-03 |
| Trasversali | RF-PAY-01, RF-ERR-01 |

I requisiti non funzionali coprono sicurezza, autorizzazione, integrità,
atomicità, concorrenza, manutenibilità, testabilità, feedback UI, ambiente,
privacy e tracciabilità.

### 5.2 Casi d'uso significativi

Sono stati formalizzati diciotto template, ognuno con numero, titolo, livello,
attori, precondizioni, trigger, postcondizioni/garanzia minima, pagine, flusso
base numerato, alternative numerate e test. I principali sono:

| Attore | Casi d'uso |
|---|---|
| User | UC-AUTH-01/02/03 |
| Customer | UC-CAT-01, UC-TD-01, UC-RENT-01/02, UC-SALE-01, UC-ACQ-01 |
| Salesman | UC-TD-02, UC-RENT-03, UC-ACQ-02, UC-INV-01 |
| Manager | UC-DASH-01, UC-STOCK-01, UC-PRICE-01, UC-ACQ-03 |
| Funzione inclusa | UC-PAY-01 |

Il testo completo è in
[`USE_CASE_TEMPLATES.md`](../docs/USE_CASE_TEMPLATES.md); il diagramma manuale è
[`use-cases.puml`](../docs/diagrams/use-cases.puml).

### 5.3 Tracciabilità

La matrice completa è in
[`MATRICE_TRACCIABILITA.md`](../docs/MATRICE_TRACCIABILITA.md). I riferimenti ai
servizi e test rappresentano obblighi di verifica e non prove di completamento.

## 6. Interfaccia e navigazione

La welcome page consente Login o Register. La registrazione riuscita apre
direttamente la sessione e il workspace del ruolo; il login usa lo stesso
`RoleRouter`. Errori di validazione restano sulla pagina corrente e il logout
torna a Welcome. Le viste previste sono:

- `Welcome.fxml`, `Login.fxml`, `Register.fxml`;
- `CustomerWorkspace.fxml`, con tab catalogo, test drive, noleggio,
  prenotazioni e proposta veicolo;
- `SalesmanWorkspace.fxml`, con test drive, noleggi, proposte e inventario;
- `ManagerWorkspace.fxml`, con dashboard, nuovi veicoli, sconti e approvazioni;
- dialog logico di pagamento P-40, con esiti successo, fallimento e
  annullamento. `PaymentDialog.fxml` è presente, ma nella baseline esaminata il
  Customer usa ancora un `ChoiceDialog`: il cablaggio è da completare o
  dichiarare estensione futura.

Il file originale `Login UI.png` mostra “CREATE ACCOUNT” e viene quindi usato
come mockup di registrazione. `Employee UI.png` è una dashboard Manager. Il
mockup Customer è parziale e non elimina le altre funzioni dei diagrammi.
Navigazione: [`page-navigation.puml`](../docs/diagrams/page-navigation.puml).

## 7. Architettura

### 7.1 Vista a strati

La dipendenza principale è `presentation → business → domain`; il business
dipende da porte `dao.interfaces`, implementate dagli adapter
`dao.postgres`. PostgreSQL e il gateway di pagamento simulato sono dipendenze
esterne. Il dominio non conosce JavaFX/JDBC; i controller non contengono SQL.

Diagramma: [`package-diagram.puml`](../docs/diagrams/package-diagram.puml).

### 7.2 Servizi applicativi

Il vocabolario di progetto prevede `AuthService`, `CatalogService`,
`RentalService`, `TestDriveService`, `SalesService`, `PurchaseProposalService`, `InventoryService`,
`PricingService`, `DashboardService` e `PaymentService`. Essi controllano ruolo,
coordinano aggregati e delimitano le transazioni tramite `UnitOfWork`.

### 7.3 Pattern e motivazioni

- Service Layer: rende espliciti i casi d'uso e mantiene sottili i controller.
- DAO/Unit of Work: separa query e transazioni dal dominio e abilita test doppi.
- Composition Root/Factory: centralizza configurazione e dipendenze.
- Gateway: isola il processore simulato e impedisce dipendenze finanziarie reali.
- Observer: propaga eventi significativi di inventario senza accoppiare i
  produttori a ogni reazione; deve essere dimostrato da un uso reale.

Dettaglio: [`ARCHITETTURA_E_DATI.md`](../docs/ARCHITETTURA_E_DATI.md).

## 8. Progettazione di dettaglio

### 8.1 Dominio

Le entità validano i dati all'ingresso e proteggono le transizioni. Gli stati
principali sono:

- TestDrive: `REQUESTED → CONFIRMED → IN_PROGRESS → COMPLETED`;
- Rental: `REQUESTED → ASSIGNED → CONFIRMED → ACTIVE → COMPLETED`;
- PurchaseProposal: `REQUESTED → OFFERED → APPROVED/REJECTED`;
- SaleOrder: `RESERVED → DEPOSIT_PAID` oppure `PAID → COMPLETED`;
- Payment: `PENDING → COMPLETED/FAILED`;
- StockOrder: `PLACED → CONFIRMED → RECEIVED`.

Le cancellazioni sono ammesse solo negli stati non terminali previsti dal
dominio. Diagramma: [`class-domain.puml`](../docs/diagrams/class-domain.puml).

### 8.2 Business

I servizi ricevono identità e comandi, caricano gli aggregati dalle porte,
invocano comportamenti del dominio, persistono e committono. Le operazioni
multi-entità eseguono rollback su qualunque errore. Diagramma:
[`class-business.puml`](../docs/diagrams/class-business.puml).

### 8.3 DAO

Sono previste porte per User, Brand, VehicleModel, Vehicle, Discount, Rental,
TestDrive, SaleOrder, Payment, PurchaseProposal e StockOrder, coordinate da
`UnitOfWork`. Gli adapter PostgreSQL incapsulano JDBC e mapping. Diagramma:
[`class-dao.puml`](../docs/diagrams/class-dao.puml).

### 8.4 Presentazione

I controller per Welcome, Login, Registration, Customer/Salesman/Manager
Workspace e PaymentDialog usano un contesto applicativo, una sessione, il
gateway UI e `NavigationManager`. Il controllo del ruolo nel business/domain
resta necessario anche se la UI nasconde un comando. Diagramma:
[`class-presentation.puml`](../docs/diagrams/class-presentation.puml).

## 9. Persistenza

Il modello canonico comprende `users`, `brands`, `vehicle_models`, `vehicles`,
`discounts`, `rentals`, `test_drives`, `sale_orders`, `payments`,
`purchase_proposals` e `stock_orders`. Le relazioni importanti sono Brand
1:N Model, Model 1:N Vehicle, Vehicle 0:1 Discount, attori 1:N verso i processi
e Payment riferito in XOR a Rental o SaleOrder.

Vincoli SQL e invarianti Java si sovrappongono intenzionalmente su unicità,
importi, date, ruoli, assignment e stati. Le migration devono essere applicabili
da database vuoto e usare gli stessi literal degli enum: versioni intermedie
dello schema usavano nomi legacy e vanno escluse dalla consegna.

Diagramma ER: [`er-diagram.puml`](../docs/diagrams/er-diagram.puml). Modello
relazionale e query critiche:
[`ARCHITETTURA_E_DATI.md`](../docs/ARCHITETTURA_E_DATI.md).

## 10. Scenari dinamici

Sono modellati manualmente tre scenari end-to-end:

1. richiesta di noleggio, pagamento e compensazione su failure:
   [`sequence-rent-payment.puml`](../docs/diagrams/sequence-rent-payment.puml);
2. richiesta test drive e presa in carico atomica fino al completamento:
   [`sequence-test-drive.puml`](../docs/diagrams/sequence-test-drive.puml);
3. proposta del Customer, offerta Salesman e decisione Manager:
   [`sequence-purchase-proposal-approval.puml`](../docs/diagrams/sequence-purchase-proposal-approval.puml).

Le sequenze distinguono flusso normale, failure e concorrenza; non implicano che
ogni metodo rappresentato esista già con quella firma.

Il flusso noleggio corrente usa UnitOfWork separate per richiesta, pagamento e
eventuale cancellazione compensativa. Questo evita una richiesta pagata in stato
errato a fine operazione, ma non elimina la finestra di crash fra i passi; il
limite A-13 va verificato o risolto prima di qualificare il flusso come atomico.

## 11. Implementazione e configurazione

La baseline tecnica rilevata usa Java 21, JavaFX 21/FXML, Maven, JDBC e
PostgreSQL 16; JUnit 5, Mockito, H2 e JaCoCo sono dipendenze di test/reporting.
`compose.yaml` e `.env.example` descrivono l'ambiente locale. Le migration sono
in `src/main/resources/db/migration`; le viste in
`src/main/resources/it/unifi/ing/drivehub/presentation/view`.

Queste tecnologie sono scelte progettuali, non vincoli espliciti delle note. I
dettagli di avvio e i percorsi per ruolo sono in
[`MANUALE_UTENTE.md`](../docs/MANUALE_UTENTE.md). Il comando previsto è
`mvn javafx:run`, dopo configurazione e avvio del database; va verificato su un
ambiente pulito prima di presentarlo come procedura collaudata.

Snippet realmente significativi, screenshot definitivi e riferimenti puntuali
ai metodi sono **[DA INSERIRE DOPO LA VERIFICA FINALE]**, come richiesto dalle
note (p. 24 e p. 33). Inserirli ora rischierebbe di documentare una versione
intermedia.

## 12. Strategia di test

Il piano combina unit test del dominio, unit test dei servizi con fake/mock,
integration test DAO su PostgreSQL, test funzionali tracciati agli UC e prova UI
manuale. Le priorità sono transizioni, ownership, conflitti di slot, transazioni,
XOR/coerenza del pagamento, CAS sullo stato atteso e mapping SQL.

La copertura non ha una soglia inventata: viene usata come indicatore, mentre i
flussi alternativi critici hanno precedenza. La tabella dei risultati è:

| Build/test | Data | Ambiente | Risultato |
|---|---|---|---|
| `mvn clean test` | [DA AGGIORNARE] | [DA AGGIORNARE] | non dichiarato eseguito |
| test PostgreSQL | [DA AGGIORNARE] | [DA AGGIORNARE] | non dichiarato eseguito |
| prova UI | [DA AGGIORNARE] | [DA AGGIORNARE] | non dichiarato eseguito |
| JaCoCo | [DA AGGIORNARE] | [DA AGGIORNARE] | non dichiarato generato |

Piano e casi attesi: [`PIANO_TEST.md`](../docs/PIANO_TEST.md).

## 13. Uso dell'intelligenza artificiale

Un assistente AI è stato usato per inventario, confronto delle fonti,
individuazione dei conflitti, supporto alla specifica, documentazione, PlantUML
manuale e scaffolding soggetto a review. Le fonti originali sono rimaste
inalterate; nessun risultato di test è stato inventato. L'AI non sostituisce la
comprensione degli autori, la review delle firme o l'esecuzione.

La dichiarazione completa e il registro da completare sono in
[`USO_AI.md`](../docs/USO_AI.md), in applicazione della richiesta delle note
(p. 4).

## 14. Assunzioni, conflitti e limiti

Le decisioni maggiormente esposte a validazione sono: registrazione pubblica
dei ruoli staff nel solo prototipo; ownership dei noleggi da parte del Salesman;
estremo finale esclusivo per il calcolo giornaliero; acconto e rimborso ancora
senza policy commerciale completa; nessun fornitore per StockOrder.

I conflitti principali risolti sono:

- `Login UI.png` è registrazione; `Employee UI.png` è Manager;
- le due bozze ER sono state consolidate e le cardinalità normalizzate;
- `User/Customer/Utente` sono normalizzati in identità User + Role;
- un unico Vehicle usa un purpose, invece di tabelle duplicate;
- Payment, SaleOrder, PurchaseProposal e StockOrder colmano lacune dell'ER;
- “Sell vehicle” è Customer → concessionario, non vendita del catalogo;
- sono esplicitati success/failure/cancel del pagamento;
- commenti informali/inappropriati nei sorgenti non sono riprodotti.

Elenco completo: [`ANALISI_PRELIMINARE.md`](../docs/ANALISI_PRELIMINARE.md) e
[`ASSUNZIONI_E_LIMITI.md`](../docs/ASSUNZIONI_E_LIMITI.md).

Sono fuori scope sedi multiple, fornitori, officina/post-vendita, danni e
assicurazioni, finanziamenti/fatture, firma digitale, notifiche reali e circuiti
di pagamento reali.

## 15. Conclusioni e checklist

DriveHub dispone di un modello coerente e tracciabile per i tre ruoli, con
separazione architetturale, macchine a stati, modello dati e piano di verifica.
La completezza accademica dipende ancora dalla chiusura delle evidenze finali.

Prima della consegna:

- [ ] inserire autori, matricole, commit e sottoscrivere lo statement;
- [ ] confermare col docente le assunzioni A-05, A-08 e le policy commerciali;
- [ ] riallineare definitivamente enum, migration, seed e diagrammi;
- [ ] verificare nomi/firme di servizi, DAO e controller nella matrice;
- [ ] applicare migration da zero su PostgreSQL 16;
- [ ] eseguire build, test unitari/integration e prova UI;
- [ ] registrare output, ambiente, date ed eventuale coverage reale;
- [ ] aggiungere screenshot e snippet significativi verificati;
- [ ] esportare i PlantUML senza generarli dal codice;
- [ ] aggiornare il registro AI e revisionare la relazione con tutti gli autori;
- [ ] produrre il PDF finale e inviarlo nei tempi/modalità previsti dal docente.

# Analisi preliminare

## 1. Scopo e metodo

L'analisi ha ricostruito DriveHub senza modificare il progetto ChargeNet, i
diagrammi originali o il materiale didattico. In caso di contrasto è stata
applicata la gerarchia richiesta:

1. disciplinare e note specifiche del progetto;
2. diagrammi originali DriveHub;
3. materiale ufficiale del corso;
4. ChargeNet come riferimento metodologico;
5. best practice generali.

Le scelte non determinate dalle fonti sono riportate in
[Assunzioni e limiti](ASSUNZIONI_E_LIMITI.md), non presentate come requisiti
originari.

## 2. Inventario delle fonti

### 2.1 Progetto completo di riferimento

`../ChargeNet-master` contiene 181 file, fra cui:

- 83 classi Java di produzione e 18 classi di test;
- 30 risorse, comprendenti FXML, CSS e schema SQL;
- 8 diagrammi PlantUML;
- 40 file della relazione, inclusi sorgenti LaTeX, template dei casi d'uso,
  mockup e PDF finale.

ChargeNet è stato usato per ricavare struttura a layer, profondità della
relazione, separazione servizi/DAO/presentazione, tipologia dei test e modalità
di documentazione. Non sono state trasferite regole del dominio della ricarica
elettrica.

### 2.2 Diagrammi originali DriveHub

In `../Diagrammi`:

- `Users SWE.drawio`: attori, casi d'uso e relazioni `include`;
- `Page Navigation Diagram.drawio`: accesso, instradamento per ruolo e pagine;
- `Package diagram.drawio`: package `presentation`, `business`, `domain`,
  `dao` e PostgreSQL;
- `SWE ER.drawio`: due bozze successive del modello ER, annotazioni e punti
  ancora aperti.

I file sono XML draw.io non compressi e sono stati analizzati sia come XML sia
nelle relazioni grafiche.

### 2.3 Mockup originali

Nella radice della cartella di lavoro:

- `Login UI.png`: non è una pagina di login; mostra **Create account** e viene
  quindi classificata come mockup di registrazione `P-02`;
- `Customer UI.png`: catalogo visuale di veicoli a noleggio con filtri;
- `Employee UI.png`: dashboard etichettata internamente come Manager.

I nomi dei file sono mantenuti per non alterare gli originali; nel progetto si
usa la funzione effettivamente mostrata.

### 2.4 Disciplinare e materiale del corso

La cartella `../Slides and notes - academic year 202526-20260727` contiene 73
file (21 PDF, 22 PNG, 9 modelli StarUML, 6 DOCX, 5 RAR, 3 ZIP, 2 HTML, 1 PPTX e
4 TXT). La fonte prioritaria è
`NotesOnTheProjectWork_Jan2025.pdf`, versione 1.1, 47 pagine fisiche.
L'elenco ricorsivo completo e le note di leggibilità sono in
[`INVENTARIO_FONTI.md`](INVENTARIO_FONTI.md).

Regole applicate:

- relazione con frontespizio e indice autoesplicativo;
- statement, architettura, tecnologie, requisiti, design, implementazione e
  testing;
- use-case template obbligatori con numero, titolo, livello, attori,
  pre/postcondizioni, flusso base e alternative;
- coerenza fra use case, pagine e test;
- package diagram e class diagram per package con responsabilità descritte;
- ER e modello relazionale per la persistenza;
- test funzionali tracciati ai flussi e test strutturali sugli elementi
  significativi, oltre a DAO/query;
- documentazione dell'uso di AI;
- UML progettato intenzionalmente, non generato dal codice.

Mockup e page-navigation diagram sono raccomandati, non sostituiscono i
template. Il disciplinare è relativo all'A.A. 2024/2025 e reca ancora la nota
“to be completed”; non è stato trovato un disciplinare successivo.

### 2.5 Progetto DriveHub

La destinazione separata è `../DriveHub`. La baseline tecnica verificata nel
`pom.xml` usa Java 21, JavaFX 21, PostgreSQL JDBC, JUnit 5, Mockito, H2 per i
test e JaCoCo. `compose.yaml` prepara PostgreSQL 16 e `.env.example` contiene
solo valori locali dimostrativi.

La baseline finale comprende cinque migration, viste FXML e servizi/adapter
verificati. Il registro dell'11 settembre 2026 documenta 109 test full-stack senza
failure/error/skipped su Temurin 21, PostgreSQL 16.15 e JavaFX sotto Xvfb; classi,
metodi e fingerprint esatti sono in `VERIFICA_FINALE.md` e nelle evidenze.

## 3. Dominio ricostruito

DriveHub è un sistema informativo per un concessionario che vende, noleggia e
acquisisce veicoli proposti dai clienti. Gli attori sono:

- **Customer**: consulta il catalogo, prenota test drive, noleggia, riserva o
  acquista un veicolo, consulta i propri noleggi e propone al concessionario un
  veicolo da vendere;
- **Salesman**: gestisce inventario, test drive e noleggi di propria competenza
  e prepara proposte di acquisto per i veicoli offerti dai clienti;
- **Manager**: consulta indicatori, registra ordini di stock, governa prezzi e
  promozioni e approva o rifiuta le proposte di acquisto.

`User` rappresenta l'identità comune e non un quarto ruolo operativo.
Registrazione e login sono funzioni comuni; dopo il login il sistema instrada
l'utente secondo il ruolo.

## 4. Matrice ChargeNet → DriveHub

| Elemento del riferimento | Equivalente DriveHub | Fonte o motivazione |
|---|---|---|
| `Driver` | `Customer` | Attore cliente nei diagrammi DriveHub |
| `StationOperator` | `Salesman` | Ruolo operativo che prende in carico pratiche |
| `EnergyManager` | `Manager` | Ruolo di supervisione e decisione |
| `ChargingStation` | `Vehicle` | Risorsa centrale consultata e resa disponibile |
| `ChargingSession` | `Rental` | Processo con periodo, stato, costo e cliente |
| approvazione stazione | approvazione `PurchaseProposal` | Workflow operatore → manager nei casi d'uso |
| wallet/transazione | `Payment` riferito a noleggio o ordine di vendita | `Process payment` incluso nei flussi cliente |
| onboarding stazione | `StockOrder` e aggiornamento inventario | `Order new vehicles`, `Manage inventory` |
| servizi per ruolo | servizi applicativi per capacità | Separazione business richiesta dal package diagram |
| DAO astratti + PostgreSQL | porte DAO + implementazioni JDBC PostgreSQL | Package diagram e modello relazionale |
| JavaFX controller/FXML | workspace Customer/Salesman/Manager | Page navigation e mockup |
| test business e DAO | test di servizi, transazioni e query DriveHub | Disciplinare, pp. 26–29 |
| rating/alert | nessun equivalente | Non supportato dai diagrammi DriveHub; escluso |
| grid/load balancing | nessun equivalente | Specifico di ChargeNet; escluso |
| relazione e PlantUML | relazione Markdown e PlantUML DriveHub | Riutilizzo del formato, non dei contenuti |

## 5. Conflitti rilevati e decisioni

| ID | Conflitto o lacuna | Decisione applicata |
|---|---|---|
| C-01 | `Login UI.png` mostra una registrazione | Trattato come `P-02 Registrazione`; login è una pagina distinta |
| C-02 | `SWE ER.drawio` contiene due bozze sovrapposte | Adottata la bozza unificata con `Vehicle.vehicleType`; la prima resta storico |
| C-03 | `Utente`, `Cliente`, `Customer` e `User` sono alternati | Codice inglese: `User` comune e ruolo `Customer` |
| C-04 | Prima bozza separa veicoli vendita/noleggio, la seconda usa `tipo` | Un'unica entità `Vehicle` con purpose `RENTAL`, `FOR_SALE`, `ACQUISITION_REQUEST` |
| C-05 | `Process payment` non ha entità nell'ER | Introdotto `Payment`, indispensabile per il caso d'uso, riferito in modo esclusivo a `Rental` o `SaleOrder` |
| C-06 | Ordini e proposte sono nei casi d'uso ma non modellati compiutamente nell'ER | Introdotti `StockOrder` e `PurchaseProposal` con attributi e stati minimi |
| C-07 | `Richiesta` elenca il cliente ma non ha il relativo arco | La richiesta è resa come `PurchaseProposal` collegata a Customer, Salesman e Vehicle |
| C-08 | Il diagramma domanda se ogni salesman gestisca solo le proprie prenotazioni | Assunto sì; la regola è tracciata come `RB-10` e resta da validare col docente |
| C-09 | Le cardinalità ER sono in alcuni punti invertite o mancanti | Normalizzate secondo chiavi esterne e vincoli espliciti nel modello relazionale |
| C-10 | La registrazione propone la scelta del ruolo, inclusi ruoli staff | Mantenuta nel prototipo perché rappresentata; marcata non idonea alla produzione senza provisioning amministrativo |
| C-11 | La page navigation non esplicita esiti del pagamento | Aggiunti successo, fallimento e annullamento nei flussi e nel PlantUML |
| C-12 | `Sell vehicle` può sembrare vendita del concessionario | Interpretato come Customer che offre il proprio veicolo al concessionario; sfocia nella proposta del Salesman e nell'approvazione del Manager |
| C-13 | I mockup mostrano dati, valuta e date di esempio | Considerati esclusivamente illustrativi, non requisiti di localizzazione o dati reali |
| C-14 | `Employee UI.png` mostra un Manager | Classificato come mockup Manager; non definisce un ulteriore ruolo Employee |
| C-15 | Customer mockup mostra solo noleggio, mentre i diagrammi includono vendita e test drive | Il mockup è una vista parziale; prevale l'insieme dei casi d'uso |

Le annotazioni informali o inappropriate presenti nel sorgente ER non sono state
riprodotte. È stato preservato soltanto il significato funzionale legittimo.

## 6. Piano operativo adottato

1. congelare fonti, conflitti e assunzioni;
2. numerare requisiti e regole di business;
3. redigere template completi e matrice di tracciabilità;
4. modellare manualmente package, classi, ER, navigazione e scenari;
5. allineare documentazione, SQL, codice e test;
6. eseguire test e registrare risultati reali;
7. completare dati personali, screenshot definitivi e frontespizio prima della
   consegna.

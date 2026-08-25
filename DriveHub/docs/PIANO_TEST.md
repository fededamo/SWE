# Piano di test

## 1. Stato

Il 23 agosto 2026 è stata eseguita offline la suite Maven finale disponibile:
43 test, 0 failure, 0 error e 0 skipped, `BUILD SUCCESS` in 4,189 s. Ambiente:
OpenJDK 25.0.4 con profilo Maven automatico `jdk-25-verification`; i test DAO
usano H2 2.2.224 in `MODE=PostgreSQL`.

Non sono stati eseguiti un test contro un server PostgreSQL 16 live né una
prova GUI interattiva. Il report JaCoCo è stato saltato perché JaCoCo 0.8.12 non
strumenta i class file di JDK 25; la configurazione resta attiva su JDK 21.

## 2. Strategia

| Livello | Scopo | Dipendenze |
|---|---|---|
| Unità dominio | invarianti, valori limite, transizioni | nessun DB/JavaFX |
| Unità servizi | autorizzazione e orchestrazione | fake/mock DAO, clock e gateway |
| Integrazione DAO | mapping, constraint, query, rollback | PostgreSQL di test preferito |
| Funzionale | flussi base e alternative dei casi d'uso | applicazione e DB isolati |
| UI/manuale | layout, navigazione, feedback e accessibilità di base | JavaFX grafico |

H2 può accelerare unit/integration test semplici, ma non sostituisce il test
dei comportamenti PostgreSQL-specifici.

## 3. Test funzionali pianificati

Gli ID `FT-*` descrivono il catalogo di accettazione. Non vanno confusi con il
numero dei metodi JUnit già eseguiti: la suite copre un sottoinsieme prioritario
del catalogo tramite test di dominio, servizi, DAO, architettura e contratti UI.

| ID | Scenario | Esito atteso |
|---|---|---|
| FT-AUTH-01 | registrazione valida | account e sessione creati; workspace del ruolo |
| FT-AUTH-02 | CF/email duplicati o dati invalidi | nessun account parziale; errore correggibile |
| FT-AUTH-03 | login valido per ciascun ruolo | routing a P-10/P-20/P-30 |
| FT-AUTH-04 | credenziali errate/inattivo | accesso negato senza dettaglio sensibile |
| FT-AUTH-05 | logout | sessione chiusa e P-00 |
| FT-CAT-01 | filtri combinati | solo veicoli conformi |
| FT-CAT-02 | nessun risultato/filtro invalido | stato vuoto o validazione chiara |
| FT-TD-01 | prenotazione slot libero | TestDrive `REQUESTED` |
| FT-TD-02 | slot passato o conflittuale | nessun duplicato |
| FT-TD-03 | conferma, avvio, completamento | sequenza di stati valida |
| FT-TD-04 | doppia presa in carico/stato illecito | secondo aggiornamento rifiutato |
| FT-RENT-01 | noleggio valido e pagamento riuscito | Rental `REQUESTED`, Payment `COMPLETED` |
| FT-RENT-02 | date errate o sovrapposizione | nessuna richiesta incoerente |
| FT-RENT-03 | pagamento fallito | nessun esito completato |
| FT-RENT-04 | annullamento dialog pagamento | ritorno alla pagina senza conferma |
| FT-RENT-05 | elenco Customer | solo propri noleggi |
| FT-RENT-06 | annullamento ammesso/non ammesso | stato coerente o rifiuto |
| FT-RENT-07 | presa in carico e avanzamento | `ASSIGNED→CONFIRMED→ACTIVE→COMPLETED` |
| FT-RENT-08 | noleggio di altro Salesman | modifica negata |
| FT-SALE-01 | acconto | ordine `DEPOSIT_PAID`, pagamento completato |
| FT-SALE-02 | saldo | ordine `PAID`, veicolo riservato |
| FT-SALE-03 | veicolo concorrente/pagamento fallito | rollback o rifiuto senza doppia vendita |
| FT-ACQ-01 | Customer propone veicolo | vehicle acquisition request e proposta `REQUESTED` |
| FT-ACQ-02 | targa duplicata/dati invalidi | nessuna pratica parziale |
| FT-ACQ-03 | Salesman formula offerta | proposta `OFFERED` |
| FT-ACQ-04 | seconda offerta/stato non valido | rifiuto |
| FT-ACQ-05 | Manager approva | proposta `APPROVED` con revisore/istante |
| FT-ACQ-06 | Manager rifiuta o race | `REJECTED` oppure conflitto gestito |
| FT-INV-01 | transizione inventario valida | stato aggiornato e notifica coerente |
| FT-INV-02 | veicolo impegnato/transizione illecita | rifiuto |
| FT-DASH-01 | dashboard su dataset noto | aggregati uguali agli expected del fixture |
| FT-STOCK-01 | ordine valido | `PLACED` con totale corretto |
| FT-STOCK-02 | quantità/costo non validi | nessun ordine |
| FT-PRICE-01 | cambio prezzo/tariffa | solo campo pertinente aggiornato |
| FT-PRICE-02 | sconto valido | una promozione attiva |
| FT-PRICE-03 | percentuale/periodo invalidi | promozione precedente invariata |
| FT-PAY-01 | successo simulato | Payment `COMPLETED` |
| FT-PAY-02 | failure simulato | Payment `FAILED`; operazione non promossa |
| FT-PAY-03 | secondo saldo dopo copertura dell'importo | richiesta rifiutata |

## 4. Unità e integrazione prioritarie

- `UT-DOM-*`: costruttori, ruoli, prezzi/importi, date e ogni transizione.
- `UT-AUTH-*`: normalizzazione email/CF, hashing e verifica, account inattivo.
- `UT-RENT-*` e `UT-TD-*`: sovrapposizioni, ownership e stato inatteso.
- `UT-SALES-*` e `UT-PAY-*`: sconto, acconto/saldo, idempotenza e rollback.
- `IT-USER-01`: unicità e round-trip utente.
- `IT-VEHICLE-*`: filtri, optimistic update e constraint dei prezzi.
- `IT-TD-*`/`IT-RENT-*`: slot, date, assegnazione atomica e query ownership.
- `IT-PROPOSAL-*`: coerenza attori/review e update condizionale.
- `IT-PAY-*`: XOR del riferimento, coerenza purpose e atomicità con ordine/noleggio.
- `IT-TX-01/02`: commit completo e rollback su eccezione deliberata.

## 5. Ambiente ed esecuzione prevista

1. usare un database isolato e credenziali non di produzione;
2. applicare le migration su database vuoto;
3. eseguire `mvn clean test`;
4. generare il report JaCoCo con `mvn jacoco:report` se configurato;
5. eseguire i test PostgreSQL e poi i test UI manuali;
6. salvare output, versione JDK/PostgreSQL, commit e data.

Non è fissata una percentuale minima perché le note del corso non ne indicano
una. La copertura è un indicatore: hanno priorità invarianti, alternative e
transazioni rispetto a getter o codice puramente dichiarativo.

## 6. Registro risultati

| Esecuzione | Data | Ambiente | Test | Failure/error/skipped | Esito/note |
|---|---|---|---:|---:|---|
| `mvn -o clean test` | 2026-08-23 | OpenJDK 25.0.4; profilo `jdk-25-verification`; H2 2.2.224 PostgreSQL mode | 43 | 0/0/0 | `BUILD SUCCESS`, 4,189 s |
| PostgreSQL 16 live | non eseguito | server non disponibile nell'ambiente | — | — | resta verifica esterna |
| prova GUI interattiva | non eseguita | ambiente grafico non disponibile | — | — | FXML verificati solo tramite contract test |
| JaCoCo | saltato su JDK 25 | plugin 0.8.12 incompatibile col classfile JDK 25 | — | — | abilitato nel profilo JDK 21 |

### 6.1 Ripartizione della suite eseguita

| Classe di test | Numero | Ambito |
|---|---:|---|
| `UserAndVehicleTest` | 6 | identità, ruoli, veicolo e invarianti |
| `RentalWorkflowTest` | 4 | stati Rental/TestDrive |
| `SalesWorkflowTest` | 4 | SaleOrder, Payment, PurchaseProposal, StockOrder |
| `PricingAndSecurityTest` | 4 | strategia prezzo/sconto e PBKDF2 |
| `AuthServiceTest` | 4 | registrazione/login e conflitti |
| `RentalServiceTest` | 2 | richiesta e claim atomico |
| `PaymentServiceTest` | 1 | pagamento noleggio e proprietà |
| `PurchaseProposalServiceTest` | 2 | workflow offerta/decisione |
| `DatabaseBootstrapIntegrationTest` | 4 | migration e bootstrap su H2 PostgreSQL mode |
| `PostgresDaoIntegrationTest` | 5 | round-trip, query e CAS DAO |
| `LayeringTest` | 3 | dipendenze architetturali |
| `FxmlContractTest` | 2 | caricabilità/contratto delle viste |
| `SessionContextTest` | 2 | apertura, accesso e chiusura sessione |
| **Totale** | **43** | **0 failure, 0 error, 0 skipped** |

## 7. Criteri di uscita

- build pulita sulla configurazione JDK 25 documentata: soddisfatto;
- nessun test disponibile fallito: soddisfatto (43/43);
- almeno flusso base e alternative critiche di ogni UC significativo verificati;
- migration applicabile da zero su H2 PostgreSQL mode: soddisfatto; su
  PostgreSQL 16 live: non eseguito;
- nessun segreto o dato personale reale nel repository/output;
- matrice e registro aggiornati con classi di test ed esiti effettivi;
- prova GUI interattiva e coverage JaCoCo su JDK 21 restano verifiche consigliate.

# Template dei casi d'uso

## 1. Catalogo pagine

| ID | Pagina/area | Evidenza |
|---|---|---|
| P-00 | Welcome | Page Navigation Diagram; `Welcome.fxml` |
| P-01 | Login | Page Navigation Diagram; `Login.fxml` |
| P-02 | Registrazione | nodo Register; mockup `Login UI.png`; `Register.fxml` |
| P-10 | Customer — Catalogo | Vehicles catalog; `CustomerWorkspace.fxml` |
| P-11 | Customer — Test drive | Test drives; tab Test drive |
| P-12 | Customer — Noleggia | Rentals; tab Noleggia |
| P-13 | Customer — Le mie prenotazioni | Rental bookings; tab omonimo |
| P-14 | Customer — Vendi un veicolo | Vehicle sale; tab omonimo |
| P-20 | Salesman — Test drive | Test drives bookings |
| P-21 | Salesman — Noleggi | Rentals bookings |
| P-22 | Salesman — Proposte | Purchase proposal creation |
| P-23 | Salesman — Inventario | Inventory dashboard |
| P-30 | Manager — Dashboard | Performance dashboard; mockup `Employee UI.png` |
| P-31 | Manager — Nuovi veicoli | New vehicles options |
| P-32 | Manager — Prezzi/promozioni | Discounts page |
| P-33 | Manager — Approvazioni | Purchase approval page |
| P-40 | Pagamento | Payment processing; `PaymentDialog.fxml` |

Le pagine di ciascun ruolo sono tab del relativo workspace, non finestre autonome.

## Confine e livelli

L'oggetto della specifica è l'intera applicazione DriveHub. Servizi, DAO, database e simulatore di pagamento appartengono a tale confine e **non sono attori secondari**. Gli altri ruoli che vedranno una pratica in seguito sono stakeholder di un caso successivo, non partecipanti simultanei. Per questa baseline tutti i template dichiarano quindi assenza di attori secondari esterni. Un processore finanziario esterno sarebbe un attore solo dopo una reale integrazione, fuori scope.

Le slide sui casi d'uso distinguono user goal (interazione che realizza un obiettivo applicativo), function (dettaglio di interazione) e summary (insieme di obiettivi). Registrarsi è l'obiettivo di ottenere un account; login, logout e pagamento sono funzioni di supporto, dettagliate comunque per sicurezza e fallimenti. Le gestioni staff sono template user goal che attraversano successivi accessi allo stesso processo; non implicano una singola sessione continua.

Ogni passo riporta fra parentesi la pagina in cui avviene; le alternative ereditano la pagina del passo d'origine salvo diversa indicazione. Il campo **Errori e fallimenti** si applica anche quando la tabella delle alternative cita una causa specifica. I test FT sono scenari identificati; metodo eseguibile e stato corrente sono separati nel piano test e nella matrice. Un ID qui citato non attesta un test passato.

## UC-AUTH-01 — Registrare un account

- **Livello:** user goal
- **Attore primario:** User non autenticato
- **Attori secondari:** nessuno esterno al confine DriveHub.
- **Descrizione:** crea un'identità DriveHub e il ruolo richiesto.
- **Precondizioni:** nessuna sessione autenticata; dati personali fittizi o
  legittimamente forniti.
- **Trigger:** l'utente seleziona “Registrati” da `P-00` o `P-01`.
- **Postcondizione di successo:** account attivo persistito, sessione aperta e
  workspace coerente con il ruolo visualizzato.
- **Garanzia minima:** nessuna password in chiaro e nessun account parziale.
- **Pagine:** P-00/P-01 → P-02 → P-10/P-20/P-30.

- **Errori e fallimenti:** validazione e conflitti mantengono la pagina correggibile; errori di persistenza annullano la transazione corrente e producono un messaggio senza dettagli sensibili. Una lettura fallita non mostra dati inventati.

### Flusso base

1. (P-02) Il sistema mostra codice fiscale, nome, cognome, email, telefono, password e ruolo.
2. (P-02) L'utente compila i campi e conferma.
3. (P-02) Il sistema valida formato, campi obbligatori e ruolo.
4. (P-02) Il sistema verifica l'unicità di email e codice fiscale.
5. (P-02) Il sistema protegge la password e crea l'account.
6. (P-02) Il sistema apre una sessione per il nuovo account.
7. (P-10/P-20/P-30) Il router apre il workspace Customer, Salesman o Manager.

### Flussi alternativi

- **3a — Dati non validi:** il sistema evidenzia i campi, conserva quelli
  correggibili e torna al passo 2.
- **4a — Email già presente:** nessun account viene creato; l'utente può
  correggere l'email o aprire il login.
- **4b — Codice fiscale già presente:** come 4a.
- **2b — Scelta di ruolo staff:** ammessa dal prototipo didattico (`A-05`); non viene descritto come implementato un provisioning amministrativo.
- **5b — Errore di persistenza:** rollback, messaggio generico, nessun dato
  sensibile esposto.

- **Test associati:** FT-AUTH-01, FT-AUTH-02, UT-AUTH-01, IT-USER-01.

- **Copertura dei flussi:** base → FT-AUTH-01; alternative 3a/4a/4b → FT-AUTH-02. Stato e metodo: `MATRICE_TRACCIABILITA.md`; verifiche di layout/routing: checklist UI.

## UC-AUTH-02 — Accedere al sistema

- **Livello:** function
- **Attore primario:** User registrato
- **Attori secondari:** nessuno esterno al confine DriveHub.
- **Descrizione:** accedere al sistema mediante il dialogo fra attore e sistema descritto dai passi.
- **Precondizioni:** account esistente e attivo.
- **Trigger:** l'utente apre `P-01` e invia le credenziali.
- **Postcondizione di successo:** sessione associata all'identità; workspace del
  ruolo aperto.
- **Garanzia minima:** in caso di errore non viene rivelato quale dato sia errato.
- **Pagine:** P-00 → P-01 → P-10/P-20/P-30.

- **Errori e fallimenti:** validazione e conflitti mantengono la pagina correggibile; errori di persistenza annullano la transazione corrente e producono un messaggio senza dettagli sensibili. Una lettura fallita non mostra dati inventati.

### Flusso base

1. (P-01) Il sistema richiede email e password.
2. (P-01) L'utente invia le credenziali.
3. (P-01) Il sistema recupera l'account per email e verifica password e stato attivo.
4. (P-01) Il sistema crea il contesto utente.
5. (P-10/P-20/P-30) Il router apre il workspace Customer, Salesman o Manager.

### Flussi alternativi

- **3a — Credenziali errate:** il sistema mostra un errore indistinto e resta a P-01.
- **3b — Account disattivato:** accesso negato con messaggio non sensibile.
- **4a — Sessione non disponibile:** la navigazione autenticata viene rifiutata; resta necessario autenticarsi.

- **Test associati:** FT-AUTH-03, FT-AUTH-04, UT-AUTH-02.

- **Copertura dei flussi:** base → FT-AUTH-03; alternative 3a/3b → FT-AUTH-04. Stato e metodo: `MATRICE_TRACCIABILITA.md`; verifiche di layout/routing: checklist UI.

## UC-AUTH-03 — Terminare la sessione

- **Livello:** function
- **Attore primario:** User autenticato
- **Attori secondari:** nessuno esterno al confine DriveHub.
- **Descrizione:** terminare la sessione mediante il dialogo fra attore e sistema descritto dai passi.
- **Precondizioni:** sessione attiva.
- **Trigger:** selezione “Logout”.
- **Postcondizione di successo:** contesto utente rimosso; P-00 visibile.
- **Garanzia minima:** una richiesta rifiutata non altera i dati persistenti; il contesto autenticato resta controllato e nessun successo viene comunicato per errore.
- **Pagine:** qualunque workspace → P-00.

- **Errori e fallimenti:** validazione e conflitti mantengono la pagina correggibile; errori di persistenza annullano la transazione corrente e producono un messaggio senza dettagli sensibili. Una lettura fallita non mostra dati inventati.

### Flusso base

1. (P-10/P-20/P-30) L'utente richiede il logout.
2. (P-10/P-20/P-30) Il sistema invalida il contesto di sessione.
3. (P-00) Il sistema torna alla welcome page.

### Flussi alternativi

- **2a — Sessione già assente:** il cleanup locale è idempotente; successive operazioni autenticate sono negate.

- **Test associati:** FT-AUTH-05, UT-NAV-01.

- **Copertura dei flussi:** base → FT-AUTH-05; alternative 2a → FT-AUTH-05, UI-M02. Stato e metodo: `MATRICE_TRACCIABILITA.md`; verifiche di layout/routing: checklist UI.

## UC-CAT-01 — Consultare il catalogo

- **Livello:** user goal
- **Attore primario:** Customer
- **Attori secondari:** nessuno esterno al confine DriveHub.
- **Descrizione:** consultare il catalogo mediante il dialogo fra attore e sistema descritto dai passi.
- **Precondizioni:** Customer autenticato.
- **Trigger:** apertura di P-10 o azione “Cerca”.
- **Postcondizione di successo:** elenco dei veicoli disponibili coerente con i filtri.
- **Garanzia minima:** una richiesta rifiutata non altera i dati persistenti; il contesto autenticato resta controllato e nessun successo viene comunicato per errore.
- **Pagine:** P-10; dal risultato si accede a P-11, P-12 o UC-SALE-01.

- **Errori e fallimenti:** validazione e conflitti mantengono la pagina correggibile; errori di persistenza annullano la transazione corrente e producono un messaggio senza dettagli sensibili. Una lettura fallita non mostra dati inventati.

### Flusso base

1. (P-10) Il sistema mostra i veicoli disponibili.
2. (P-10) Il Customer indica facoltativamente testo, tipo e prezzo massimo.
3. (P-10) Il sistema valida i filtri ed esegue la ricerca.
4. (P-10) Il sistema mostra targa, modello, tipo, chilometraggio, prezzo/tariffa e stato.
5. (P-10) Il Customer seleziona un veicolo per l'azione desiderata.

### Flussi alternativi

- **3a — Prezzo non numerico o negativo:** il filtro è rifiutato e correggibile.
- **4a — Nessun risultato:** il sistema mostra uno stato vuoto, non un errore.
- **5a — Veicolo divenuto indisponibile:** l'azione è bloccata e il catalogo aggiornato.

- **Test associati:** FT-CAT-01, FT-CAT-02, UT-CAT-01, IT-VEHICLE-01.

- **Copertura dei flussi:** base → FT-CAT-01; alternative 3a/4a → FT-CAT-02. Stato e metodo: `MATRICE_TRACCIABILITA.md`; verifiche di layout/routing: checklist UI.

## UC-TD-01 — Prenotare un test drive

- **Livello:** user goal
- **Attore primario:** Customer
- **Attori secondari:** nessuno esterno al confine DriveHub.
- **Descrizione:** prenotare un test drive mediante il dialogo fra attore e sistema descritto dai passi.
- **Precondizioni:** Customer autenticato; veicolo `FOR_SALE`, esistente e `AVAILABLE`.
- **Trigger:** “Prenota test drive” da P-10.
- **Postcondizione di successo:** TestDrive in stato `REQUESTED`.
- **Garanzia minima:** nessuna doppia prenotazione di cliente o veicolo nello slot.
- **Pagine:** P-10 → P-11.

- **Errori e fallimenti:** validazione e conflitti mantengono la pagina correggibile; errori di persistenza annullano la transazione corrente e producono un messaggio senza dettagli sensibili. Una lettura fallita non mostra dati inventati.

### Flusso base

1. (P-10) Il Customer seleziona un veicolo dal catalogo.
2. (P-11) Il sistema apre P-11 e mostra il veicolo scelto.
3. (P-11) Il Customer indica data e ora future e conferma.
4. (P-11) Il sistema valida formato e disponibilità dello slot.
5. (P-11) Il sistema registra la prenotazione e la mostra nell'elenco.

### Flussi alternativi

- **3a — Data/ora mancante o non futura:** il sistema richiede la correzione.
- **4a — Veicolo già occupato:** nessuna prenotazione; scegliere un altro slot.
- **4b — Customer già impegnato nell’intervallo:** nessuna prenotazione.
- **5a — Conflitto concorrente:** il vincolo dati rifiuta il secondo inserimento
  e il sistema aggiorna gli slot.

- **Test associati:** FT-TD-01, FT-TD-02, UT-TD-01, IT-TD-01.

- **Copertura dei flussi:** base → FT-TD-01; alternative 3a/4a/4b/5a → FT-TD-02. Stato e metodo: `MATRICE_TRACCIABILITA.md`; verifiche di layout/routing: checklist UI.

## UC-RENT-01 — Noleggiare e pagare un veicolo

- **Livello:** user goal
- **Attore primario:** Customer
- **Attori secondari:** nessuno esterno al confine DriveHub.
- **Descrizione:** noleggiare e pagare un veicolo mediante il dialogo fra attore e sistema descritto dai passi.
- **Precondizioni:** Customer autenticato; veicolo `RENTAL` selezionato.
- **Trigger:** “Noleggia” da P-10.
- **Postcondizione di successo:** Rental `REQUESTED`, Payment `COMPLETED` e
  disponibilità coerente; la conferma operativa resta al Salesman.
- **Garanzia minima:** rifiuto conserva atomicamente Payment `FAILED` e Rental `CANCELLED`; annullamento del dialog non scrive dati e un errore inatteso provoca rollback completo.
- **Pagine:** P-10 → P-12 → P-40 → P-13.

- **Errori e fallimenti:** validazione e conflitti mantengono la pagina correggibile; errori di persistenza annullano la transazione corrente e producono un messaggio senza dettagli sensibili. Una lettura fallita non mostra dati inventati.

### Flusso base

1. (P-10) Il Customer seleziona un veicolo `RENTAL` disponibile.
2. (P-12) Il sistema apre P-12.
3. (P-12) Il Customer indica data iniziale e finale.
4. (P-12) Il sistema valida periodo e assenza di sovrapposizioni.
5. (P-12) Il sistema calcola giorni, tariffa applicabile e totale e mostra il preventivo.
6. (P-40) Il Customer conferma.
7. (P-40) Il sistema crea la richiesta di Rental e avvia UC-PAY-01 nella stessa unità
   di lavoro applicativa.
8. (P-40) Il pagamento riesce.
9. (P-13) Il sistema conserva il Rental in `REQUESTED` e mostra la prenotazione.

### Flussi alternativi

- **3a — Periodo incompleto:** il preventivo non viene calcolato.
- **4a — Intervallo non valido:** correggere le date e tornare al passo 3.
- **4b — Sovrapposizione:** il sistema segnala indisponibilità.
- **7a — Preventivo cambiato:** il prezzo ricalcolato non coincide con quello confermato; rollback e nuova conferma richiesta.
- **8a — Pagamento fallito:** nella stessa transazione si conservano Rental `CANCELLED` e Payment `FAILED`; il Customer riceve il rifiuto. Un nuovo tentativo avvia una nuova richiesta.
- **8b — Pagamento annullato:** nessun comando applicativo e nessuna scrittura; ritorno a P-12.
- **9a — Concorrenza sulla disponibilità:** transazione annullata e messaggio al Customer.

- **Test associati:** FT-RENT-01..04, UT-RENT-01..03, UT-PAY-01, IT-RENT-01,
  IT-PAY-01.

- **Copertura dei flussi:** base → FT-RENT-01; alternative 4a/4b → FT-RENT-02; 8a → FT-RENT-03; 8b → FT-RENT-04; 7a/9a → FT-SALE-03 e test checkout. Stato e metodo: `MATRICE_TRACCIABILITA.md`; verifiche di layout/routing: checklist UI.

## UC-RENT-02 — Consultare o annullare i propri noleggi

- **Livello:** user goal
- **Attore primario:** Customer
- **Attori secondari:** nessuno esterno al confine DriveHub.
- **Descrizione:** consultare o annullare i propri noleggi mediante il dialogo fra attore e sistema descritto dai passi.
- **Precondizioni:** Customer autenticato.
- **Trigger:** apertura P-13.
- **Postcondizione di successo:** elenco limitato al Customer; eventuale Rental posto in
  `CANCELLED`.
- **Garanzia minima:** una richiesta rifiutata non altera i dati persistenti; il contesto autenticato resta controllato e nessun successo viene comunicato per errore.
- **Pagine:** P-13.

- **Errori e fallimenti:** validazione e conflitti mantengono la pagina correggibile; errori di persistenza annullano la transazione corrente e producono un messaggio senza dettagli sensibili. Una lettura fallita non mostra dati inventati.

### Flusso base

1. (P-13) Il sistema recupera i noleggi del Customer autenticato.
2. (P-13) Il sistema mostra veicolo, periodo, importo e stato.
3. (P-13) Il Customer seleziona un noleggio annullabile.
4. (P-13) Il Customer conferma l'annullamento.
5. (P-13) Il sistema aggiorna lo stato e ricalcola la disponibilità.

### Flussi alternativi

- **1a — Nessun noleggio:** stato vuoto.
- **3a — Noleggio non annullabile:** comando disabilitato o rifiutato dal servizio.
- **4a — Conferma negata:** nessuna modifica.
- **5a — Stato cambiato nel frattempo:** refresh e messaggio di conflitto.

- **Test associati:** FT-RENT-05, FT-RENT-06, UT-RENT-04, IT-RENT-02.

- **Copertura dei flussi:** base → FT-RENT-05/06; alternative 3a/5a → FT-RENT-06. Stato e metodo: `MATRICE_TRACCIABILITA.md`; verifiche di layout/routing: checklist UI.

## UC-SALE-01 — Riservare o acquistare un veicolo

- **Livello:** user goal
- **Attore primario:** Customer
- **Attori secondari:** nessuno esterno al confine DriveHub.
- **Descrizione:** riservare o acquistare un veicolo mediante il dialogo fra attore e sistema descritto dai passi.
- **Precondizioni:** Customer autenticato; Vehicle `FOR_SALE` e `AVAILABLE`.
- **Trigger:** “Prenota/acquista” da P-10.
- **Postcondizione di successo:** SaleOrder `DEPOSIT_PAID` (acconto) o `PAID`
  (saldo), Payment `COMPLETED` e Vehicle non più acquistabile da terzi.
- **Garanzia minima:** una richiesta rifiutata non altera i dati persistenti; il contesto autenticato resta controllato e nessun successo viene comunicato per errore.
- **Pagine:** P-10 → P-40 → P-10.

- **Errori e fallimenti:** validazione e conflitti mantengono la pagina correggibile; errori di persistenza annullano la transazione corrente e producono un messaggio senza dettagli sensibili. Una lettura fallita non mostra dati inventati.

### Flusso base

1. (P-10) Il Customer seleziona un veicolo in vendita.
2. (P-40) Il sistema mostra prezzo, sconto applicabile e importo richiesto.
3. (P-40) Il Customer sceglie riserva o acquisto e conferma.
4. (P-40) Il sistema verifica nuovamente la disponibilità.
5. (P-40) Il sistema crea SaleOrder `RESERVED` e avvia UC-PAY-01.
6. (P-40) Il pagamento riesce.
7. (P-10) Il sistema marca l'ordine `DEPOSIT_PAID` o `PAID` e mantiene il veicolo
   `RESERVED`; la consegna conclude l'ordine in `COMPLETED`.

### Flussi alternativi

- **2a — Promozione scaduta:** il sistema ricalcola e richiede nuova conferma.
- **4a — Veicolo non disponibile:** nessun ordine; catalogo aggiornato.
- **5a — Preventivo cambiato:** il totale o l’importo richiesto differisce da quello confermato; nessun pagamento e nuova conferma. La percentuale demo dell’acconto è 10%, da validare come scelta commerciale.
- **6a — Pagamento fallito:** Payment `FAILED`, SaleOrder `CANCELLED` e veicolo nuovamente `AVAILABLE` nella stessa transazione.
- **3b — Pagamento annullato:** il dialog si chiude senza invocare checkout e senza scritture.
- **7a — Conflitto concorrente:** rollback completo.

- **Test associati:** FT-SALE-01..03, UT-SALE-01..03, IT-SALE-01, IT-PAY-02.

- **Copertura dei flussi:** base → FT-SALE-01/02; alternative 4a/5a/6a/7a → FT-SALE-03; 3b → UI-M04. Stato e metodo: `MATRICE_TRACCIABILITA.md`; verifiche di layout/routing: checklist UI.

## UC-ACQ-01 — Proporre un veicolo al concessionario

- **Livello:** user goal
- **Attore primario:** Customer
- **Attori secondari:** nessuno esterno al confine DriveHub.
- **Descrizione:** proporre un veicolo al concessionario mediante il dialogo fra attore e sistema descritto dai passi.
- **Precondizioni:** Customer autenticato.
- **Trigger:** apertura P-14.
- **Postcondizione di successo:** Vehicle con purpose `ACQUISITION_REQUEST` e
  PurchaseProposal `REQUESTED`, disponibili al Salesman.
- **Garanzia minima:** una richiesta rifiutata non altera i dati persistenti; il contesto autenticato resta controllato e nessun successo viene comunicato per errore.
- **Pagine:** P-14 → P-22.

- **Errori e fallimenti:** validazione e conflitti mantengono la pagina correggibile; errori di persistenza annullano la transazione corrente e producono un messaggio senza dettagli sensibili. Una lettura fallita non mostra dati inventati.

### Flusso base

1. (P-14) Il Customer inserisce targa, marchio, modello, anno, chilometraggio e importo richiesto.
2. (P-14) Il sistema valida dati e valori.
3. (P-14) Il sistema verifica che la targa non sia già presente.
4. (P-14) Il sistema registra il veicolo proposto collegato al Customer.
5. (P-14) Il sistema conferma l'invio.

### Flussi alternativi

- **2a — Dati non validi:** correzione senza perdita dei campi validi.
- **3a — Targa già registrata:** richiesta respinta per evitare duplicati.
- **4a — Errore atomico:** nessun veicolo/pratica parziale.

- **Test associati:** FT-ACQ-01, FT-ACQ-02, UT-SALES-01, IT-PROPOSAL-01.

- **Copertura dei flussi:** base → FT-ACQ-01; alternative 2a/3a/4a → FT-ACQ-02. Stato e metodo: `MATRICE_TRACCIABILITA.md`; verifiche di layout/routing: checklist UI.

## UC-TD-02 — Gestire i test drive

- **Livello:** user goal
- **Attore primario:** Salesman
- **Attori secondari:** nessuno esterno al confine DriveHub.
- **Descrizione:** gestire i test drive mediante il dialogo fra attore e sistema descritto dai passi.
- **Precondizioni:** Salesman autenticato.
- **Trigger:** apertura P-20.
- **Postcondizione di successo:** stato del TestDrive avanzato lungo
  `REQUESTED → CONFIRMED → IN_PROGRESS → COMPLETED`, oppure
  `CANCELLED` quando ammesso.
- **Garanzia minima:** una richiesta rifiutata non altera i dati persistenti; il contesto autenticato resta controllato e nessun successo viene comunicato per errore.
- **Pagine:** P-20.

- **Errori e fallimenti:** validazione e conflitti mantengono la pagina correggibile; errori di persistenza annullano la transazione corrente e producono un messaggio senza dettagli sensibili. Una lettura fallita non mostra dati inventati.

### Flusso base

1. (P-20) Il sistema mostra le prenotazioni pertinenti.
2. (P-20) Il Salesman seleziona una prenotazione `REQUESTED` e la conferma, prendendola
   in carico (`CONFIRMED`).
3. (P-20) All'arrivo del Customer seleziona “Avvia” (`IN_PROGRESS`).
4. (P-20) Al termine seleziona “Completa”.
5. (P-20) Il sistema verifica ruolo e stato e registra `COMPLETED`.

### Flussi alternativi

- **2a — Nessuna selezione:** nessuna modifica.
- **2b — Richiesta già presa in carico:** conferma atomica rifiutata e refresh.
- **3a — Annullamento:** quando ammesso, conferma e stato `CANCELLED`.
- **5a — Stato già terminale o modifica concorrente:** rifiuto e refresh.

- **Test associati:** FT-TD-03, FT-TD-04, UT-TD-02, IT-TD-02.

- **Copertura dei flussi:** base → FT-TD-03; alternative 2b/5a → FT-TD-04. Stato e metodo: `MATRICE_TRACCIABILITA.md`; verifiche di layout/routing: checklist UI.

## UC-RENT-03 — Gestire i noleggi assegnati

- **Livello:** user goal
- **Attore primario:** Salesman
- **Attori secondari:** nessuno esterno al confine DriveHub.
- **Descrizione:** gestire i noleggi assegnati mediante il dialogo fra attore e sistema descritto dai passi.
- **Precondizioni:** Salesman autenticato.
- **Trigger:** apertura P-21.
- **Postcondizione di successo:** assegnazione o stato del Rental aggiornato validamente.
- **Garanzia minima:** una richiesta rifiutata non altera i dati persistenti; il contesto autenticato resta controllato e nessun successo viene comunicato per errore.
- **Pagine:** P-21.

- **Errori e fallimenti:** validazione e conflitti mantengono la pagina correggibile; errori di persistenza annullano la transazione corrente e producono un messaggio senza dettagli sensibili. Una lettura fallita non mostra dati inventati.

### Flusso base

1. (P-21) Il sistema mostra noleggi disponibili o assegnati secondo RB-10.
2. (P-21) Il Salesman seleziona un noleggio non assegnato e lo prende in carico.
3. (P-21) Il sistema assegna il Salesman in modo atomico.
4. (P-21) Il Salesman conferma (`CONFIRMED`) e, all'inizio, attiva (`ACTIVE`) il noleggio.
5. (P-21) Al termine, il Salesman seleziona “Completa”.
6. (P-21) Il sistema registra `COMPLETED` e rende coerente lo stato del veicolo.

### Flussi alternativi

- **2a — Già assegnato ad altro Salesman:** presa in carico rifiutata.
- **4a — Annullamento ammesso:** stato `CANCELLED` e disponibilità aggiornata.
- **5a — Noleggio non `ACTIVE`:** completamento rifiutato.

- **Test associati:** FT-RENT-07, FT-RENT-08, UT-RENT-05, IT-RENT-03.

- **Copertura dei flussi:** base → FT-RENT-07; alternative 2a/4a/5a → FT-RENT-08 e FT-RENT-06. Stato e metodo: `MATRICE_TRACCIABILITA.md`; verifiche di layout/routing: checklist UI.

## UC-ACQ-02 — Creare e inviare una proposta di acquisto

- **Livello:** user goal
- **Attore primario:** Salesman
- **Attori secondari:** nessuno esterno al confine DriveHub.
- **Descrizione:** creare e inviare una proposta di acquisto mediante il dialogo fra attore e sistema descritto dai passi.
- **Precondizioni:** Salesman autenticato; PurchaseProposal `REQUESTED` del
  Customer riferita a un Vehicle `ACQUISITION_REQUEST`.
- **Trigger:** selezione pratica in P-22.
- **Postcondizione di successo:** PurchaseProposal `OFFERED` collegata a
  Vehicle, Customer e Salesman.
- **Garanzia minima:** una richiesta rifiutata non altera i dati persistenti; il contesto autenticato resta controllato e nessun successo viene comunicato per errore.
- **Pagine:** P-22 → P-33.

- **Errori e fallimenti:** validazione e conflitti mantengono la pagina correggibile; errori di persistenza annullano la transazione corrente e producono un messaggio senza dettagli sensibili. Una lettura fallita non mostra dati inventati.

### Flusso base

1. (P-22) Il sistema mostra i veicoli proposti e la valutazione richiesta.
2. (P-22) Il Salesman seleziona una pratica.
3. (P-22) Il Salesman inserisce l'importo offerto.
4. (P-22) Il sistema valida importo, ruolo e assenza di proposta attiva incompatibile.
5. (P-22) Il sistema crea e invia la proposta.
6. (P-33) La proposta compare nella coda Manager.

### Flussi alternativi

- **3a — Importo mancante/negativo:** correzione richiesta.
- **4a — Proposta non più `REQUESTED`:** operazione rifiutata.
- **4b — Proposta già sottoposta:** nessun duplicato.
- **5a — Errore di persistenza:** rollback.

- **Test associati:** FT-ACQ-03, FT-ACQ-04, UT-SALES-02, IT-PROPOSAL-02.

- **Copertura dei flussi:** base → FT-ACQ-03; alternative 4a/4b → FT-ACQ-04. Stato e metodo: `MATRICE_TRACCIABILITA.md`; verifiche di layout/routing: checklist UI.

## UC-INV-01 — Gestire lo stato dell'inventario

- **Livello:** user goal
- **Attore primario:** Salesman
- **Attori secondari:** nessuno esterno al confine DriveHub.
- **Descrizione:** gestire lo stato dell'inventario mediante il dialogo fra attore e sistema descritto dai passi.
- **Precondizioni:** Salesman autenticato; Vehicle esistente.
- **Trigger:** apertura P-23.
- **Postcondizione di successo:** stato del Vehicle aggiornato con transizione lecita.
- **Garanzia minima:** una richiesta rifiutata non altera i dati persistenti; il contesto autenticato resta controllato e nessun successo viene comunicato per errore.
- **Pagine:** P-23.

- **Errori e fallimenti:** validazione e conflitti mantengono la pagina correggibile; errori di persistenza annullano la transazione corrente e producono un messaggio senza dettagli sensibili. Una lettura fallita non mostra dati inventati.

### Flusso base

1. (P-23) Il sistema mostra inventario con targa, modello, tipo e stato.
2. (P-23) Il Salesman seleziona un Vehicle.
3. (P-23) Il Salesman seleziona un nuovo stato ammesso.
4. (P-23) Il sistema verifica conflitti con noleggi, ordini e prenotazioni.
5. (P-23) Il sistema salva lo stato e aggiorna l'elenco.

### Flussi alternativi

- **3a — Stato uguale o transizione illecita:** nessuna modifica.
- **4a — Veicolo impegnato:** transizione incompatibile rifiutata.
- **5a — Versione concorrente:** refresh obbligatorio.

- **Test associati:** FT-INV-01, FT-INV-02, UT-INV-01, IT-VEHICLE-02.

- **Copertura dei flussi:** base → FT-INV-01; alternative 3a/4a/5a → FT-INV-02. Stato e metodo: `MATRICE_TRACCIABILITA.md`; verifiche di layout/routing: checklist UI.

## UC-DASH-01 — Consultare la dashboard

- **Livello:** user goal
- **Attore primario:** Manager
- **Attori secondari:** nessuno esterno al confine DriveHub.
- **Descrizione:** consultare la dashboard mediante il dialogo fra attore e sistema descritto dai passi.
- **Precondizioni:** Manager autenticato.
- **Trigger:** apertura o aggiornamento P-30.
- **Postcondizione di successo:** metriche calcolate sui dati correnti e attività mostrate.
- **Garanzia minima:** una richiesta rifiutata non altera i dati persistenti; il contesto autenticato resta controllato e nessun successo viene comunicato per errore.
- **Pagine:** P-30.

- **Errori e fallimenti:** validazione e conflitti mantengono la pagina correggibile; errori di persistenza annullano la transazione corrente e producono un messaggio senza dettagli sensibili. Una lettura fallita non mostra dati inventati.

### Flusso base

1. (P-30) Il sistema verifica il ruolo Manager.
2. (P-30) Il sistema calcola veicoli totali, noleggi aperti (non terminali), incassi registrati e proposte in attesa.
3. (P-30) Il sistema recupera le attività di sintesi.
4. (P-30) Il sistema mostra valori complessivi e date delle attività; non offre una selezione di periodo contabile.

### Flussi alternativi

- **2a — Query fallita:** la lettura viene interrotta e la UI comunica l’errore; non si sostituisce il valore con zero.
- **3a — Nessuna attività:** stato vuoto.
- **1a — Ruolo non Manager:** accesso negato.

- **Test associati:** FT-DASH-01, UT-DASH-01, IT-DASH-01.

- **Copertura dei flussi:** base → FT-DASH-01; alternative 1a/3a → FT-DASH-02. Stato e metodo: `MATRICE_TRACCIABILITA.md`; verifiche di layout/routing: checklist UI.

## UC-STOCK-01 — Ordinare nuovi veicoli

- **Livello:** user goal
- **Attore primario:** Manager
- **Attori secondari:** nessuno esterno al confine DriveHub.
- **Descrizione:** ordinare nuovi veicoli mediante il dialogo fra attore e sistema descritto dai passi.
- **Precondizioni:** Manager autenticato.
- **Trigger:** apertura P-31.
- **Postcondizione di successo:** StockOrder `PLACED` riferito a un VehicleModel.
- **Garanzia minima:** una richiesta rifiutata non altera i dati persistenti; il contesto autenticato resta controllato e nessun successo viene comunicato per errore.
- **Pagine:** P-31.

- **Errori e fallimenti:** validazione e conflitti mantengono la pagina correggibile; errori di persistenza annullano la transazione corrente e producono un messaggio senza dettagli sensibili. Una lettura fallita non mostra dati inventati.

### Flusso base

1. (P-31) Il Manager indica marchio, modello, anno, quantità e costo unitario.
2. (P-31) Il sistema valida valori e identifica o crea Brand e VehicleModel coerenti.
3. (P-31) Il sistema crea lo StockOrder.
4. (P-31) Il sistema mostra codice e stato.

### Flussi alternativi

- **1a — Quantità non positiva o costo negativo:** correzione richiesta.
- **2a — Modello omonimo incompatibile:** creazione rifiutata e conflitto segnalato.
- **3a — Errore transazionale:** nessun brand/modello/ordine parziale.

- **Test associati:** FT-STOCK-01, FT-STOCK-02, UT-INV-02, IT-STOCK-01.

- **Copertura dei flussi:** base → FT-STOCK-01; alternative 1a/3a → FT-STOCK-02. Stato e metodo: `MATRICE_TRACCIABILITA.md`; verifiche di layout/routing: checklist UI.

## UC-PRICE-01 — Gestire prezzi e promozioni

- **Livello:** user goal
- **Attore primario:** Manager
- **Attori secondari:** nessuno esterno al confine DriveHub.
- **Descrizione:** gestire prezzi e promozioni mediante il dialogo fra attore e sistema descritto dai passi.
- **Precondizioni:** Manager autenticato; Vehicle esistente.
- **Trigger:** apertura P-32.
- **Postcondizione di successo:** prezzo/tariffa o sconto aggiornato validamente.
- **Garanzia minima:** una richiesta rifiutata non altera i dati persistenti; il contesto autenticato resta controllato e nessun successo viene comunicato per errore.
- **Pagine:** P-32.

- **Errori e fallimenti:** validazione e conflitti mantengono la pagina correggibile; errori di persistenza annullano la transazione corrente e producono un messaggio senza dettagli sensibili. Una lettura fallita non mostra dati inventati.

### Flusso base

1. (P-32) Il sistema mostra Vehicle, prezzo/tariffa corrente e sconto.
2. (P-32) Il Manager seleziona un Vehicle.
3. (P-32) Il Manager indica il nuovo prezzo pertinente al tipo.
4. (P-32) Il sistema valida e salva il prezzo.
5. (P-32) Il Manager indica una percentuale e applica lo sconto.
6. (P-32) Il sistema verifica percentuale e periodo e sostituisce o crea l’unico record di sconto del veicolo; la rimozione lo disabilita.

### Flussi alternativi

- **3a — Prezzo negativo o campo non pertinente al tipo:** rifiuto.
- **5a — Percentuale fuori `(0,100)`:** rifiuto.
- **5b — Rimozione:** il Manager rimuove/disattiva lo sconto selezionato.
- **6a — Periodo incoerente:** rifiuto senza alterare la promozione precedente.

- **Test associati:** FT-PRICE-01..03, UT-PRICE-01..03, IT-DISCOUNT-01.

- **Copertura dei flussi:** base → FT-PRICE-01/02; alternative 3a/5a/6a → FT-PRICE-03. Stato e metodo: `MATRICE_TRACCIABILITA.md`; verifiche di layout/routing: checklist UI.

## UC-ACQ-03 — Approvare o rifiutare una proposta

- **Livello:** user goal
- **Attore primario:** Manager
- **Attori secondari:** nessuno esterno al confine DriveHub.
- **Descrizione:** approvare o rifiutare una proposta mediante il dialogo fra attore e sistema descritto dai passi.
- **Precondizioni:** Manager autenticato; PurchaseProposal `OFFERED`.
- **Trigger:** selezione proposta in P-33.
- **Postcondizione di successo:** proposta `APPROVED` o `REJECTED`, con revisore e istante.
- **Garanzia minima:** una richiesta rifiutata non altera i dati persistenti; il contesto autenticato resta controllato e nessun successo viene comunicato per errore.
- **Pagine:** P-33.

- **Errori e fallimenti:** validazione e conflitti mantengono la pagina correggibile; errori di persistenza annullano la transazione corrente e producono un messaggio senza dettagli sensibili. Una lettura fallita non mostra dati inventati.

### Flusso base

1. (P-33) Il sistema mostra le proposte in attesa con Salesman, Customer, Vehicle e importo.
2. (P-33) Il Manager seleziona una proposta.
3. (P-33) Il Manager sceglie “Approva”.
4. (P-33) Il sistema verifica stato e ruolo.
5. (P-33) Il sistema registra `APPROVED`, Manager e istante in una transazione.
6. (P-33) Il sistema aggiorna la coda.

### Flussi alternativi

- **2a — Nessuna selezione:** nessuna modifica.
- **3a — Rifiuto:** al passo 5 viene registrato `REJECTED`.
- **4a — Proposta già revisionata:** rifiuto e refresh.
- **5a — Conflitto/errore:** rollback; la proposta resta nello stato precedente.

- **Test associati:** FT-ACQ-05, FT-ACQ-06, UT-SALES-03, IT-PROPOSAL-03.

- **Copertura dei flussi:** base → FT-ACQ-05; alternative 3a/4a/5a → FT-ACQ-06. Stato e metodo: `MATRICE_TRACCIABILITA.md`; verifiche di layout/routing: checklist UI.

## UC-PAY-01 — Processare un pagamento dimostrativo

- **Livello:** function
- **Attore primario:** Customer tramite UC-RENT-01 o UC-SALE-01
- **Attori secondari:** nessuno esterno al confine DriveHub.
- **Descrizione:** processare un pagamento dimostrativo mediante il dialogo fra attore e sistema descritto dai passi.
- **Precondizioni:** intenzione di noleggio/acquisto oppure ordine già esistente; importo da confermare determinato. La creazione può avvenire dopo la conferma nella transazione checkout.
- **Trigger:** il caso includente richiede il pagamento.
- **Postcondizione di successo:** Payment `COMPLETED` per l'importo dovuto;
  il caso includente può confermare l'operazione.
- **Garanzia minima:** nessun dato di carta reale e nessuna doppia registrazione.
- **Pagine:** P-40.

- **Errori e fallimenti:** validazione e conflitti mantengono la pagina correggibile; errori di persistenza annullano la transazione corrente e producono un messaggio senza dettagli sensibili. Una lettura fallita non mostra dati inventati.

### Flusso base

1. (P-40) Il sistema mostra importo e riferimento non sensibile.
2. (P-40) Il Customer seleziona un metodo simulato.
3. (P-40) Il Customer conferma.
4. (P-40) Il servizio verifica ownership e importo ancora dovuto e registra Payment
   `PENDING`.
5. (P-40) Il gateway simulato restituisce successo.
6. (P-40) Il servizio registra `COMPLETED` e, per SaleOrder, aggiorna l'importo pagato.

### Flussi alternativi

- **2a — Nessun metodo:** conferma disabilitata o rifiutata.
- **3a — Annullamento:** dialog chiuso; nessun Payment riuscito.
- **4a — Importo già coperto o pagamento eccedente:** richiesta rifiutata.
- **5a — Fallimento simulato:** Payment `FAILED`; il caso includente non è confermato.
- **6a — Errore di persistenza:** rollback dell’intera transazione locale, senza esito di successo. Il gateway demo non produce effetti esterni da riconciliare.

- **Test associati:** FT-PAY-01..03, UT-PAY-01..04, IT-PAY-01..02.

- **Copertura dei flussi:** base → FT-PAY-01; alternative 3a → FT-RENT-04; 4a → FT-PAY-03; 5a → FT-PAY-02; 6a → IT-TX-02. Stato e metodo: `MATRICE_TRACCIABILITA.md`; verifiche di layout/routing: checklist UI.

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

Le pagine `P-10..P-14`, `P-20..P-23` e `P-30..P-33` possono essere realizzate
come tab di un workspace di ruolo. Il mapping è funzionale, non impone finestre
separate.

## UC-AUTH-01 — Registrare un account

- **Livello:** user goal
- **Attore primario:** User non autenticato
- **Attori secondari:** servizio identità, persistenza utenti
- **Descrizione:** crea un'identità DriveHub e il ruolo richiesto.
- **Precondizioni:** nessuna sessione autenticata; dati personali fittizi o
  legittimamente forniti.
- **Trigger:** l'utente seleziona “Registrati” da `P-00` o `P-01`.
- **Postcondizione di successo:** account attivo persistito, sessione aperta e
  workspace coerente con il ruolo visualizzato.
- **Garanzia minima:** nessuna password in chiaro e nessun account parziale.
- **Pagine:** P-00/P-01 → P-02 → P-10/P-20/P-30.

### Flusso base

1. Il sistema mostra codice fiscale, nome, cognome, email, telefono, password e ruolo.
2. L'utente compila i campi e conferma.
3. Il sistema valida formato, campi obbligatori e ruolo.
4. Il sistema verifica l'unicità di email e codice fiscale.
5. Il sistema protegge la password e crea l'account.
6. Il sistema apre una sessione per il nuovo account.
7. Il router apre il workspace Customer, Salesman o Manager.

### Flussi alternativi

- **3a — Dati non validi:** il sistema evidenzia i campi, conserva quelli
  correggibili e torna al passo 2.
- **4a — Email già presente:** nessun account viene creato; l'utente può
  correggere l'email o aprire il login.
- **4b — Codice fiscale già presente:** come 4a.
- **5a — Ruolo staff senza provisioning:** nell'assetto produttivo la richiesta
  è rifiutata; nel prototipo la scelta è ammessa e segnalata (`A-05`).
- **5b — Errore di persistenza:** rollback, messaggio generico, nessun dato
  sensibile esposto.

- **Test associati:** FT-AUTH-01, FT-AUTH-02, UT-AUTH-01, IT-USER-01.

## UC-AUTH-02 — Accedere al sistema

- **Livello:** user goal
- **Attore primario:** User registrato
- **Attori secondari:** servizio identità
- **Precondizioni:** account esistente e attivo.
- **Trigger:** l'utente apre `P-01` e invia le credenziali.
- **Postcondizione di successo:** sessione associata all'identità; workspace del
  ruolo aperto.
- **Garanzia minima:** in caso di errore non viene rivelato quale dato sia errato.
- **Pagine:** P-00 → P-01 → P-10/P-20/P-30.

### Flusso base

1. Il sistema richiede email e password.
2. L'utente invia le credenziali.
3. Il sistema recupera l'account per email e verifica password e stato attivo.
4. Il sistema crea il contesto utente.
5. Il router apre il workspace Customer, Salesman o Manager.

### Flussi alternativi

- **3a — Credenziali errate:** il sistema mostra un errore indistinto e resta a P-01.
- **3b — Account disattivato:** accesso negato con messaggio non sensibile.
- **5a — Ruolo sconosciuto:** la sessione viene chiusa e l'anomalia registrata.

- **Test associati:** FT-AUTH-03, FT-AUTH-04, UT-AUTH-02.

## UC-AUTH-03 — Terminare la sessione

- **Livello:** function
- **Attore primario:** User autenticato
- **Precondizioni:** sessione attiva.
- **Trigger:** selezione “Logout”.
- **Postcondizione:** contesto utente rimosso; P-00 visibile.
- **Pagine:** qualunque workspace → P-00.

### Flusso base

1. L'utente richiede il logout.
2. Il sistema invalida il contesto di sessione.
3. Il sistema torna alla welcome page.

### Flussi alternativi

- **2a — Cleanup parziale:** il sistema rimuove comunque i riferimenti locali e
  impedisce ulteriori operazioni autenticate.

- **Test associati:** FT-AUTH-05, UT-NAV-01.

## UC-CAT-01 — Consultare il catalogo

- **Livello:** user goal
- **Attore primario:** Customer
- **Attori secondari:** servizio catalogo
- **Precondizioni:** Customer autenticato.
- **Trigger:** apertura di P-10 o azione “Cerca”.
- **Postcondizione:** elenco dei veicoli disponibili coerente con i filtri.
- **Pagine:** P-10; dal risultato si accede a P-11, P-12 o UC-SALE-01.

### Flusso base

1. Il sistema mostra i veicoli disponibili.
2. Il Customer indica facoltativamente testo, tipo e prezzo massimo.
3. Il sistema valida i filtri ed esegue la ricerca.
4. Il sistema mostra targa, modello, tipo, chilometraggio, prezzo/tariffa e stato.
5. Il Customer seleziona un veicolo per l'azione desiderata.

### Flussi alternativi

- **3a — Prezzo non numerico o negativo:** il filtro è rifiutato e correggibile.
- **4a — Nessun risultato:** il sistema mostra uno stato vuoto, non un errore.
- **5a — Veicolo divenuto indisponibile:** l'azione è bloccata e il catalogo aggiornato.

- **Test associati:** FT-CAT-01, FT-CAT-02, UT-CAT-01, IT-VEHICLE-01.

## UC-TD-01 — Prenotare un test drive

- **Livello:** user goal
- **Attore primario:** Customer
- **Attori secondari:** servizio test drive
- **Precondizioni:** Customer autenticato; veicolo esistente e selezionabile.
- **Trigger:** “Prenota test drive” da P-10.
- **Postcondizione di successo:** TestDrive in stato `REQUESTED`.
- **Garanzia minima:** nessuna doppia prenotazione di cliente o veicolo nello slot.
- **Pagine:** P-10 → P-11.

### Flusso base

1. Il Customer seleziona un veicolo dal catalogo.
2. Il sistema apre P-11 e mostra il veicolo scelto.
3. Il Customer indica data e ora future e conferma.
4. Il sistema valida formato e disponibilità dello slot.
5. Il sistema registra la prenotazione e la mostra nell'elenco.

### Flussi alternativi

- **3a — Data/ora mancante o non futura:** il sistema richiede la correzione.
- **4a — Veicolo già occupato:** nessuna prenotazione; scegliere un altro slot.
- **4b — Customer già impegnato nello slot:** nessuna prenotazione.
- **5a — Conflitto concorrente:** il vincolo dati rifiuta il secondo inserimento
  e il sistema aggiorna gli slot.

- **Test associati:** FT-TD-01, FT-TD-02, UT-TD-01, IT-TD-01.

## UC-RENT-01 — Noleggiare e pagare un veicolo

- **Livello:** user goal
- **Attore primario:** Customer
- **Attori secondari:** servizio noleggi, UC-PAY-01, persistenza
- **Precondizioni:** Customer autenticato; veicolo `RENTAL` selezionato.
- **Trigger:** “Noleggia” da P-10.
- **Postcondizione di successo:** Rental `REQUESTED`, Payment `COMPLETED` e
  disponibilità coerente; la conferma operativa resta al Salesman.
- **Garanzia minima:** pagamento fallito, annullamento o rollback non producono
  un pagamento completato né una richiesta incoerente.
- **Pagine:** P-10 → P-12 → P-40 → P-13.

### Flusso base

1. Il Customer seleziona un veicolo `RENTAL` disponibile.
2. Il sistema apre P-12.
3. Il Customer indica data iniziale e finale.
4. Il sistema valida periodo e assenza di sovrapposizioni.
5. Il sistema calcola giorni, tariffa applicabile e totale e mostra il preventivo.
6. Il Customer conferma.
7. Il sistema crea la richiesta di Rental e avvia UC-PAY-01 nella stessa unità
   di lavoro applicativa.
8. Il pagamento riesce.
9. Il sistema conserva il Rental in `REQUESTED` e mostra la prenotazione.

### Flussi alternativi

- **3a — Periodo incompleto:** il preventivo non viene calcolato.
- **4a — Intervallo non valido:** correggere le date e tornare al passo 3.
- **4b — Sovrapposizione:** il sistema segnala indisponibilità.
- **7a — Nessun Salesman assegnabile:** nessun noleggio definitivo; esito da
  definire secondo la policy `A-08`.
- **8a — Pagamento fallito:** Rental resta non confermato ed è annullato o
  conservato per retry secondo la policy applicativa.
- **8b — Pagamento annullato:** nessun addebito; ritorno a P-12.
- **9a — Concorrenza sulla disponibilità:** transazione annullata e messaggio al Customer.

- **Test associati:** FT-RENT-01..04, UT-RENT-01..03, UT-PAY-01, IT-RENT-01,
  IT-PAY-01.

## UC-RENT-02 — Consultare o annullare i propri noleggi

- **Livello:** user goal
- **Attore primario:** Customer
- **Precondizioni:** Customer autenticato.
- **Trigger:** apertura P-13.
- **Postcondizione:** elenco limitato al Customer; eventuale Rental posto in
  `CANCELLED`.
- **Pagine:** P-13.

### Flusso base

1. Il sistema recupera i noleggi del Customer autenticato.
2. Il sistema mostra veicolo, periodo, importo e stato.
3. Il Customer seleziona un noleggio annullabile.
4. Il Customer conferma l'annullamento.
5. Il sistema aggiorna lo stato e ricalcola la disponibilità.

### Flussi alternativi

- **1a — Nessun noleggio:** stato vuoto.
- **3a — Noleggio non annullabile:** comando disabilitato o rifiutato dal servizio.
- **4a — Conferma negata:** nessuna modifica.
- **5a — Stato cambiato nel frattempo:** refresh e messaggio di conflitto.

- **Test associati:** FT-RENT-05, FT-RENT-06, UT-RENT-04, IT-RENT-02.

## UC-SALE-01 — Riservare o acquistare un veicolo

- **Livello:** user goal
- **Attore primario:** Customer
- **Attori secondari:** servizio vendite, UC-PAY-01
- **Precondizioni:** Customer autenticato; Vehicle `FOR_SALE` e `AVAILABLE`.
- **Trigger:** “Prenota/acquista” da P-10.
- **Postcondizione di successo:** SaleOrder `DEPOSIT_PAID` (acconto) o `PAID`
  (saldo), Payment `COMPLETED` e Vehicle non più acquistabile da terzi.
- **Pagine:** P-10 → P-40 → P-10.

### Flusso base

1. Il Customer seleziona un veicolo in vendita.
2. Il sistema mostra prezzo, sconto applicabile e importo richiesto.
3. Il Customer sceglie riserva o acquisto e conferma.
4. Il sistema verifica nuovamente la disponibilità.
5. Il sistema crea SaleOrder `RESERVED` e avvia UC-PAY-01.
6. Il pagamento riesce.
7. Il sistema marca l'ordine `DEPOSIT_PAID` o `PAID` e mantiene il veicolo
   `RESERVED`; la consegna conclude l'ordine in `COMPLETED`.

### Flussi alternativi

- **2a — Promozione scaduta:** il sistema ricalcola e richiede nuova conferma.
- **4a — Veicolo non disponibile:** nessun ordine; catalogo aggiornato.
- **5a — Importo dell'acconto non configurato:** operazione bloccata e lacuna segnalata.
- **6a — Pagamento fallito/annullato:** nessuna riserva o vendita confermata.
- **7a — Conflitto concorrente:** rollback completo.

- **Test associati:** FT-SALE-01..03, UT-SALE-01..03, IT-SALE-01, IT-PAY-02.

## UC-ACQ-01 — Proporre un veicolo al concessionario

- **Livello:** user goal
- **Attore primario:** Customer
- **Attori secondari:** servizio vendite/acquisizioni
- **Precondizioni:** Customer autenticato.
- **Trigger:** apertura P-14.
- **Postcondizione di successo:** Vehicle con purpose `ACQUISITION_REQUEST` e
  PurchaseProposal `REQUESTED`, disponibili al Salesman.
- **Pagine:** P-14 → P-22.

### Flusso base

1. Il Customer inserisce targa, marchio, modello, anno, chilometraggio e importo richiesto.
2. Il sistema valida dati e valori.
3. Il sistema verifica che la targa non sia già presente.
4. Il sistema registra il veicolo proposto collegato al Customer.
5. Il sistema conferma l'invio.

### Flussi alternativi

- **2a — Dati non validi:** correzione senza perdita dei campi validi.
- **3a — Targa già registrata:** richiesta respinta per evitare duplicati.
- **4a — Errore atomico:** nessun veicolo/pratica parziale.

- **Test associati:** FT-ACQ-01, FT-ACQ-02, UT-SALES-01, IT-PROPOSAL-01.

## UC-TD-02 — Gestire i test drive

- **Livello:** user goal
- **Attore primario:** Salesman
- **Precondizioni:** Salesman autenticato.
- **Trigger:** apertura P-20.
- **Postcondizione:** stato del TestDrive avanzato lungo
  `REQUESTED → CONFIRMED → IN_PROGRESS → COMPLETED`, oppure
  `CANCELLED` quando ammesso.
- **Pagine:** P-20.

### Flusso base

1. Il sistema mostra le prenotazioni pertinenti.
2. Il Salesman seleziona una prenotazione `REQUESTED` e la conferma, prendendola
   in carico (`CONFIRMED`).
3. All'arrivo del Customer seleziona “Avvia” (`IN_PROGRESS`).
4. Al termine seleziona “Completa”.
5. Il sistema verifica ruolo e stato e registra `COMPLETED`.

### Flussi alternativi

- **2a — Nessuna selezione:** nessuna modifica.
- **2a — Richiesta già presa in carico:** conferma atomica rifiutata e refresh.
- **3a — Annullamento:** quando ammesso, conferma e stato `CANCELLED`.
- **5a — Stato già terminale o modifica concorrente:** rifiuto e refresh.

- **Test associati:** FT-TD-03, FT-TD-04, UT-TD-02, IT-TD-02.

## UC-RENT-03 — Gestire i noleggi assegnati

- **Livello:** user goal
- **Attore primario:** Salesman
- **Precondizioni:** Salesman autenticato.
- **Trigger:** apertura P-21.
- **Postcondizione:** assegnazione o stato del Rental aggiornato validamente.
- **Pagine:** P-21.

### Flusso base

1. Il sistema mostra noleggi disponibili o assegnati secondo RB-10.
2. Il Salesman seleziona un noleggio non assegnato e lo prende in carico.
3. Il sistema assegna il Salesman in modo atomico.
4. Il Salesman conferma (`CONFIRMED`) e, all'inizio, attiva (`ACTIVE`) il noleggio.
5. Al termine, il Salesman seleziona “Completa”.
6. Il sistema registra `COMPLETED` e rende coerente lo stato del veicolo.

### Flussi alternativi

- **2a — Già assegnato ad altro Salesman:** presa in carico rifiutata.
- **4a — Annullamento ammesso:** stato `CANCELLED` e disponibilità aggiornata.
- **5a — Noleggio non `ACTIVE`:** completamento rifiutato.

- **Test associati:** FT-RENT-07, FT-RENT-08, UT-RENT-05, IT-RENT-03.

## UC-ACQ-02 — Creare e inviare una proposta di acquisto

- **Livello:** user goal
- **Attore primario:** Salesman
- **Attori secondari:** Customer interessato, Manager revisore
- **Precondizioni:** Salesman autenticato; PurchaseProposal `REQUESTED` del
  Customer riferita a un Vehicle `ACQUISITION_REQUEST`.
- **Trigger:** selezione pratica in P-22.
- **Postcondizione di successo:** PurchaseProposal `OFFERED` collegata a
  Vehicle, Customer e Salesman.
- **Pagine:** P-22 → P-33.

### Flusso base

1. Il sistema mostra i veicoli proposti e la valutazione richiesta.
2. Il Salesman seleziona una pratica.
3. Il Salesman inserisce l'importo offerto.
4. Il sistema valida importo, ruolo e assenza di proposta attiva incompatibile.
5. Il sistema crea e invia la proposta.
6. La proposta compare nella coda Manager.

### Flussi alternativi

- **3a — Importo mancante/negativo:** correzione richiesta.
- **4a — Proposta non più `REQUESTED`:** operazione rifiutata.
- **4b — Proposta già sottoposta:** nessun duplicato.
- **5a — Errore di persistenza:** rollback.

- **Test associati:** FT-ACQ-03, FT-ACQ-04, UT-SALES-02, IT-PROPOSAL-02.

## UC-INV-01 — Gestire lo stato dell'inventario

- **Livello:** user goal
- **Attore primario:** Salesman
- **Precondizioni:** Salesman autenticato; Vehicle esistente.
- **Trigger:** apertura P-23.
- **Postcondizione:** stato del Vehicle aggiornato con transizione lecita.
- **Pagine:** P-23.

### Flusso base

1. Il sistema mostra inventario con targa, modello, tipo e stato.
2. Il Salesman seleziona un Vehicle.
3. Il Salesman seleziona un nuovo stato ammesso.
4. Il sistema verifica conflitti con noleggi, ordini e prenotazioni.
5. Il sistema salva lo stato e aggiorna l'elenco.

### Flussi alternativi

- **3a — Stato uguale o transizione illecita:** nessuna modifica.
- **4a — Veicolo impegnato:** transizione incompatibile rifiutata.
- **5a — Versione concorrente:** refresh obbligatorio.

- **Test associati:** FT-INV-01, FT-INV-02, UT-INV-01, IT-VEHICLE-02.

## UC-DASH-01 — Consultare la dashboard

- **Livello:** user goal
- **Attore primario:** Manager
- **Precondizioni:** Manager autenticato.
- **Trigger:** apertura o aggiornamento P-30.
- **Postcondizione:** metriche calcolate sui dati correnti e attività mostrate.
- **Pagine:** P-30.

### Flusso base

1. Il sistema verifica il ruolo Manager.
2. Il sistema calcola veicoli totali, noleggi attivi, incassi registrati e proposte in attesa.
3. Il sistema recupera le attività di sintesi.
4. Il sistema mostra timestamp/periodo di riferimento e valori.

### Flussi alternativi

- **2a — Dato parzialmente indisponibile:** il valore è marcato non disponibile,
  non sostituito con un numero inventato.
- **3a — Nessuna attività:** stato vuoto.
- **1a — Ruolo non Manager:** accesso negato.

- **Test associati:** FT-DASH-01, UT-DASH-01, IT-DASH-01.

## UC-STOCK-01 — Ordinare nuovi veicoli

- **Livello:** user goal
- **Attore primario:** Manager
- **Precondizioni:** Manager autenticato.
- **Trigger:** apertura P-31.
- **Postcondizione:** StockOrder `PLACED` riferito a un VehicleModel.
- **Pagine:** P-31.

### Flusso base

1. Il Manager indica marchio, modello, anno, quantità e costo unitario.
2. Il sistema valida valori e identifica o crea Brand e VehicleModel coerenti.
3. Il sistema crea lo StockOrder.
4. Il sistema mostra codice e stato.

### Flussi alternativi

- **1a — Quantità non positiva o costo negativo:** correzione richiesta.
- **2a — Modello omonimo incompatibile:** creazione rifiutata e conflitto segnalato.
- **3a — Errore transazionale:** nessun brand/modello/ordine parziale.

- **Test associati:** FT-STOCK-01, FT-STOCK-02, UT-INV-02, IT-STOCK-01.

## UC-PRICE-01 — Gestire prezzi e promozioni

- **Livello:** user goal
- **Attore primario:** Manager
- **Precondizioni:** Manager autenticato; Vehicle esistente.
- **Trigger:** apertura P-32.
- **Postcondizione:** prezzo/tariffa o sconto aggiornato validamente.
- **Pagine:** P-32.

### Flusso base

1. Il sistema mostra Vehicle, prezzo/tariffa corrente e sconto.
2. Il Manager seleziona un Vehicle.
3. Il Manager indica il nuovo prezzo pertinente al tipo.
4. Il sistema valida e salva il prezzo.
5. Il Manager indica una percentuale e applica lo sconto.
6. Il sistema verifica percentuale e periodo e sostituisce/crea lo sconto attivo.

### Flussi alternativi

- **3a — Prezzo negativo o campo non pertinente al tipo:** rifiuto.
- **5a — Percentuale fuori `(0,100]`:** rifiuto.
- **5b — Rimozione:** il Manager rimuove/disattiva lo sconto selezionato.
- **6a — Periodo incoerente:** rifiuto senza alterare la promozione precedente.

- **Test associati:** FT-PRICE-01..03, UT-PRICE-01..03, IT-DISCOUNT-01.

## UC-ACQ-03 — Approvare o rifiutare una proposta

- **Livello:** user goal
- **Attore primario:** Manager
- **Attori secondari:** Salesman, Customer
- **Precondizioni:** Manager autenticato; PurchaseProposal `OFFERED`.
- **Trigger:** selezione proposta in P-33.
- **Postcondizione:** proposta `APPROVED` o `REJECTED`, con revisore e istante.
- **Pagine:** P-33.

### Flusso base

1. Il sistema mostra le proposte in attesa con Salesman, Customer, Vehicle e importo.
2. Il Manager seleziona una proposta.
3. Il Manager sceglie “Approva”.
4. Il sistema verifica stato e ruolo.
5. Il sistema registra `APPROVED`, Manager e istante in una transazione.
6. Il sistema aggiorna la coda.

### Flussi alternativi

- **2a — Nessuna selezione:** nessuna modifica.
- **3a — Rifiuto:** al passo 5 viene registrato `REJECTED`.
- **4a — Proposta già revisionata:** rifiuto e refresh.
- **5a — Conflitto/errore:** rollback; la proposta resta nello stato precedente.

- **Test associati:** FT-ACQ-05, FT-ACQ-06, UT-SALES-03, IT-PROPOSAL-03.

## UC-PAY-01 — Processare un pagamento dimostrativo

- **Livello:** function
- **Attore primario:** Customer tramite UC-RENT-01 o UC-SALE-01
- **Attori secondari:** PaymentService, gateway simulato
- **Precondizioni:** Rental `REQUESTED` o SaleOrder `RESERVED`/`DEPOSIT_PAID`;
  importo residuo determinato.
- **Trigger:** il caso includente richiede il pagamento.
- **Postcondizione di successo:** Payment `COMPLETED` per l'importo dovuto;
  il caso includente può confermare l'operazione.
- **Garanzia minima:** nessun dato di carta reale e nessuna doppia registrazione.
- **Pagine:** P-40.

### Flusso base

1. Il sistema mostra importo e riferimento non sensibile.
2. Il Customer seleziona un metodo simulato.
3. Il Customer conferma.
4. Il servizio verifica ownership e importo ancora dovuto e registra Payment
   `PENDING`.
5. Il gateway simulato restituisce successo.
6. Il servizio registra `COMPLETED` e, per SaleOrder, aggiorna l'importo pagato.

### Flussi alternativi

- **2a — Nessun metodo:** conferma disabilitata o rifiutata.
- **3a — Annullamento:** dialog chiuso; nessun Payment riuscito.
- **4a — Importo già coperto o pagamento eccedente:** richiesta rifiutata.
- **5a — Fallimento simulato:** Payment `FAILED`; il caso includente non è confermato.
- **6a — Errore di persistenza:** transazione annullata e stato da riconciliare,
  senza dichiarare successo all'utente.

- **Test associati:** FT-PAY-01..03, UT-PAY-01..04, IT-PAY-01..02.

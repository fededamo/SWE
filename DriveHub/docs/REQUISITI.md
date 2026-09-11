# Requisiti di DriveHub

## 1. Scope

Il sistema copre autenticazione, catalogo, noleggio, test drive, vendita di un
veicolo dal cliente al concessionario, prenotazione/acquisto dal catalogo,
pagamenti dimostrativi, inventario, ordini di stock, prezzi/promozioni e
dashboard. Sedi, fornitori, officina e post-vendita non sono inclusi perché non
supportati dalle fonti.

## 2. Requisiti funzionali

### Accesso

- **RF-AUTH-01 — Registrazione.** Il sistema deve consentire la creazione di un
  account raccogliendo codice fiscale, nome, cognome, email, telefono, password
  e ruolo. L'email e il codice fiscale devono essere univoci. La selezione
  pubblica di ruoli staff è ammessa solo nel prototipo didattico (`A-05`).
- **RF-AUTH-02 — Login.** Il sistema deve autenticare un account attivo con
  email e password e instradarlo all'area del ruolo associato.
- **RF-AUTH-03 — Logout.** Ogni area autenticata deve terminare la sessione e
  tornare alla welcome page.

### Customer

- **RF-CAT-01 — Consultazione catalogo.** Il Customer deve poter consultare i
  veicoli disponibili e filtrarli almeno per marchio/modello, tipo e prezzo.
- **RF-TD-01 — Prenotazione test drive.** Il Customer deve poter scegliere un
  veicolo, una data e un orario e creare una prenotazione priva di conflitti.
- **RF-RENT-01 — Noleggio veicolo.** Il Customer deve poter scegliere un
  veicolo a noleggio e un intervallo valido, ottenere il preventivo e confermare
  il noleggio mediante il flusso di pagamento.
- **RF-RENT-02 — Consultazione e annullamento noleggi.** Il Customer deve poter
  visualizzare esclusivamente i propri noleggi e annullare quelli per cui la
  transizione è consentita.
- **RF-SALE-01 — Prenotazione/acquisto.** Il Customer deve poter riservare o
  acquistare un veicolo in vendita, versando l'importo richiesto mediante il
  flusso di pagamento.
- **RF-ACQ-01 — Offerta di un veicolo al concessionario.** Il Customer deve
  poter comunicare targa, marchio, modello, anno, chilometraggio e valutazione
  richiesta per avviare il processo di acquisizione.

### Salesman

- **RF-TD-02 — Gestione test drive.** Il Salesman deve poter consultare le
  richieste e confermarle, avviarle, completarle o annullarle secondo stato.
- **RF-RENT-03 — Gestione noleggi.** Il Salesman deve poter prendere in carico,
  consultare e aggiornare i noleggi di propria competenza.
- **RF-ACQ-02 — Creazione proposta di acquisto.** Il Salesman deve poter
  esaminare un veicolo offerto dal Customer, indicare un importo e sottoporre
  una proposta al Manager.
- **RF-INV-01 — Gestione inventario.** Il Salesman deve poter consultare
  l'inventario e aggiornare lo stato operativo di un veicolo nei limiti delle
  transizioni ammesse.

### Manager

- **RF-DASH-01 — Dashboard.** Il Manager deve poter consultare almeno numero di
  veicoli, noleggi attivi, incassi registrati e proposte in attesa, con attività
  di sintesi.
- **RF-STOCK-01 — Ordine di nuovi veicoli.** Il Manager deve poter registrare un
  ordine specificando modello, quantità e costo unitario e seguirne lo stato.
- **RF-PRICE-01 — Prezzi e promozioni.** Il Manager deve poter aggiornare il
  prezzo pertinente al tipo di veicolo, applicare promozioni e disabilitarle;
  se più promozioni sono applicabili vale la percentuale maggiore, senza cumulo.
- **RF-ACQ-03 — Approvazione proposta di acquisto.** Il Manager deve poter
  approvare o rifiutare una proposta `OFFERED`, registrando revisore e data.

### Servizi trasversali

- **RF-PAY-01 — Pagamento.** Noleggio e prenotazione/acquisto devono includere
  un pagamento con esito riuscito o fallito e impedire un nuovo saldo quando
  l'importo dovuto risulta già coperto. Nel progetto
  universitario il gateway è simulato e non tratta strumenti finanziari reali.
- **RF-ERR-01 — Gestione errori.** Errori di validazione, autorizzazione,
  conflitto o persistenza devono produrre un messaggio comprensibile senza
  esporre credenziali, query o stack trace all'utente.

## 3. Regole di business

- **RB-01.** Email, codice fiscale, targa e codice modello sono univoci.
- **RB-02.** I ruoli ammessi sono `CUSTOMER`, `SALESMAN`, `MANAGER`.
- **RB-03.** Un Salesman può riferire al massimo un Manager nel prototipo; un
  Manager può supervisionare zero o più Salesman. L'assegnazione è opzionale
  perché `manager_id` è nullable e la registrazione pubblica non la raccoglie.
- **RB-04.** Un Vehicle appartiene a un solo VehicleModel; un VehicleModel a un
  solo Brand. Brand e modello possono esistere senza veicoli in stock.
- **RB-05.** Un Vehicle ha un solo tipo fra `RENTAL`, `FOR_SALE`,
  `ACQUISITION_REQUEST`.
  Un veicolo `RENTAL` ha tariffa giornaliera; un `FOR_SALE` ha prezzo di
  vendita; un `ACQUISITION_REQUEST` rappresenta il veicolo proposto da un
  Customer.
- **RB-06.** Chilometraggio, prezzi, tariffa, importi e quantità non possono
  essere negativi; la quantità di un ordine è positiva.
- **RB-07.** Un noleggio richiede `startDate < endDate` e non può sovrapporsi a
  un noleggio confermato dello stesso veicolo.
- **RB-08.** Customer e veicolo non possono avere due test drive nello stesso
  slot; il test drive è riferito a un veicolo esistente.
- **RB-09.** Un Payment appartiene a un solo contesto: Rental XOR SaleOrder;
  purpose e contesto devono essere compatibili.
- **RB-10.** Un Salesman consulta e modifica i noleggi che ha preso in carico.
  È un'assunzione da validare perché nel diagramma ER era formulata come
  domanda aperta.
- **RB-11.** La persistenza conserva al massimo uno sconto sostituibile per
  veicolo. La Strategy, se riceve più candidati, seleziona il maggiore e non li
  cumula. Ogni percentuale è in `(0,100)` e l'intervallo è coerente.
- **RB-12.** Solo una proposta `OFFERED` può essere approvata o rifiutata;
  l'esito registra Manager e istante di revisione.
- **RB-13.** Una proposta approvata non produce automaticamente un pagamento
  al Customer: il regolamento dell'acquisizione è fuori scope.
- **RB-14.** Una prenotazione/acquisto riuscita deve evitare che lo stesso
  veicolo resti acquistabile da un altro Customer.
- **RB-15.** Un pagamento fallito non deve confermare Rental o SaleOrder.
- **RB-16.** I dati del mockup e del seed sono fittizi e non rappresentano
  persone, veicoli o transazioni reali.

## 4. Requisiti non funzionali

- **RNF-SEC-01 — Credenziali.** Le password non devono essere salvate in chiaro
  né registrate nei log. Le credenziali DB provengono dall'ambiente.
- **RNF-SEC-02 — Autorizzazione.** Ogni operazione deve controllare il ruolo;
  nascondere un pulsante non è un controllo sufficiente.
- **RNF-DATA-01 — Integrità.** Vincoli di dominio e transizioni critiche devono
  essere protetti sia nel business layer sia, ove possibile, nel database.
- **RNF-DATA-02 — Atomicità.** Creazione di operazione economica, pagamento e
  aggiornamento dello stato devono usare una transazione applicativa coerente.
- **RNF-DATA-03 — Concorrenza.** Prenotazioni, prese in carico e cambi di
  disponibilità devono usare update compare-and-set sullo stato atteso e
  rilevare quando zero righe sono aggiornate.
- **RNF-QUAL-01 — Manutenibilità.** Presentazione, business, dominio e DAO
  devono dipendere in una sola direzione e avere responsabilità distinte.
- **RNF-QUAL-02 — Testabilità.** I servizi dipendono da porte DAO e gateway
  sostituibili, per consentire test isolati senza JavaFX o PostgreSQL reali.
- **RNF-UX-01 — Feedback.** Ogni flusso deve comunicare successo, fallimento o
  annullamento e preservare i dati correggibili dall'utente.
- **RNF-PORT-01 — Ambiente.** La baseline è Java 21, JavaFX 21 e PostgreSQL 16;
  build e test sono orchestrati con Maven.
- **RNF-PRIV-01 — Dati.** Il progetto non include dati personali reali né
  integra circuiti di pagamento esterni.
- **RNF-DOC-01 — Tracciabilità.** Requisiti, casi d'uso, UI, servizi, dati e test
  devono mantenere identificatori coerenti.

## 5. Criteri di accettazione della baseline

Un requisito è considerato implementato soltanto quando:

1. esiste un percorso UI o un'interfaccia applicativa che lo espone;
2. il servizio applicativo applica autorizzazioni e regole di business;
3. la persistenza conserva i dati e i vincoli richiesti;
4. almeno un flusso normale e le alternative critiche sono coperte da test;
5. l'esito reale dei test è riportato in `PIANO_TEST.md`.

La presenza del requisito in questo documento non prova da sola la sua
implementazione.

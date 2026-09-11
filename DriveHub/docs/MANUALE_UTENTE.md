# Manuale utente

## 1. Ambito e stato

DriveHub è un prototipo desktop per Customer, Salesman e Manager. I passaggi
seguenti descrivono il comportamento implementato e coperto dai test
end-to-end. Il pagamento è
simulato: non inserire numeri di carta o dati reali.

## 2. Prerequisiti e avvio

- JDK 21 e Maven compatibile;
- Docker/Compose per PostgreSQL 16, oppure un'istanza PostgreSQL equivalente;
- variabili indicate in `.env.example`, copiate in un file locale non versionato.

Procedura:

1. dalla directory `DriveHub`, configurare URL, utente e password del database;
2. avviare PostgreSQL con `docker compose up -d` (il servizio usa
   `compose.yaml` e l'immagine PostgreSQL 16);
3. verificare che migration/seed siano applicati dalla procedura di bootstrap;
4. avviare l'applicazione con `mvn javafx:run`;
5. al termine chiudere l'applicazione e, se desiderato, il servizio con
   `docker compose down`.

Il bootstrap applica le migration una sola volta. Il seed dimostrativo è
facoltativo e contiene soltanto catalogo/account fittizi; non usare queste
credenziali fuori da un ambiente locale. Per verificare automaticamente lo
stack isolato usare `bash scripts/test_full_stack.sh`.

## 3. Accesso

### Registrazione

Da Welcome o Login scegliere “Registrati”, inserire codice fiscale, nome,
cognome, email, telefono, password e ruolo, quindi confermare. Dopo il successo
la sessione viene aperta e l'utente è portato direttamente al workspace del
ruolo. In caso di errore correggere i campi evidenziati; la password viene
richiesta nuovamente.

La selezione pubblica di Salesman/Manager è mantenuta nel prototipo per
coerenza con i diagrammi. Non è una policy adatta alla produzione, dove lo
staff dovrebbe essere creato da un amministratore.

### Login e logout

Inserire email e password. Un login riuscito apre Customer, Salesman o Manager
Workspace in base al ruolo persistito. Un errore non deve rivelare se esiste
l'email. “Logout” chiude la sessione e torna a Welcome.

## 4. Customer Workspace

- **Catalogo:** cercare e filtrare i veicoli, quindi selezionare un elemento.
- **Test drive:** scegliere veicolo e slot futuro; l'invio crea una richiesta
  `REQUESTED`, che un Salesman dovrà confermare.
- **Noleggia:** scegliere date valide, controllare il preventivo e completare il
  pagamento dimostrativo. La richiesta resta `REQUESTED` fino alla presa in
  carico dello staff.
- **Prenotazioni:** vedere solo i propri noleggi e annullare quelli consentiti.
- **Vendi un veicolo:** inviare dati e importo richiesto. Il concessionario
  formula successivamente un'offerta; non è una vendita immediata.
- **Prenota/acquista:** su un veicolo in vendita, scegliere acconto o saldo e
  completare il dialog di pagamento. L'ordine passa a `DEPOSIT_PAID` o `PAID`.

## 5. Salesman Workspace

- **Test drive:** selezionare una richiesta; confermare, avviare e completare in
  ordine. Un comando non valido per lo stato corrente deve essere rifiutato.
- **Noleggi:** prendere in carico una richiesta libera, poi confermare, avviare
  e completare. Il Salesman opera sui noleggi di propria competenza.
- **Proposte:** esaminare un veicolo proposto e formulare l'importo; l'offerta
  `OFFERED` passa alla coda Manager.
- **Inventario:** consultare e modificare soltanto transizioni compatibili con
  prenotazioni e operazioni attive.

## 6. Manager Workspace

- **Dashboard:** aggiornare indicatori e attività, interpretandoli nel periodo
  mostrato.
- **Nuovi veicoli:** indicare marchio, modello, anno, quantità e costo; l'ordine
  nasce `PLACED`.
- **Prezzi/promozioni:** modificare prezzo o tariffa pertinente e gestire lo
  sconto del veicolo.
- **Approvazioni:** selezionare una proposta `OFFERED`, quindi approvare o
  rifiutare. L'azione è terminale e registra il Manager.

## 7. Pagamento dimostrativo

Il dialog P-40 è caricato da `PaymentDialog.fxml`, mostra importo e riferimento
non sensibile e consente un metodo simulato. Produce tre esiti UI distinti:
successo, fallimento o annullamento. Il successo
porta Payment a `COMPLETED`; il fallimento a `FAILED`; la chiusura non deve
essere comunicata come successo. Riprovare la stessa richiesta non deve
creare un secondo saldo quando l'importo dovuto è già coperto.

## 8. Problemi comuni

| Sintomo | Controllo |
|---|---|
| connessione DB fallita | container attivo, URL/porta/credenziali e log non sensibili |
| migration rifiutata | database vuoto e literal SQL allineati agli enum finali |
| login negato | formato email, password, account attivo; non usare dati inventati |
| operazione disabilitata | selezione e stato corrente dell'elemento |
| conflitto su slot/veicolo | aggiornare l'elenco e scegliere altro intervallo |
| JavaFX non parte | JDK 21, dipendenze Maven e ambiente grafico |

Gli stack trace completi sono materiale diagnostico e non devono comparire nei
dialog destinati all'utente.

## 9. Checklist manuale prima della presentazione

- compilare nomi e matricole nel frontespizio della relazione;
- avviare con JDK 21 e un database locale vuoto, quindi accedere con i tre ruoli;
- provare tastiera, focus, ridimensionamento e leggibilità sul monitor d'esame;
- eseguire un noleggio e una vendita mostrando conferma, annullamento e rifiuto;
- illustrare il limite del gateway simulato senza inserire dati di pagamento;
- rieseguire `bash scripts/test_full_stack.sh` se cambiano codice, SQL, FXML,
  POM o Compose.

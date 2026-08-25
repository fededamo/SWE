# Assunzioni e limiti

## 1. Assunzioni minime

| ID | Assunzione | Motivazione | Impatto/reversibilità |
|---|---|---|---|
| A-01 | `User` è l'identità comune; Customer, Salesman e Manager sono ruoli, non tabelle separate | Use case generalizza i tre attori e l'ER duplica gli stessi attributi | Un ruolo aggiuntivo richiede enum, policy e UI, non una nuova gerarchia DB |
| A-02 | `Vehicle` unifica vendita, noleggio e veicolo proposto mediante `vehicleType` | Seconda bozza ER e package `vehicles` | È possibile sostituire il tipo con sottotipi senza cambiare i casi d'uso |
| A-03 | `Sell vehicle` significa che il Customer offre il proprio veicolo al concessionario | Catena Customer → proposta Salesman → approvazione Manager | Evita di confonderlo con la vendita dal catalogo |
| A-04 | Il pagamento è simulato e registra solo metodo/esito tecnico non sensibile | Mockup e requisito universitario, assenza di provider | Un adapter può integrare in seguito un provider reale |
| A-05 | Nel prototipo la registrazione può mostrare la scelta del ruolo | Use case e mockup di registrazione includono il ruolo | In produzione Salesman e Manager devono essere creati da un amministratore |
| A-06 | Un Payment si riferisce a Rental oppure SaleOrder, mai a entrambi | `Process payment` è incluso in due flussi distinti | Il vincolo XOR è esplicito nel modello dati |
| A-07 | Promozioni applicabili allo stesso veicolo non si cumulano: vale quella con percentuale maggiore | `StandardPricingStrategy` seleziona il massimo fra gli sconti applicabili | Consente storico e sovrapposizioni deterministiche |
| A-08 | Ogni Rental operativo è assegnato a un Salesman; solo l'assegnatario lo modifica | Attributo salesman nell'ER e domanda aperta sull'ownership | La politica di assegnazione è separabile; la regola deve essere confermata |
| A-09 | L'intervallo di noleggio usa giorni civili e l'estremo finale è esclusivo per il calcolo | L'ER fornisce solo data inizio/fine | La convenzione deve essere resa visibile nell'interfaccia |
| A-10 | Una prenotazione/acquisto usa `SaleOrder` e può rappresentare acconto o saldo | Casi d'uso accorpano “Reserve/purchase” e l'ER contiene Acconto | Gli stati distinguono `RESERVED`, `DEPOSIT_PAID`, `PAID` e `COMPLETED` |
| A-11 | `StockOrder` registra l'ordine, non un fornitore | Il caso d'uso cita solo “Order new vehicles” | Nessuna entità Supplier è aggiunta senza fonte |
| A-12 | La dashboard aggrega dati già presenti e non introduce contabilità completa | Mockup Manager e use case `View dashboard` | Le metriche sono informative, non un bilancio fiscale |
| A-13 | Il flusso UI noleggio/vendita usa transazioni di servizio separate e una cancellazione compensativa se il pagamento fallisce | `ServiceUiGateway` orchestra servizi con proprie `UnitOfWork` | Un arresto fra i passi può lasciare una richiesta da riconciliare; una transazione applicativa unica sarebbe più forte |

## 2. Informazioni mancanti

- autore/autori e matricole: usare `[DA INSERIRE]`;
- data e modalità esatta della discussione;
- policy definitiva di provisioning degli account staff;
- algoritmo definitivo di assegnazione dei noleggi ai Salesman;
- regole commerciali per importo dell'acconto, rimborso e scadenza della
  prenotazione;
- durata degli slot di test drive, orari di apertura e fuso operativo;
- valuta configurabile: la baseline usa EUR, senza requisito multivaluta;
- gestione della consegna/rientro fisico del veicolo e danni;
- integrazione con fornitori o circuiti di pagamento;
- requisiti legali, fiscali, privacy e conservazione documentale;
- deploy target oltre all'ambiente locale.

Queste lacune non sono colmate inventando processi. Dove impediscono una
decisione, la funzionalità è lasciata configurabile o marcata da completare.

## 3. Fuori scope

- sedi multiple e trasferimento veicoli fra sedi;
- anagrafica fornitori;
- officina, manutenzione, garanzie e post-vendita;
- assicurazione, multe, danni e deposito cauzionale del noleggio;
- finanziamenti, rate, fatturazione e contabilità;
- documenti contrattuali con firma digitale;
- notifiche email/SMS reali;
- pagamento o rimborso reale;
- analisi predittiva e raccomandazioni automatiche.

## 4. Limiti di verifica attuali

- I sorgenti PlantUML documentano il design intenzionale; devono essere
  confrontati con le classi finali.
- I mockup originali sono parziali e non provano l'implementazione delle pagine.
- Gli script SQL descrivono il modello relazionale, ma l'applicazione effettiva
  delle migration deve essere verificata nell'ambiente finale.
- `PaymentDialog.fxml` e il relativo controller sono presenti, ma il flusso
  Customer corrente usa un `ChoiceDialog`; l'integrazione della vista dedicata
  deve essere completata o la risorsa va dichiarata estensione futura.
- La suite offline è stata eseguita con esito 43/43; PostgreSQL 16 live, prova
  GUI e coverage JaCoCo su JDK 21 non sono stati eseguiti nell'ambiente corrente.
- Le credenziali demo non sono inventate in questo documento: se aggiunte,
  devono essere esclusivamente fittizie e dichiarate nel manuale.

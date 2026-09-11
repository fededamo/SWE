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
| A-07 | Il prototipo conserva un solo record promozione sostituibile per veicolo; la Strategy seleziona comunque il massimo fra i candidati ricevuti | Unique SQL e `PricingService.applyDiscount`; `StandardPricingStrategy` resta indipendente dalla cardinalità della persistenza | Uno storico futuro può rimuovere la unique senza cambiare la Strategy |
| A-08 | Ogni Rental operativo è assegnato a un Salesman; solo l'assegnatario lo modifica | Attributo salesman nell'ER e domanda aperta sull'ownership | La politica di assegnazione è separabile; la regola deve essere confermata |
| A-09 | L'intervallo di noleggio usa giorni civili e l'estremo finale è esclusivo per il calcolo | L'ER fornisce solo data inizio/fine | La convenzione deve essere resa visibile nell'interfaccia |
| A-10 | Una prenotazione/acquisto usa `SaleOrder` e può rappresentare acconto o saldo | Casi d'uso accorpano “Reserve/purchase” e l'ER contiene Acconto | Gli stati distinguono `RESERVED`, `DEPOSIT_PAID`, `PAID` e `COMPLETED` |
| A-11 | `StockOrder` registra l'ordine, non un fornitore | Il caso d'uso cita solo “Order new vehicles” | Nessuna entità Supplier è aggiunta senza fonte |
| A-12 | La dashboard aggrega dati già presenti e non introduce contabilità completa | Mockup Manager e use case `View dashboard` | Le metriche sono informative, non un bilancio fiscale |
| A-13 | Il gateway di pagamento è simulato, locale e privo di effetti esterni | `CheckoutService` mantiene operazione e Payment nella stessa `UnitOfWork` | Un provider reale richiederebbe idempotenza e riconciliazione; non basta tenere aperta una transazione DB |

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

- I 17 sorgenti PlantUML sono stati confrontati manualmente e renderizzati; il
  controllo sintattico non dimostra da solo la correttezza semantica UML.
- I mockup originali restano parziali. Gli screenshot finali derivano da stage
  JavaFX reali, ma non sostituiscono una prova umana su più monitor.
- Migration, constraint, query e concorrenza sono stati verificati su
  PostgreSQL 16.15 effimero; non è stata svolta una prova di carico o durata.
- `PaymentDialog.fxml` è cablato nei flussi noleggio e vendita e coperto da
  smoke test. Il precedente `ChoiceDialog` resta soltanto per scegliere fra
  acconto e acquisto totale, non per raccogliere il metodo di pagamento.
- La suite full-stack su JDK 21 ha superato 109/109 test e prodotto JaCoCo. Il
  limite GUI residuo è l'assenza di una sessione manuale con mouse/tastiera su
  desktop fisico; si veda `VERIFICA_FINALE.md`.
- Il seed contiene solo catalogo fittizio e nessuna credenziale applicativa.

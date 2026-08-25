# Dichiarazione sull'uso dell'intelligenza artificiale

## 1. Uso effettuato

Nel lavoro documentato è stato usato un assistente di intelligenza artificiale
come supporto a:

- inventario e lettura comparativa di materiale didattico, diagrammi e progetto
  ChargeNet;
- individuazione di conflitti terminologici e lacune di tracciabilità;
- proposta e revisione di requisiti, casi d'uso, piano di test e relazione;
- stesura manuale di sorgenti PlantUML a partire dalla specifica;
- supporto alla progettazione e allo scaffolding del codice, soggetto a review.

Non è corretto attribuire all'AI validazione accademica, esecuzione dei test o
approvazione delle scelte: restano responsabilità degli autori.

## 2. Materiale fornito e tutela dei dati

Sono stati usati file locali del progetto, diagrammi, mockup, note didattiche e
il progetto ChargeNet dei colleghi come riferimento metodologico. Non sono
stati intenzionalmente introdotti credenziali, dati personali reali o strumenti
di pagamento. Dati di esempio e nominativi UI vanno mantenuti fittizi.

## 3. Controlli adottati

- fonti originali trattate in sola lettura;
- ChargeNet usato per struttura e metodo, non per copia del dominio;
- conflitti registrati invece di risolverli silenziosamente;
- nomi e stati verificati, per quanto possibile, su dominio, UI e schema;
- risultati di test non dichiarati se non eseguiti;
- risultati e limiti riportati separando evidenza statica, suite eseguita e
  verifiche ambientali non disponibili;
- diagrammi UML scritti dalla specifica, non generati automaticamente dal
  codice, come richiesto dalle note di progetto (p. 4).

## 4. Limiti e review umana ancora richiesta

Un modello generativo può inventare API, fraintendere cardinalità o produrre
documenti coerenti tra loro ma non con l'implementazione. Prima della consegna
occorre quindi controllare:

1. firme e nomi effettivi di servizi, DAO e controller;
2. allineamento enum ↔ constraint SQL ↔ seed;
3. build, test e flussi UI su ambiente pulito;
4. originalità, correttezza e comprensione da parte di tutti gli autori;
5. dati di frontespizio e registro dei prompt.

## 5. Registro sintetico

| Data | Strumento/modello | Attività | Input principali | Review umana/esito |
|---|---|---|---|---|
| 2026-08-23 | Codex/OpenAI, modello GPT-5 | analisi, progettazione assistita e redazione | note, diagrammi, mockup, ChargeNet, repository DriveHub | review statica e suite 43/43; review accademica finale agli autori |

Conservare, se richiesto dal docente, i prompt rilevanti o un loro riepilogo
senza segreti. La dichiarazione va aggiornata dagli autori per descrivere
fedelmente ogni uso successivo.

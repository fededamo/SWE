# Dichiarazione sull'uso dell'intelligenza artificiale

## 1. Uso effettuato

Nel lavoro documentato è stato usato un assistente di intelligenza artificiale
come supporto a:

- inventario e lettura comparativa di materiale didattico, diagrammi e progetto
  ChargeNet;
- individuazione di conflitti terminologici e lacune di tracciabilità;
- proposta e revisione di requisiti, casi d'uso, piano di test e relazione;
- stesura manuale di sorgenti PlantUML a partire dalla specifica;
- supporto alla progettazione, implementazione e revisione di codice e test,
  con successiva verifica automatica e review sui diff.

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
- risultati di test dichiarati soltanto dopo esecuzione e conservati con log,
  XML Surefire, coverage e fingerprint dei sorgenti;
- risultati e limiti riportati separando evidenza statica, suite eseguita e
  verifiche ambientali non disponibili;
- diagrammi UML scritti dalla specifica, non generati automaticamente dal
  codice, come richiesto dalle note di progetto (p. 4).

## 4. Limiti e review umana richiesta

Un modello generativo può inventare API, fraintendere cardinalità o produrre
documenti coerenti tra loro ma non con l'implementazione. Per questo sono stati
controllati automaticamente e mediante review firme e nomi del codice,
allineamento enum ↔ SQL ↔ seed, build, test, rendering UML e flussi UI in
ambiente isolato.

Restano invece agli autori la responsabilità accademica, la comprensione e
difesa orale di ogni scelta, la verifica di originalità, la compilazione dei
dati di frontespizio e la validazione col docente delle decisioni aperte. La
prova JavaFX automatica sotto Xvfb non sostituisce una sessione umana su desktop
fisico.

## 5. Registro sintetico

| Data | Strumento/modello | Attività | Input principali | Review umana/esito |
|---|---|---|---|---|
| 2026-08-23 | Codex/OpenAI, modello GPT-5 | analisi iniziale, progettazione assistita e redazione | note, diagrammi, mockup, ChargeNet, repository DriveHub | baseline iniziale riesaminata; nessuna approvazione accademica attribuita all'AI |
| 2026-09-07/11 | Codex/OpenAI, agente di coding | audit end-to-end, correzioni, test, UML, tracciabilità e relazione | fonti locali in sola lettura e workspace DriveHub | 109 test full-stack 0/0/0; PostgreSQL 16.15; 17 UML renderizzati; review finale agli autori |

Conservare, se richiesto dal docente, i prompt rilevanti o un loro riepilogo
senza segreti. La dichiarazione va aggiornata dagli autori per descrivere
fedelmente ogni uso successivo.

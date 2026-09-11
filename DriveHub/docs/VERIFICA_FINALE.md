# Registro di verifica finale

## Baseline identificata

- Ultima verifica full-stack: 11 settembre 2026 (Europe/Rome).
- Commit base: `ed03c32289aa663dfc04b1bf0c8fda15da326b47`.
- Il repository contiene modifiche di consegna non ancora committate; la
  baseline eseguibile è quindi identificata anche dal manifest SHA-256
  `e7fb29af5332d2abf13939c12de0462b99c58573cc722cfceb78dba911aeec25`.
- Il manifest comprende `pom.xml`, Compose e tutti i file sotto `src/`; è in
  `docs/evidence/full-stack/source-manifest.json`.

## Risultati riproducibili

| Comando/ambiente | Ambito | Test | Failure / error / skipped | Esito |
|---|---|---:|---:|---|
| `mvn clean test`, OpenJDK 25.0.4.1 host | suite standard, target bytecode 21 | 81 | 0 / 0 / 0 | `BUILD SUCCESS`, JaCoCo generato |
| `docker run … maven:3.9.11-eclipse-temurin-21 mvn -B clean test` | suite standard su JDK target Temurin 21.0.9 | 81 | 0 / 0 / 0 | `BUILD SUCCESS`, JaCoCo generato |
| `docker run … maven:3.9.11-eclipse-temurin-21 mvn -B clean package` | package finale JDK target | 81 | 0 / 0 / 0 | `BUILD SUCCESS` in 25,088 s; `target/drivehub-1.0.0.jar` |
| `bash scripts/test_postgres.sh`, PostgreSQL 16.15 | standard + constraint/query/concorrenza PostgreSQL | 101 | 0 / 0 / 0 | `BUILD SUCCESS`; container e rete rimossi |
| profilo `gui` su JDK 21 + Xvfb/GTK | standard + 7 smoke JavaFX | 88 | 0 / 0 / 0 | `BUILD SUCCESS`, screenshot acquisiti |
| `bash scripts/test_full_stack.sh`, JDK 21 + PostgreSQL 16.15 + Xvfb/GTK | tutti i test, incluso composition root JavaFX→PostgreSQL | 109 | 0 / 0 / 0 | `BUILD SUCCESS` in 29,062 s Maven; container e rete rimossi |
| `PLANTUML_JAR=/tmp/plantuml-1.2026.4.jar bash scripts/render_diagrams.sh` | 17 sorgenti PlantUML intenzionali | — | — | check sintattico e rendering SVG/PNG riusciti |
| `python3 scripts/build_report.py` | relazione autonoma con figure e snippet | — | — | PDF 33 pagine A4, 19 figure entro i margini; testo/immagini ispezionati |

Gli avvisi Maven sulle metadata JavaFX e l'avviso “classes were loaded from
unnamed module” non hanno prodotto failure; il progetto usa classpath con
`useModulePath=false`. Mockito 5.20 è caricato come javaagent, perciò non usa il
self-attach dinamico di Byte Buddy. JaCoCo 0.8.14 ha strumentato le 100 classi
analizzate sia su JDK 21 sia su JDK 25.

## Coverage full-stack JaCoCo

| Metrica | Coperti | Mancanti | Totale | Percentuale |
|---|---:|---:|---:|---:|
| Istruzioni | 12.604 | 3.479 | 16.083 | 78,37% |
| Branch | 687 | 408 | 1.095 | 62,74% |
| Linee | 2.532 | 603 | 3.135 | 80,77% |
| Complessità | 1.019 | 560 | 1.579 | 64,53% |
| Metodi | 825 | 200 | 1.025 | 80,49% |
| Classi | 94 | 6 | 100 | 94,00% |

La percentuale non è un criterio di correttezza. I test privilegiano
transizioni, autorizzazioni, ownership, rollback, vincoli e race condition; non
sono stati aggiunti test di getter/setter per gonfiare il valore.

## PostgreSQL verificato

Il database viene creato vuoto su `postgres:16-alpine`, riportato dal server
come PostgreSQL 16.15. I test hanno verificato:

- applicazione ordinata delle cinque migration e bootstrap/seed idempotente;
- 22 foreign key e indici attesi, unique, check su ruoli, enum, importi, date,
  XOR Payment e coerenza degli stati;
- corrispondenza automatica fra undici enum Java e literal dei CHECK reali;
- query di catalogo, ownership, overlap e mapping con round-trip;
- partial unique index PostgreSQL e riuso di slot annullati;
- rollback dopo errore SQL e dopo approvazione del gateway simulato;
- competizioni simultanee su vendita, saldo, noleggio, test drive, claim e
  decisione proposta, con un solo vincitore dove richiesto.

H2 resta un doppio rapido per la suite standard e non viene presentato come
prova equivalente a PostgreSQL.

## GUI verificata

Gli otto test della classe `JavaFxSmokeTest` hanno istanziato controlli e stage
JavaFX reali sotto Xvfb. Sono stati controllati caricamento e routing dei tre
ruoli, guard delle route, logout/sessione, stati vuoti, validazioni, protezione
dal doppio click, apertura reale di `PaymentDialog.fxml`, annullamento,
successo/rifiuto e checkout persistito tramite il composition root su
PostgreSQL. Gli screenshot in `docs/screenshots/` sono prodotti da tali stage.

Limite residuo: non è stata eseguita una sessione manuale con mouse e tastiera
su un desktop fisico. Gli screenshot sono stati ispezionati, ma accessibilità,
ridimensionamento su monitor diversi e percezione d'uso richiedono comunque la
checklist manuale del manuale utente.

## Relazione verificata

`Relazione_SWE/DriveHub_Relazione.pdf` è stato generato dallo stesso sorgente
Markdown tramite una pipeline HTML→ODT→PDF. Lo script ridimensiona nel documento
le cornici delle immagini preservando rapporto e pixel: `pdfimages` riporta
circa 212 ppi per i diagrammi principali. `pdfinfo` conferma 33 pagine A4; il
testo estratto non contiene placeholder tecnici obsoleti e le pagine sono state
controllate anche come contact sheet. Restano intenzionalmente i due placeholder
personali `[DA INSERIRE]` per autore/i e matricola/e.

## Evidenze conservate

`docs/evidence/full-stack/` contiene:

- log Maven sanitizzato;
- 20 XML Surefire privati di proprietà JVM e output locali;
- `jacoco.xml`;
- `summary.json` con versioni, totali e coverage;
- `source-manifest.json` con hash dei sorgenti verificati.

I risultati descrivono esattamente la baseline del manifest. Una modifica a
codice, SQL, FXML, POM o Compose invalida l'evidenza e richiede una nuova
esecuzione dello script.

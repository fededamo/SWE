# DriveHub

DriveHub è un’applicazione desktop JavaFX per un concessionario che vende,
noleggia e acquisisce veicoli. Il progetto è stato realizzato per l’esame di
Ingegneria del Software, A.A. 2025/2026.

Sono implementati i tre ruoli del modello dei casi d’uso:

- Customer: catalogo, test drive, noleggio e pagamento simulato,
  prenotazione/acquisto e proposta di vendita del proprio veicolo;
- Salesman: presa in carico e gestione esclusiva di test drive, noleggi e
  proposte, più gestione dello stato inventario;
- Manager: dashboard, ordini stock, prezzi/promozioni e decisione sulle
  proposte di acquisto.

## Avvio rapido

Prerequisiti: JDK 21, Maven 3.9+, Docker con Compose.

```bash
cp .env.example .env
# cambiare almeno DRIVEHUB_DB_PASSWORD nel file .env
docker compose up -d
set -a
source .env
set +a
mvn clean javafx:run
```

Al primo avvio le migrazioni vengono applicate automaticamente. Con
`DRIVEHUB_DB_SEED_DEMO=true` vengono inseriti soltanto marca, modelli e due
veicoli dimostrativi: nessuna password o identità personale è inclusa. Creare
quindi un account dalla schermata **Registrati**.

> In questo prototipo accademico è possibile registrare tutti e tre i ruoli,
> come mostrato nel mockup e nella generalizzazione UML. In produzione,
> Salesman e Manager dovrebbero essere creati da un amministratore.

Per arrestare il database:

```bash
docker compose down
```

`docker compose down -v` elimina anche il volume dati e non è necessario per il
normale utilizzo.

## Configurazione

La configurazione è letta da variabili d’ambiente o proprietà JVM. Le proprietà
JVM hanno precedenza.

| Variabile | Proprietà JVM | Default |
|---|---|---|
| `DRIVEHUB_DB_URL` | `drivehub.db.url` | `jdbc:postgresql://localhost:5432/drivehub` |
| `DRIVEHUB_DB_USER` | `drivehub.db.user` | `drivehub` |
| `DRIVEHUB_DB_PASSWORD` | `drivehub.db.password` | obbligatoria |
| `DRIVEHUB_DB_SEED_DEMO` | `drivehub.db.seed-demo` | `false` |

Le credenziali reali non devono essere versionate. `.env` è ignorato da Git.

## Verifica

```bash
mvn clean test
mvn package
bash scripts/test_postgres.sh       # PostgreSQL 16 reale, database effimero
bash scripts/test_full_stack.sh     # JDK 21 + PostgreSQL 16 + JavaFX/Xvfb
```

La suite standard copre dominio, servizi, contratti FXML, vincoli
architetturali e DAO in H2/PostgreSQL mode; H2 non è considerato equivalente a
PostgreSQL. Gli script dedicati verificano migration, constraint, query,
concorrenza e rollback su PostgreSQL 16 e i flussi JavaFX reali sotto Xvfb. Il
target ufficiale è Java 21. Mockito è caricato esplicitamente come javaagent,
evitando il self-attach di Byte Buddy; JaCoCo 0.8.14 funziona anche sulla JDK 25
presente nell'ambiente di revisione.

Baseline verificata l'11 settembre 2026: 81 test standard, 101 con PostgreSQL,
88 con JavaFX e 109 full-stack, sempre con 0 failure/error/skipped. Evidenze e
limiti sono in [`docs/VERIFICA_FINALE.md`](docs/VERIFICA_FINALE.md); la relazione
consegnabile è `Relazione_SWE/DriveHub_Relazione.pdf`.

## Struttura

```text
src/main/java/.../presentation   JavaFX, controller, navigazione, UI gateway
src/main/java/.../business       servizi applicativi, Strategy, sicurezza
src/main/java/.../domain         entità, invarianti, stati, Observer
src/main/java/.../dao            porte e adapter JDBC PostgreSQL
src/main/resources/db            migrazioni e dati demo non sensibili
docs/                            analisi, UC, tracciabilità, test e diagrammi
Relazione_SWE/                   relazione consegnabile e PDF
```

Indice documentale: [`docs/README.md`](docs/README.md). Relazione principale:
[`Relazione_SWE/relazione.md`](Relazione_SWE/relazione.md).

Autore/i e matricola/e: **[DA INSERIRE]**.

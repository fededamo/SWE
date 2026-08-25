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
```

La suite copre dominio, servizi, contratti FXML, vincoli architetturali,
migrazioni e query DAO in H2/PostgreSQL mode. Il target ufficiale è Java 21;
su JDK 25 il profilo Maven `jdk-25-verification` disattiva automaticamente la
sola strumentazione JaCoCo incompatibile, senza saltare i test.

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

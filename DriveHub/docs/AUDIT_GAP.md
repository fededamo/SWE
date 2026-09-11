# Audit verificabile e decisioni di revisione

Baseline osservata il 7 settembre 2026. Le evidenze descrivono difetti trovati nella versione iniziale; le soluzioni descrivono la revisione richiesta. Un intervento scritto non equivale a un controllo passato: il registro di esecuzione è `VERIFICA_FINALE.md`.

## Fonti, autorità e criteri

L'ordine applicato è: **materiale ufficiale del corso**, diagrammi originali `Diagrammi/`, implementazione e documentazione DriveHub, ChargeNet come benchmark di qualità, best practice compatibili. I riferimenti alle note sono pagine fisiche PDF: il numero stampato è spesso inferiore di uno. Per le dispense con due slide per foglio sono indicati entrambi quando utile.

| Codice | Fonte locale e criterio verificato | Applicazione |
|---|---|---|
| C01 | NotesOnTheProjectWork_Jan2025, pp. 3–4: padronanza dell'intero lavoro, documentare AI, divieto UML da codice | Modelli intenzionali corretti manualmente; registro AI e discussione orale |
| C02 | Note pp. 10–12: frontespizio, indice, statement applicativo, package/responsabilità/strumenti | Eliminata l'interpretazione errata di statement come dichiarazione firmata |
| C03 | Note pp. 13, 22; UseCaseDiagsAndTemplates, fogli 12, 14–18 | Template con livello, attori esterni, trigger, garanzie, passi e alternative originate |
| C04 | Note pp. 24, 27, 35 | Snippet motivati e osservazioni interpretative delle classi |
| C05 | Note pp. 26–29 | Test funzionali via API business ammessi; unit strutturali; DAO/query distinti |
| C06 | Note pp. 30, 33, 45 | Modello relazionale/ER e interfaccia con mockup o screenshot |
| C07 | UmlClassDiagramsImplementationPerspective20, fogli 4–7 (slide 8–14) | Frecce di navigabilità, associazioni per riferimenti stabili, dipendenze uses/creates, niente composizioni arbitrarie |
| C08 | DesignPatternsJava22, fogli 2–7, discussione Observer; DesignPatternsJava20/22, sezioni Strategy | Problema, partecipanti, collaborazione, beneficio e limiti dei pattern reali |
| C09 | Software Testing Principles and Java Testing Tools, pp. 18–20, 23–25 | White/black/gray-box, oracolo, selezione e copertura distinta dall'assenza di difetti |
| C10 | Lifecycles20, pp. 11, 24; ObjectOrientedAnalysis20, sezioni responsabilità e specifica | Separare verifica tecnica da validazione delle assunzioni e iterare sui rischi |

Il docente **Enrico Vicario** è attestato nel frontespizio delle note di gennaio. Le note disponibili recano A.A. 2024/2025; il progetto è documentato per A.A. 2025/2026. Non viene inventato un disciplinare successivo.

## Benchmark ChargeNet: profondità e struttura

Sono stati confrontati `Relazione_SWE/main.tex`, l'indice e i capitoli di `main.pdf`/`chargenet_text.txt`, `capitoli/2_progettazione.tex`, `3_implementazione.tex`, `4_testing.tex` e i template. Il benchmark è narrativo: non trasferisce attori, entità, algoritmi o decisioni commerciali.

| Aspetto del benchmark | Cosa rende utile la presentazione | Traduzione autonoma in DriveHub |
|---|---|---|
| Template selezionati | Flusso, stato finale, alternativa e test nello stesso contesto | Tutti gli UC formalizzati; noleggio, pagamento e proposta sviluppati nella relazione |
| Mockup per ruolo e navigazione | Contenuti e controlli prima della struttura interna | Pagine P-00…P-40, navigazione per ruolo, screenshot reali separati dai mockup originali |
| Package/classi/sequenze/ER | Viste statiche e dinamiche collegate | Quattro viste di classi, confini UnitOfWork nelle sequenze e cardinalità effettive |
| Implementazione | Snippet selezionati e loro conseguenza sui comportamenti | CAS, rollback, residuo, iniezione del clock, mapping JDBC |
| Pattern | Partecipanti e cooperazione sul problema concreto | Strategy prezzo, Observer inventario, DAO, Unit of Work e Gateway realmente presenti |
| Testing | Motivi del test, fixture, metodo, oracolo | FT via servizi, UT di invarianti, IT H2, prove PostgreSQL e JavaFX esplicitamente distinte |

Le immagini del benchmark larghe `1.10\\textwidth` non sono considerate una buona regola editoriale: in DriveHub si controllano margini e leggibilità. Non si riutilizzano nominativi o matricole dei suoi autori.

## Matrice dei gap

| ID / gravità | Problema ed evidenza iniziale | Criterio | Soluzione motivata | File interessati | Verifica richiesta |
|---|---|---|---|---|---|
| G01 P0 | JDK25: 43 test, 9 errori Mockito self-attach; documenti affermavano 43/43 | RNF-PORT-01, C09 | Java21 ufficiale e configurazione Mockito esplicita compatibile | pom.xml; docs/PIANO_TEST.md | clean test e package, versione JDK, XML Surefire |
| G02 P0 | Richiesta, pagamento e compensazione in transazioni UI separate | RNF-DATA-02; RB15 | CheckoutService: unica UnitOfWork; fallimento previsto conserva FAILED+CANCELLED atomicamente, eccezione rollback | business/services; presentation/core; sequence-rent-payment; requisiti | successo, rifiuto, eccezione, rollback e crash boundary |
| G03 P1 | Report e matrice dichiaravano PaymentDialog assente pur essendo caricato nei controller | RF-PAY-01, C03 | Verificare e mantenere cablaggio; prezzo esplicito, annullamento prima del comando | controller; FXML; manuale; navigazione | FXMLLoader e azioni dialog, prova JavaFX |
| G04 P1 | Firme fittizie nelle sequenze: SalesService.requestAcquisition, TestDriveService.request, UnitOfWork.begin | C01/C07 | Sequenze manuali con oggetti e metodi reali; DaoFactory.begin | docs/diagrams/sequence-*.puml | confronto sorgenti e rendering |
| G05 P1 | Servizi/DB trattati come attori; login user goal automatico; 2a duplicata | C03 | Confine DriveHub esplicito; funzione login/logout; alternative uniche col passo origine | USE_CASE_TEMPLATES; use-cases | audit template e ID |
| G06 P1 | Doc su promozioni multiple mentre SQL uq_discounts_vehicle e PricingService.replace ammettono 0..1 | RB11, C06 | Documentare un record sostituibile per veicolo; massimo della Strategy è selezione dei candidati | requisiti; ER; architettura; relazione | SQL unique, test sostituzione/prezzo |
| G07 P1 | RB03 e ER imponevano manager obbligatorio, manager_id è nullable; Java conserva Long | C07, RB03 | Supervisore opzionale in prototipo, nessuna falsa navigazione User→User in vista implementativa | requisiti; class-domain; ER; assunzioni | costruttore User, FK e registrazione staff |
| G08 P1 | RF-ACQ03 richiede istante decisione assente da proposal/SQL | requisito effettivo | Aggiungere decided_at tramite migrazione e mapping | V003; PurchaseProposal; DAO; ER | approva/rifiuta, round-trip e vincolo stato |
| G09 P1 | Letture prima di update espongono saldo/stato ad aggiornamenti concorrenti | RNF-DATA-03, RB14 | Lock di riga e CAS per claim/transizioni; prevenire doppio saldo/vendita | servizi; DAO; test | due transazioni concorrenti PostgreSQL |
| G10 P1 | FT solo catalogo, UT/IT senza metodi; metriche storiche sparse | C05/C09, RNF-DOC-01 | Identificare test per flusso; matrice di evidenza con metodo e stato | PIANO_TEST; MATRICE_TRACCIABILITA | controllo automatico ID e Surefire |
| G11 P1 | Test DAO H2 chiamati prova PostgreSQL; nessun server live dimostrato | C05/C06 | Prove PostgreSQL16 dedicate, H2 etichettato esplicitamente | test DAO; script; registro finale | migration vuota, FK/unique/check/query/rollback |
| G12 P1 | ServiceUiGateway mescolava conversione DTO e orchestrazione economica | RNF-QUAL-01 | Estrazione mapper e checkout nel business; architettura controllata | UiModelMapper; CheckoutService; LayeringTest | dipendenze vietate e API controller |
| G13 P1 | Observer notificava prima del commit e utilità operativa non dimostrata | C08 | Eventi pubblicati dopo commit e consumer esplicito, oppure limite dichiarato | InventoryService; presentation/core | commit prima della notifica; rollback senza evento |
| G14 P1 | Relazione senza immagini effettive/snippet e con risultati non aggiornati | C02/C04/C06 | Documento autonomo con UC selezionati, figure/caption e prove esatte | Relazione_SWE; script report | HTML/PDF, immagini, font, indice e margini |
| G15 P2 | Autorità fonti errata; statement confuso; inventario autocontraddittorio | C02/C10 | Gerarchia utente; statement applicativo; conteggi con comando verificabile | ANALISI_PRELIMINARE; INVENTARIO_FONTI | confronto diretto fonti immutate |
| G16 P1 | Registrazione pubblica staff e policy commerciali non approvate | A05/A08/A10 | Conservare scope prototipo e raccogliere decisioni da validare | ASSUNZIONI_E_LIMITI; relazione | review degli autori/docente, senza inventare assenso |

P0 blocca eseguibilità o coerenza transazionale, P1 blocca una difesa verificabile del requisito, P2 migliora precisione/leggibilità. Il registro finale distingue problemi risolti da decisioni umane e limiti ambientali: la gravità iniziale non viene usata come prova di esito.

## Chiusura all'11 settembre 2026

| Gap | Esito | Evidenza di chiusura |
|---|---|---|
| G01 | chiuso | Temurin 21.0.9, Mockito javaagent, JaCoCo 0.8.14; test e package |
| G02 | chiuso | `CheckoutService`, sequenze transazionali e test successo/decline/eccezione/rollback |
| G03 | chiuso | `PaymentDialogController` cablato; smoke JavaFX su annullamento, successo e rifiuto |
| G04 | chiuso | cinque sequenze riallineate alle firme e 17 diagrammi renderizzati |
| G05 | chiuso | attori esterni, confine e alternative originate dal passo nei template |
| G06 | chiuso | sconto unico sostituibile, dominio/SQL/documenti e test allineati a `(0,100)` |
| G07 | chiuso tecnicamente | `manager_id` opzionale allineato; policy di provisioning staff resta decisione A05 |
| G08 | chiuso | `decided_at` in migration, dominio, DAO, ER e test PostgreSQL |
| G09 | chiuso | lock/CAS/indici parziali; otto test di concorrenza PostgreSQL |
| G10 | chiuso | matrice con metodi reali; 109 test e XML Surefire archiviati |
| G11 | chiuso | PostgreSQL 16.15 reale: 20 test dedicati fra constraint, DAO e concorrenza |
| G12 | chiuso | `UiModelMapper`, `CheckoutService` e sei regole `LayeringTest` |
| G13 | chiuso | `InventoryActivityFeed`, pubblicazione after-commit e test rollback/unsubscribe |
| G14 | chiuso | relazione autonoma, figure/snippet e PDF verificato; risultati nel registro finale |
| G15 | chiuso | gerarchia delle fonti e statement applicativo esplicitati |
| G16 | aperto alla validazione umana | A05/A08 e policy commerciali restano dichiarate, non inventate |

Durante la chiusura è stata inoltre corretta la semantica dell'acquisto
integrale: usa `SALE_BALANCE`, mentre la riserva usa `SALE_DEPOSIT`; acconto e
saldo devono coincidere esattamente con l'importo residuo richiesto. I test
funzionali coprono anche il tentativo di acconto eccedente.

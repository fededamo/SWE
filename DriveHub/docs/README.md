# Documentazione DriveHub

Questa cartella raccoglie i sorgenti documentali e PlantUML del progetto
universitario DriveHub. I diagrammi sono stati modellati manualmente a partire
dai requisiti e dai diagrammi originali: non sono il risultato di reverse
engineering dal codice.

## Indice

- [Analisi preliminare](ANALISI_PRELIMINARE.md)
- [Inventario ricorsivo delle fonti didattiche](INVENTARIO_FONTI.md)
- [Requisiti](REQUISITI.md)
- [Assunzioni e limiti](ASSUNZIONI_E_LIMITI.md)
- [Template dei casi d'uso](USE_CASE_TEMPLATES.md)
- [Matrice di tracciabilità](MATRICE_TRACCIABILITA.md)
- [Architettura e modello relazionale](ARCHITETTURA_E_DATI.md)
- [Piano di test](PIANO_TEST.md)
- [Registro di verifica finale](VERIFICA_FINALE.md)
- [Manuale utente](MANUALE_UTENTE.md)
- [Documentazione dell'uso di AI](USO_AI.md)
- [Sorgenti dei diagrammi](diagrams/)
- [Relazione principale](../Relazione_SWE/relazione.md)

## Stato del documento

Requisiti, diagrammi e riferimenti architetturali sono allineati alla baseline
dell'11 settembre 2026. La verifica full-stack su JDK 21, PostgreSQL 16 e JavaFX
ha superato 109 test su 109 e ha prodotto JaCoCo; dettagli, fingerprint e limiti
sono nel registro di verifica finale.

## Convenzioni

- `RF-*`: requisito funzionale.
- `RNF-*`: requisito non funzionale.
- `RB-*`: regola di business.
- `UC-*`: caso d'uso.
- `P-*`: pagina o area dell'interfaccia.
- `FT-*`, `UT-*`, `IT-*`: test funzionale, unitario, di integrazione.
- **Verificato**: riscontro diretto in un file presente nel repository.
- **Progettato**: scelta intenzionale non necessariamente coperta da un test.
- **Da decidere**: informazione non determinabile dalle fonti.

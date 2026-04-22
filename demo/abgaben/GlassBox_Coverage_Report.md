# Aufgabe 16 – Glass-Box-Test (ohne GUI-Bewertung)

**Projekt:** KlausurCraft  
**Datum:** 22.04.2026

## Vorgehen
Die Abdeckung wurde mit JaCoCo über Maven gemessen:

```bash
mvn -q -Dmaven.repo.local=/tmp/m2 -Dmaven.compiler.release=21 clean \
  org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent \
  test \
  org.jacoco:jacoco-maven-plugin:0.8.12:report
```

Für die Bewertung in diesem Report habe ich die GUI-Pakete ausgeschlossen:
- `simon.klausurcraft.app`
- `simon.klausurcraft.ui`
- `simon.klausurcraft.ui.components`
- `simon.klausurcraft.ui.home`

## Ergebnis (ohne GUI)
- **Instruction Coverage:** **97,49 %** (3965 / 4067)
- **Branch Coverage:** **86,85 %** (251 / 289)

Damit ist die Kernlogik deutlich über 90% Anweisungsabdeckung.

## Paketwerte (ohne GUI)
- `simon.klausurcraft.task`: 99,14 % / 96,77 %
- `simon.klausurcraft.task.io`: 96,98 % / 84,09 %
- `simon.klausurcraft.task.planning`: 98,61 % / 82,86 %
- `simon.klausurcraft.task.export`: 96,78 % / 80,26 %
- `simon.klausurcraft.shipping`: 98,88 % / 94,59 %

## Umgesetzte Testverbesserungen
- Zusätzliche Fehler- und Sonderfalltests in `TaskXmlStore`.
- Ergänzte Äquivalenzklassen-/Randfalltests in `Points` und `PdfExportRules`.
- Testbarer Export-Flow in `PdfExportService` (inkl. Abbruchfall und Lösungsexport).
- Randomized-Test mit 1000 Fällen für den Porto-Rechner.

## Zusatzinfo
Die reine Gesamtabdeckung inkl. GUI liegt bei 30,73 % / 31,14 %. Dieser Wert ist für die Aufgabenbewertung weniger aussagekräftig, weil die GUI nicht im Fokus der Unit-Tests liegt.

## Abgabe-Dateien
- HTML-Report: `abgaben/jacoco-report-2026-04-22/index.html`
- CSV: `abgaben/coverage-jacoco.csv`
- XML: `abgaben/coverage-jacoco.xml`

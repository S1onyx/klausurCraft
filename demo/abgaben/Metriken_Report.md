# Aufgabe 18 – Metriken

**Projekt:** KlausurCraft  
**Datum:** 22.04.2026

## Vorgehen
Die Metriken habe ich über das Projekt-Skript `scripts/collect-metrics.sh` erhoben und anschließend in Text- und CSV-Form exportiert.

Verwendete Werkzeuge:
- `cloc` für Code-/Kommentarzeilen
- `rg` und Shell-Auswertung für Typen, Methoden und Imports

Scope der Messung: `src/main/java`

## Pflichtmetriken
- Lines of Code (ohne Leer- und Kommentarzeilen): **4163**
- Anzahl Pakete: **8**
- Anzahl Klassen (`class`): **26**

Zusatz:
- Java-Dateien gesamt: 29
- Typen gesamt (`class`, `enum`, `record`, `interface`): 32

## Weitere Projektmetriken
- Kommentaranteil: **4,82 %**
- Erkannte Methoden (heuristisch): **218**
- Durchschnittliche Methodengröße: **19,10 LoC/Method**
- Gesamtzahl Imports: **327**
- Coupling-Proxy (Ø Imports pro Typ): **11,68**
- Datei mit den meisten Imports: `KlausurCraftApp.java` (43)

## Größte Dateien (Top 5)
1. `src/main/java/simon/klausurcraft/ui/home/HomeGenerateFlow.java` (740 Zeilen)
2. `src/main/java/simon/klausurcraft/task/export/PdfExportService.java` (543)
3. `src/main/java/simon/klausurcraft/ui/home/HomeCenterController.java` (476)
4. `src/main/java/simon/klausurcraft/ui/home/HomeController.java` (404)
5. `src/main/java/simon/klausurcraft/task/io/TaskXmlStore.java` (362)

## IDE-/Plugin-Seite (Cohesion/Coupling)
Die Aufgabe verlangt auch den Blick auf IDE-Metriken wie Kohäsion und Kopplung. Dafür habe ich das Vorgehen geprüft:
- IntelliJ: z. B. **MetricsReloaded**
- Eclipse: z. B. **CodeMR**

Damit lassen sich Metriken wie LCOM, Komplexität und detaillierte Kopplung pro Klasse/Paket exportieren.

## Paketübersicht
- `simon.klausurcraft.app`
- `simon.klausurcraft.task`
- `simon.klausurcraft.task.export`
- `simon.klausurcraft.task.io`
- `simon.klausurcraft.task.planning`
- `simon.klausurcraft.ui`
- `simon.klausurcraft.ui.components`
- `simon.klausurcraft.ui.home`

## Exportdateien
- `abgaben/metrics-run.txt`
- `abgaben/metrics-export.csv`
- `scripts/collect-metrics.sh`

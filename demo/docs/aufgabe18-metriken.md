# Aufgabe 18 - Metriken (KlausurCraft)

## 1) Ziel und Vorgehen
Für die Implementierung wurden die geforderten Kernmetriken (Lines of Code, Anzahl Pakete, Anzahl Klassen) erhoben und um weitere Metriken ergänzt (Coupling-Proxy, durchschnittliche Methodengröße, Kommentar-Anteil).

Messzeitpunkt: **01.04.2026, 11:55 CEST**

Werkzeuge:
- `scripts/collect-metrics.sh`
- `cloc` (Code/Kommentar/Blank-Zeilen)
- `rg`/Regex-basierte Auswertung (Typen, Methoden, Imports)

## 2) Kernmetriken (Pflicht)

| Metrik | Ergebnis |
|---|---:|
| Lines of Code (Java, ohne Leer-/Kommentarzeilen) | **2393** |
| Anzahl Pakete | **8** |
| Anzahl Klassen (`class`) | **25** |

Ergänzend zur Einordnung:
- Java-Dateien gesamt: 28
- Java-Typen gesamt (`class + enum + record + interface`): 33

## 3) Erweiterte Metriken

| Metrik | Ergebnis | Hinweis |
|---|---:|---|
| Kommentar-Anteil | **7,36 %** | `comment / (comment + code)` |
| Erkannte Methoden (heuristisch) | **136** | Regex-basierte Erkennung |
| Durchschnittliche Methodengröße | **17,60 LoC/Method** | `2393 / 136` |
| Gesamtzahl Imports | **215** | Summe aller `import`-Zeilen |
| Coupling-Proxy (Ø Imports pro Typ) | **7,96** | Näherung für Kopplung |
| Datei mit höchster Importzahl | `HomeGenerateFlow.java` (**23**) | möglicher Kopplungs-Hotspot |

## 4) Paketstruktur
1. `simon.klausurcraft.app`
2. `simon.klausurcraft.task`
3. `simon.klausurcraft.task.export`
4. `simon.klausurcraft.task.io`
5. `simon.klausurcraft.task.planning`
6. `simon.klausurcraft.ui`
7. `simon.klausurcraft.ui.components`
8. `simon.klausurcraft.ui.home`

## 5) Auffällige Dateien (Größe)
Top-5 nach Zeilen:
1. `src/main/java/simon/klausurcraft/task/io/TaskXmlStore.java` (362)
2. `src/main/java/simon/klausurcraft/ui/home/HomeSubtaskSheet.java` (284)
3. `src/main/java/simon/klausurcraft/ui/home/HomeGenerateFlow.java` (273)
4. `src/main/java/simon/klausurcraft/ui/home/HomeCenterController.java` (230)
5. `src/main/java/simon/klausurcraft/ui/home/TaskSelectionCell.java` (186)

## 6) Erfassung weiterer IDE-Metriken (Cohesion/Coupling)
Zur vollständigen IDE-Analyse (z. B. **LCOM/Cohesion**, zyklomatische Komplexität, detaillierte Coupling-Graphen) kann ein IDE-Plugin verwendet werden.

Empfohlene Optionen:
- IntelliJ IDEA: **MetricsReloaded** (Plugin Marketplace)
- Eclipse: **CodeMR** (wie in der Aufgabenfolie), Link: https://marketplace.eclipse.org/content/codemr-static-code-analyser

Vorgehen (kurz):
1. Plugin installieren.
2. Projekt vollständig neu bauen/indexieren.
3. Metrics-View öffnen und Werte für Package/Class/Method-Ebene erzeugen.
4. Export als CSV/HTML/PDF durchführen und dem Projektbericht beilegen.

## 7) Export-Dateien
- Rohausgabe der Messung: `docs/metrics-run.txt`
- Tabellarischer Export: `docs/metrics-export.csv`
- Reproduzierbares Skript: `scripts/collect-metrics.sh`

## 8) Reproduzierbarkeit
Die Metriken können jederzeit mit folgendem Befehl neu erzeugt werden:

```bash
./scripts/collect-metrics.sh
```

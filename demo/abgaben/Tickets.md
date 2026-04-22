# Aufgabe 21 – Bug Reporting (Tickets)

**Projekt:** KlausurCraft  
**Version:** 1.0.0  
**Datum:** 22.04.2026

## Ticket 1 – Korrigierende Wartung
- **ID:** SPR-021-001
- **Titel:** Sidebar wird nach Subtask-Löschung nicht sofort aktualisiert
- **Priorität:** Hoch
- **Umgebung:** macOS 26.3.1, Java 25, JavaFX 25

**Beschreibung:**
Wenn ich eine Subtask im Center lösche, ist sie dort korrekt weg, in der Sidebar bleibt sie aber manchmal noch sichtbar.

**Soll / Ist:**
- Soll: Eintrag ist nach dem Löschen in beiden Bereichen sofort weg.
- Ist: Sidebar zeigt den Eintrag bis zum nächsten Refresh teilweise weiter an.

**Reproduktion:**
1. XML mit mehreren Tasks laden.
2. Im Center eine Subtask löschen.
3. Sidebar kontrollieren.

## Ticket 2 – Adaptive Wartung
- **ID:** SPR-021-002
- **Titel:** Datumsanzeige im Export an lokales Format anpassen
- **Priorität:** Mittel
- **Umgebung:** macOS 26.3.1, Java 25, JavaFX 25

**Beschreibung:**
Für den Einsatz im Hochschulkontext soll das Datum im PDF lokal formatiert sein.

**Soll / Ist:**
- Soll: `dd.MM.yyyy`
- Ist: früher ISO-Format

**Reproduktion:**
1. Datum im Generate-Dialog setzen.
2. PDF exportieren.
3. Datum im PDF-Kopf prüfen.

## Ticket 3 – Perfektive Wartung
- **ID:** SPR-021-003
- **Titel:** Suchperformance bei großen XML-Dateien verbessern
- **Priorität:** Mittel
- **Umgebung:** macOS 26.3.1, Java 25, JavaFX 25

**Beschreibung:**
Bei großen Datenmengen reagiert die Suche im Center spürbar verzögert.

**Soll / Ist:**
- Soll: flüssiges Filtern auch bei vielen Subtasks.
- Ist: verzögerte Aktualisierung bei schneller Eingabe.

**Reproduktion:**
1. `large-example.xml` laden.
2. Mehrere Suchbegriffe nacheinander eingeben.
3. UI-Reaktion beobachten.

## Ticket 4 – Präventive Wartung
- **ID:** SPR-021-004
- **Titel:** XML-Speicherung auf atomisches Schreiben mit Backup umstellen
- **Priorität:** Hoch
- **Umgebung:** macOS 26.3.1, Java 25, JavaFX 25

**Beschreibung:**
Direktes Schreiben in die Zieldatei birgt bei Abbruch das Risiko einer beschädigten XML-Datei.

**Soll / Ist:**
- Soll: erst temporär schreiben, dann atomar ersetzen; optional Backup der letzten validen Version.
- Ist: direkte Speicherung ohne Recovery-Strategie.

**Reproduktion (Risikofall):**
1. Änderungen durchführen (Autosave aktiv).
2. Prozess während Speichervorgang hart beenden.
3. Datei erneut öffnen und Integrität prüfen.

# Tickets (Aufgabe 21 - Bug Reporting)

Projekt: KlausurCraft  
Version: 1.0.0  
Erstellt am: 01.04.2026

## Ticket 1 - Korrigierende Wartung (Bug Report)

1. **Ticket-Nummer (id):** SPR-021-001  
2. **Titel:** Sidebar-Inhaltsverzeichnis wird nach dem Löschen einer Subtask im Center-View nicht aktualisiert  
3. **Priorität:** Hoch  
4. **Meldende Person:** Simon Riedinger  
5. **Version:** KlausurCraft 1.0.0  
6. **Umgebung:** macOS 26.3.1, Java 25, JavaFX 25  
7. **Beschreibung des Fehlers:** Beim Löschen einer Subtask über die Kartenansicht in der Mitte wird die Subtask zwar aus den Daten entfernt, bleibt aber im linken Inhaltsverzeichnis sichtbar.  
8. **Soll-/Ist-Resultat:**  
   Soll: Nach dem Löschen ist die Subtask sowohl im Center als auch in der Sidebar sofort entfernt.  
   Ist: Im Center ist sie entfernt, in der Sidebar bleibt sie bis zu einem späteren Refresh sichtbar.  
9. **Reproduktion des Fehlers:**  
   1) XML-Datei mit mehreren Tasks/Subtasks laden.  
   2) In der Mitte bei einer Subtask auf „Delete“ klicken und Löschung bestätigen.  
   3) Sidebar prüfen: Der gelöschte Eintrag ist weiterhin sichtbar.  
10. **Screenshot:** Optional, derzeit nicht beigefügt.

## Ticket 2 - Adaptive Wartung (Change Request)

1. **Ticket-Nummer (id):** SPR-021-002  
2. **Titel:** Exportiertes Prüfungsdatum soll an lokales Datumsformat angepasst werden  
3. **Priorität:** Mittel  
4. **Meldende Person:** Simon Riedinger  
5. **Version:** KlausurCraft 1.0.0  
6. **Umgebung:** macOS 26.3.1, Java 25, JavaFX 25  
7. **Beschreibung des Änderungswunschs:** In den erzeugten PDF-Dateien wird das Datum aktuell im ISO-Format (`YYYY-MM-DD`) ausgegeben. Für den Einsatz im Hochschulkontext wird das lokale Format (`dd.MM.yyyy`) benötigt.  
8. **Soll-/Ist-Resultat:**  
   Soll: Datumsanzeige in lokalem Format, z. B. `01.04.2026`.  
   Ist: Datumsanzeige im Format `2026-04-01`.  
9. **Reproduktion (aktuelles Verhalten):**  
   1) Generierungsdialog öffnen und ein Datum setzen.  
   2) PDF exportieren.  
   3) Kopfbereich prüfen: Datum erscheint im ISO-Format statt lokalem Format.  
10. **Screenshot:** Optional, derzeit nicht beigefügt.

## Ticket 3 - Perfektive Wartung (Change Request)

1. **Ticket-Nummer (id):** SPR-021-003  
2. **Titel:** Suchfunktion im Center-View bei großen XML-Dateien spürbar beschleunigen  
3. **Priorität:** Mittel  
4. **Meldende Person:** Simon Riedinger  
5. **Version:** KlausurCraft 1.0.0  
6. **Umgebung:** macOS 26.3.1, Java 25, JavaFX 25  
7. **Beschreibung des Änderungswunschs:** Bei vielen Tasks/Subtasks wird die Such- und Renderlogik im Center sichtbar träge. Ziel ist eine bessere Laufzeit durch weniger wiederholte Zugriffe auf Subtask-Metadaten und effizientere Filterung.  
8. **Soll-/Ist-Resultat:**  
   Soll: Flüssiges Filtern auch bei größeren Aufgabenpools.  
   Ist: Bei großen Datenmengen sind Suchupdates verzögert wahrnehmbar.  
9. **Reproduktion (aktuelles Verhalten):**  
   1) Große Beispieldatei laden (z. B. `large-example.xml`).  
   2) In der Suche mehrere Begriffe nacheinander eingeben.  
   3) UI-Reaktion beobachten: Rendering verzögert sich.  
10. **Screenshot:** Optional, derzeit nicht beigefügt.

## Ticket 4 - Präventive Wartung (Change Request)

1. **Ticket-Nummer (id):** SPR-021-004  
2. **Titel:** XML-Speicherung auf atomisches Schreiben mit Backup umstellen  
3. **Priorität:** Hoch  
4. **Meldende Person:** Simon Riedinger  
5. **Version:** KlausurCraft 1.0.0  
6. **Umgebung:** macOS 26.3.1, Java 25, JavaFX 25  
7. **Beschreibung des Änderungswunschs:** Das Speichern schreibt direkt in die Produktivdatei. Bei Absturz oder Stromausfall während des Schreibens besteht das Risiko einer beschädigten XML-Datei.  
8. **Soll-/Ist-Resultat:**  
   Soll: Erst in temporäre Datei schreiben, danach atomar ersetzen; optional letzte valide Version als Backup behalten.  
   Ist: Direkte Speicherung in die Zieldatei ohne Recovery-Strategie.  
9. **Reproduktion (Risikofall):**  
   1) Änderungen an Task/Subtask durchführen (Autosave aktiv).  
   2) Während des Speicherns Prozess abrupt beenden (Force Quit).  
   3) Datei erneut öffnen und auf XML-Integrität prüfen.  
10. **Screenshot:** Optional, derzeit nicht beigefügt.

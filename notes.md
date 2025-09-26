# Analysefragen für Klausurerstellungstool

## 1. Funktionale Anforderungen

### Grundfunktionalität
- Welche Arten von Prüfungsaufgaben sollen unterstützt werden (Multiple Choice, offene Fragen, Rechenaufgaben, etc.)?
- Sollen Aufgaben kategorisiert werden können (Schwierigkeitsgrad, Themenbereich, Lernziele)?
- Wie soll die automatische Auswahl der Aufgaben erfolgen (zufällig, gewichtet, regelbasiert)?
- Was für Nutzerrollen mit welchen Berechtigungen soll es geben?

### Aufgabenverwaltung
- Wer ist für die Eingabe und Pflege der Aufgaben zuständig?
- Sollen Aufgaben versioniert werden können?
- Wie sollen Bilder, Formeln oder andere Medien in Aufgaben eingebunden werden?
- Müssen Musterlösungen und Bewertungskriterien hinterlegt werden?
- Sollen Aufgaben zwischen verschiedenen Dozenten geteilt werden können?

### Klausurgenerierung
- Welche Parameter sollen bei der Klausurgenerierung berücksichtigt werden (Gesamtpunktzahl, Zeitlimit, Themenverteilung)?
- Sollen bestimmte Aufgaben als Pflichtaufgaben definiert werden können?
- Wie detailliert soll die Konfiguration der Klausur sein (Reihenfolge, Gewichtung, etc.)?
- Müssen verschiedene Ausgabeformate unterstützt werden (PDF, Word, E-Learning, etc.)?

---

## 2. Nicht-funktionale Anforderungen

### Benutzerfreundlichkeit
- Welche Zielgruppen werden das System nutzen (Dozenten, Assistenten, Verwaltung)?
- Welche technischen Kenntnisse haben die Benutzer?
- In welchen Sprachen soll das Interface verfügbar sein?
- Welche Barrierefreiheitsanforderungen bestehen?

### Performance
- Wie viele Benutzer sollen gleichzeitig mit dem System arbeiten können?
- Wie schnell soll eine Klausur generiert werden?
- Welche Systemauslastung ist in Prüfungszeiten zu erwarten?

### Sicherheit und Datenschutz
- Welche Sicherheitsanforderungen bestehen für die Prüfungsaufgaben?
- Wie sollen Zugriffsrechte verwaltet werden?
- Welche Datenschutzbestimmungen müssen beachtet werden?

---

## 3. Technische Anforderungen

### Systemarchitektur
- Soll es eine Web-Anwendung oder Desktop-Software werden?
- Welche bestehenden IT-Systeme der Hochschule müssen angebunden werden?
- Welche Datenbank-Systeme werden bevorzugt oder sind bereits vorhanden?
- Gibt es Vorgaben für Programmiersprachen oder Frameworks?

### Integration
- Ist eine Anbindung an Notenverwaltungssysteme erforderlich?

### Hosting und Betrieb
- Soll das System on-premise oder in der Cloud betrieben werden?
- Welche Hardware-Ressourcen stehen zur Verfügung?
- Wer ist für den Betrieb und die Wartung zuständig?

---

## 4. Organisatorische Anforderungen

### Arbeitsabläufe
- Wie läuft der aktuelle Prozess der Klausurerstellung ab?
- Wer ist am Genehmigungsprozess für Klausuren beteiligt?
- Sollen mehrere Dozenten gleichzeitig am Aufgabenpool arbeiten können? 

### Rollen und Rechte
- Welche Benutzerrollen soll es geben?
- Wer darf Aufgaben erstellen, bearbeiten, löschen?
- Sollen Freigabeprozesse für Aufgaben implementiert werden?
- Wie soll die Rechteverwaltung auf Fachbereichsebene erfolgen?

---

## 5. Qualitätssicherung

### Validierung
- Wie soll die Qualität der generierten Klausuren sichergestellt werden?
- Sollen automatische Prüfungen implementiert werden (z. B. Gesamtpunktzahl, Themenabdeckung)?
- Wer ist für die finale Freigabe einer generierten Klausur zuständig?

### Testing
- Sollen Testklausuren generiert werden können?
- Wie soll das System vor dem Produktiveinsatz getestet werden?

---

## 6. Rechtliche und Compliance-Anforderungen

### Prüfungsordnung
- Welche Vorgaben aus der Prüfungsordnung müssen beachtet werden?
- Gibt es spezielle Anforderungen für bestimmte Studiengänge?

### Dokumentation
- Welche Dokumentationsanforderungen bestehen?
- Müssen Änderungen an Aufgaben nachvollziehbar sein?
- Wie lange müssen Klausurdaten aufbewahrt werden?

---

## 7. Budget und Zeitplan

### Ressourcen
- Welches Budget steht für die Entwicklung zur Verfügung?
- Bis wann soll das System einsatzbereit sein?
- Welche internen Ressourcen können für das Projekt bereitgestellt werden?
- Soll das System in Phasen ausgerollt werden?

### Wartung und Support
- Wie soll der langfristige Support sichergestellt werden?
- Welches Budget ist für Wartung und Weiterentwicklung vorgesehen?

---

## Zusätzliche Überlegungen
- Beispielklausuren: Können Sie uns Beispiele aktueller Klausuren zur Verfügung stellen?  
- Stakeholder: Wer sind alle beteiligten Personen, die wir interviewen sollten?

---

# Notizen

Softwar, lokal, einzelpersion, zufallsgenerator, klausuraufgaben, teilaufgaben, datensammlung, betriebssystemunabhängig, java anwendung mit javaFX als Standalone anwendung, openSource, ohne Datentransverierung, aktuell libreoffice dokument, probeklausur, anlegen des datensatzes möglich, aufgabentext, keine grafiken in version 1 gerne aber später, kein import, aufgabentext, musterlösung die natürlich nicht auf der klausur stehen aber option zur musterlösungserstellung, fortlaufende nummer der aufgabe, jede aufgabe hat verschiedene teilaufgaben (1.a 1.b, 1.c). Es geht um teilaufgaben. Die echten aufgaben sind dann die themengebiete, punkte pro teilaufgabe, jede teilaufgabe hat kategorie (schwer, mittel, leicht), bei klausurgenerierung wird angegeben wie viele themen genutzt werden sollen also pro aufgabe dann ein thema, dann pro aufgabe die gesamtpunktzahl angeben und aus dem teilaufgaben pool sollen dann die teilaufgaben zufällig ausgewählt werden sollen 1/3 aus jeder schwierigkeitskategorie, vorschlag der punktzahl bei der auswahl des tehemengebiets bitte hier dropdown mit allen möglichen punktekombinationen anzubieten, aufgaben bearbeiten und löschen können, Korrektur der generiergten klausur in version 2 bitte, generierte klausuren speichern in historie als pdf (vielleicht als andere formate optional), generieren button und dann direkt speichern, in version 2 villeicht noch bestätigen, beim generieren werden die themen + punkte pro thema eingegeben, jedes thema ist eine aufgabe, jede teilaufgabe ist einem kapitel zugeordnet und wird dann hinzugefügt random mit 1/3 pro schwierigkeit, bei der generierung in abhängikeit der punktzahl die größe des kastens erstellen, optional in version 2 die größe des kastens anpassen nach dem generieren und vor dem bestätigen, teilaufgabe kann erstmals auch ohne musterlösung erstellt werden und auch später hinzugefügt werden, bei jeder klausur deckblatt als version 2, seitenzahl der klausr bspw 2/9, checkbox auswal aus den kapiteln bei der klausurerstellung, ganzes tool auf englisch bitte also benuteroberfläche später soll sprache bitte einfach erweiterbar sein aber erst in verison 2, bei jeder teilaufgabe muss angegeben werden Probeklausuraufgabe oder Klausuraufgabe, es soll zusätzlich manuell eine teilaufgabengruppe erstellt werden können, diese teilaufgabengruppe besteht aus 2 oder mehr teilaufgaben und bei der klausur soll dann höchstens eine teilaufgabe dieser gruppe drin sein, in moodlee ist eine beispielklausur, code gut dokumentieren, in version 2 plichtaufgaben, reihenfolge der themen (also kapitel/ aufgaben) bitte konfigurierbar, es gibt also aufgaben die entsprechen themen, dann gibt es teilaufgaben die immer genau einer aufgabe (thema) zugeordnet werden optional kann auch eine teilaufgabengruppe erstellt werden, aus der dann in einer klausur immer nur eine teilaufgabe ist. Bei erstellung der teilaufgabe gibt es den teilaufgabentext, die punktzahl und das thema sowie eine mustetrlösung und ein atribut mit klausuraufgabe und probeklausuraufgabe. Man soll dasnn eine klausur/ Probeklausur sowie deren musterlösung generieren können. Hierbei sollen die tehmen ausgewählt werden und dann anordnen. Es soll eine auswahl an möglichen punkten pro aufgabe geben, die automatisch aus allen möglichkeiten kombinationen der teilaufgaben generiert werden sollen. Dann soll generiert werden und vorschau angezeigt, dann bestätigt werden und abgepsichert werden. Mann soll zu teilaufgabengruppen bestehende teilaufgaben sowie neue teilaufgabengruppen  

Datensatz als xml abspeichern und dann im tool xml importieren und laden, so können unterschiedleiche vorlesungen erstellt werden, in version 2 sollen die aufgaben html fähig sein am anfang einfach text  

Es soll doch keine teilaufgbengruppen geben sondern jede teilaufgabe kann verschiedeen aufgabentextvarianten haben. Dieser soll dann bei dem auswahl der teilaufgabe nochmals zufällig ausgewählt weden. Eine Aufgabe mit mehreren Varianten soll dann ein übergordnerter varianten name hinzugefügt werden. Hübsche benutroberfläche, laden speichern, speichern unter von xml dateien, + - buttons, info button, klausur generieren, probeklausr generieren, option zum auswählen wo die generierte klausr gespeichert werden soll. Historiee soll nur in form der pdfs da sein, keine historie in der benutzeroberfläche, keine unterschiedlichen user…  
Hover effekt über buttons mit bescvhreibung was der button macht  
Tehemreihenfolge soll definierbar sein, teilaufbaben von eifnach nach schwer sortieren, fehlermeldung wenn zu wenig aufgaben in einer schwierigkeitskategorie, teilaufgaben sollen auch ohne muserlösung erstellt werden können und bei der generierung der musterlösung dann einfach leer gelassen werden, man soll aber in der oberfläche später die musterlösung bearbietet werden können, bei der 1/3 aufteilung ist sinvolle toleranz in ordnung  

---

# Anforderungen Klausurerstellungstool

## 1. Allgemeines
- Softwareart: Lokale Desktop-Anwendung, Einzelbenutzer  
- Technologie: Java mit JavaFX, Standalone-Anwendung  
- Lizenzierung: Open Source  
- Betriebssystem: Unabhängig (plattformübergreifend)  
- Datenhaltung: Speicherung als XML-Dateien (inkl. Laden/Speichern/„Speichern unter“)  
- Export: Generierte Klausuren als PDF speichern (weitere Formate optional)  
- UI/UX:  
  - Intuitive, moderne Oberfläche mit Buttons (+ / - / Info / Generate etc.)  
  - Hover-Effekte mit Kurzbeschreibung der Buttons  
  - Themenreihenfolge konfigurierbar  
  - Keine Mehrbenutzerverwaltung notwendig  

## 2. Datenmodell

### 2.1 Aufgaben & Teilaufgaben
- Aufgabe = Thema (übergeordnet)  
- Teilaufgabe = konkrete Frage/Unteraufgabe, immer genau einer Aufgabe (Thema) zugeordnet  
- Teilaufgabenattribute:  
  - Text (HTML-fähig ab Version 2, zunächst nur Text)  
  - Punktzahl  
  - Kategorie (Schwierigkeitsgrad: leicht, mittel, schwer)  
  - Variante (optional mehrere Textvarianten, zufällige Auswahl bei Klausurgenerierung)  
  - Musterlösung (optional; kann später hinzugefügt oder leer gelassen werden)  
  - Kennzeichnung als Klausuraufgabe oder Probeklausuraufgabe  

### 2.2 Aufgabenvarianten
- Jede Teilaufgabe kann mehrere Textvarianten haben.  
- Bei der Klausurgenerierung wird zufällig eine Variante ausgewählt.  
- Varianten sind durch einen übergeordneten „Variantennamen“ gruppiert.  
- Jede Variante kann auch eine eigene Lösung haben  

## 3. Funktionen

### 3.1 Datenverwaltung
- Anlegen, Bearbeiten und Löschen von Aufgaben und Teilaufgaben  
- Import/Export von XML-Dateien (z. B. für unterschiedliche Vorlesungen)  
- Laden/Speichern/„Speichern unter“ von Datensätzen  

### 3.2 Klausurgenerierung
- Auswahl: Klausur oder Probeklausur (inkl. Musterlösung optional)  
- Auswahl der Themen (Aufgaben), konfigurierbare Reihenfolge  
- Eingabe der Punkte pro Aufgabe (Auswahl aus automatisch generierten gültigen Kombinationen)  
- Zuweisung der Teilaufgaben nach Zufall (mit 1/3-Regelung pro Schwierigkeitskategorie, toleranzfähig)  
- Fehlermeldung, wenn zu wenige Aufgaben pro Kategorie vorhanden sind  
- Sortierung der Teilaufgaben innerhalb einer Aufgabe: von leicht nach schwer  
- Generierung per Button:  
  - PDF-Ausgabe  
  - Option, Speicherort auszuwählen  
  - Historie nur in Form gespeicherter PDFs, keine interne Liste  

### 3.3 Musterlösung
- Automatische Generierung (falls vorhanden, sonst leer)  
- Nachträgliche Bearbeitung in der Oberfläche möglich  

## 4. Versionierung

### Version 1 (Basis)
- Lokale Standalone-App mit JavaFX  
- Aufgaben- und Teilaufgabenverwaltung  
- Generierung von Klausuren und Probeklausuren inkl. Musterlösung  
- Speicherung in XML und Export als PDF  
- 1/3-Zufallsregel für Schwierigkeitsgrade  
- Varianten pro Teilaufgabe  
- Hover-Infos über Buttons  
- Themenreihenfolge konfigurierbar  
- Kein Nutzer- oder Rollensystem  
- Keine Teilaufgabengruppen (stattdessen Varianten)  

### Version 2 (Erweiterungen)
- Pflichtaufgaben definierbar  
- Vorschau der Klausur vor dem finalen Speichern  
- Bestätigungsschritt vor Generierung  
- Deckblatt und Seitennummerierung (z. B. „Seite 2/9“)  
- Manuelle Anpassung der Kästchengrößen (abhängig von Punktzahl)  
- HTML-Unterstützung für Aufgabeninhalte  
- Sprachunterstützung: Oberfläche auf Englisch, Erweiterbarkeit für weitere Sprachen  
- Korrekturfunktion für generierte Klausuren  

## 5. Technische Anforderungen
- Dateiformat: XML für Aufgabenpool, PDF für Klausuren  
- Codequalität: Gut dokumentierter Code  
- Fehlerhandling:  
  - Meldungen bei zu wenig Aufgaben in einer Kategorie  
  - Plausibilitätsprüfung bei Generierung (Punkte, Themen etc.)  

# UI-Konzept
- Startseite mit Auffoerderung zum laden des xml-files
- Dann Startscreen -> Links "Inhaltsverzeichnis" in dem die Struktur der Tasks-Subtasks dargestellt wird. Beim klick auf eine Task oder Subtask wird dann im mittleren Bildabschnitt zur korrekten Stelle gespruchgen, der mittlere Bildabschnitt enthält alle Tasks-Subtasks-Varianten. Man kann die Subtasks Anklicken und bekommt dann in einem "Sheet" einen detailierten View der Subtask mit allen Infos und Varianten.
Unten am Startscreen ist so eine kleine Leiste in der Steht welches xml file man gerade impoertiert hat und wie viele Tasks und Subtasks es gerade gibt. Hier soll es auch einen Button geben mit dem man das geladene xml-file wechseln kann.
Auf dem Startscreen im Rechten Eck in der Leiste ist dann ein Button auf dem "Generate" steht. Beim klicken wird man ebenfalls mit Sheets durch den generierungsprozess geleitet:
1. Radio Button Auswahl zwischen exam/practice/both
2. Dann kommt eine Übersicht aller Themen/Tasks die ertmal ausgegraut sind, Hier kann man die Themen/Tasks mittels einer Checkbox links Aktivieren, dann wird das jeweilige Thema erstmal leicht rot bis man auf der rechten Seite aus einem Dropdownmenü in dem alle Möglichen Punktekombinationen der Subtasks verfügbar sind eine Punktzahl auswählt. Man kann die Themen dann auch in der Reihenfolge verschieben. Unten gibt es eine Leiste in der man die Gesamtpunktzahl der Klausur sieht, die sich aus den ausgewählten Themen zusammensetzt. Zudem ist dort ein "Generate Exam" Button mit dem man die Generirung startet und eine Checkbox mit "Sample Solution"
3. Beim klicken auf "Generte Exam" startet die Generirung des PDFs und wahlweise auch der Sample Solution. Wenn die Generierung geklappt hat wird ein Fenster angezeigt, in dem auswählen kann wo die generierten Files auf dem Computer gespeichert werden sollen.

Danach kommt man wieder auf den Startscreen

Wichtig ist das alles mit den css klassen light.css und dark.css funktioniert. Also das Theme soll auch von dunkel auf hell umgeschalten werden können.

Dann soll oben rechts ein "+"-Button integriert werden Mit diesem Button soll man die Möglichkeit haben eine neue Aufgabe anzulegen. Man soll in ein Sheet kommen indem man einfach den Titel der Task angeben kann. 
Neben jeder Task im Startscreen ist ein kleiner Button mit einem Mülleimer, so kann man direkt mittels einem klick und einer anschliesenden bestätigung die gesamte Task inklusiver aller Subtasks löschen. Dann gibt es noch einen Stift, hier kann man den Name der Task bearbeiten und ein Plus dann wird man auf ein Sheet geleitet in dem man eine neue Subtask zur Aufgabe anlegen kann. Man gibt zuerst die Punktzahl, schwierigkeit und sichtbarkeit an. Dann kann man den group name angeben und den Aufgabentext, sowie optional Lösung hinzufügen. Mittels eines weiteren Plus kann dann eine weitere Variante erstellt werden. Wenn man zufireden ist klickt man oben rechts auf den Haken zum bestätigen. 
Neben jeder Subtask gibt es einen mülleimer zum löschen der subtask inkklusive aller varianten und einen stift zum bearbeiten des group namens und der varianten, so kann bspw. der aufgabentext oder die lösung bearbeitet werden, oder auch einzelne varianten hinzugefügt oder gelöscht werden.



$$
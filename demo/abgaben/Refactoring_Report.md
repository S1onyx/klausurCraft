# Aufgabe 20 – Refactoring Report

**Projekt:** KlausurCraft  
**Datum:** 22.04.2026

Für diese Aufgabe habe ich drei Refactoring-Patterns aus dem Katalog ausgewählt und jeweils auf eine konkrete Stelle im vorhandenen Code bezogen.

## 1) Extract Method
**Katalog:** https://refactoring.com/catalog/extractMethod.html  
**Stelle im Projekt:** `HomeGenerateFlow.generateExamNow(...)`

Die Methode enthält mehrere Schritte in einem Ablauf: Auswahl passender Subtasks, Verteilungslogik, Variantenauswahl und Aufbau der Exportdaten. Für das Verständnis ist das auf einmal recht viel. Mit `Extract Method` lässt sich das in kleinere Schritte aufteilen, die man einzeln lesen und testen kann.

## 2) Encapsulate Collection
**Katalog:** https://refactoring.com/catalog/encapsulateCollection.html  
**Stelle im Projekt:** `Task.getSubtasks()` und `Subtask.getVariants()`

Aktuell werden veränderbare Listen direkt nach außen gegeben. Dadurch kann Objektzustand von außen leicht unkontrolliert geändert werden. Mit `Encapsulate Collection` würde ich lieber nur kontrollierte Methoden anbieten (z. B. hinzufügen/entfernen) oder eine unveränderliche Sicht zurückgeben.

## 3) Split Phase
**Katalog:** https://refactoring.com/catalog/splitPhase.html  
**Stelle im Projekt:** `TaskXmlStore.load(...)`

Beim Laden passiert derzeit in einem Fluss sowohl XML-Validierung als auch Mapping in die Domänenobjekte. Bei Fehlern ist es dadurch schwerer zu sehen, ob das Problem in der Datei, in der Validierung oder im Mapping liegt. Mit `Split Phase` (z. B. erst validiertes Dokument bauen, dann separat mappen) wird der Ablauf klarer und besser wartbar.

## Fazit
Die drei Patterns passen gut zum aktuellen Stand des Projekts, weil sie Lesbarkeit, Testbarkeit und Wartbarkeit verbessern, ohne das fachliche Verhalten zu verändern.

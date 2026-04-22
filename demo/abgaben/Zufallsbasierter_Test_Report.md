# Aufgabe 17 – Zufallsbasierter Test

**Projekt:** KlausurCraft  
**Datum:** 22.04.2026

## Ziel
Für diese Aufgabe habe ich die Portoberechnung mit einem zufallsbasierten JUnit-Test abgesichert.

## Umsetzung im Projekt
Neue, klar getrennte Komponente:
- `src/main/java/simon/klausurcraft/shipping/ShippingZone.java`
- `src/main/java/simon/klausurcraft/shipping/Parcel.java`
- `src/main/java/simon/klausurcraft/shipping/PostageCalculator.java`

Testklasse:
- `src/test/java/simon/klausurcraft/shipping/PostageCalculatorRandomizedTest.java`

## Erfüllung der Anforderungen
- **1000 zufällige Pakete:** In `randomizedPostageTest_1000Parcels_matchesExpectedResultAndRange()` werden 1000 Pakete erzeugt.
- **Soll-/Ist-Vergleich:** Das Ist-Ergebnis `PostageCalculator.calculate(...)` wird je Fall gegen `expectedPostage(...)` geprüft.
- **Separate Soll-Funktion:** `expectedPostage(Parcel parcel)` ist im Test klar getrennt implementiert.
- **Zusatzbedingungen:** In jedem Zufallsfall wird geprüft, dass das Porto im Bereich `0 <= Porto <= 100` liegt.

## Verwendete Zufallsbereiche
- Länge: 15 bis 160 cm
- Breite: 10 bis 120 cm
- Höhe: 2 bis 100 cm
- Gewicht: 0,10 bis 30,00 kg
- Zone: LOCAL, NATIONAL, INTERNATIONAL
- Optionen: `priority` und `fragile`

Die Werte decken kleine, mittlere und große Pakete sinnvoll ab.

## Zusätzliche Randfalltests
Neben dem 1000er-Lauf habe ich noch explizite Grenztests ergänzt:
- Tarif-/Gewichtsgrenzen
- Clamp auf maximal 100,00 bei extrem großen Paketen

## Ausführung
```bash
mvn -q test
```

Der Testlauf war erfolgreich.

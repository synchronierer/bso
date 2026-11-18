# Schuljahr 2026/27 – Fachregeln aus Rückmeldung und Excel

Quelle: **BSO Veranstaltungen und Münzen 202627.xlsx**, Arbeitsblatt `Tabelle1`.
Dieses Excel-Dokument wird nicht ins öffentliche Repository übernommen. Der strukturierte
Regelstand aus dem Fachblatt ist in Flyway V14/V15 und `CoinRules` übernommen.

## Zertifikatsschwellen

| Jahrgang | Track | Erfolg | Besonderer Erfolg | Auszeichnung |
| --- | --- | ---: | ---: | ---: |
| 8 | ohne | 10 | 17 | 22 |
| 9 | KA | 119 | 126 | 133 |
| 9 | BR | 119 | 126 | 133 |
| 9 | V10 | 16 | 23 | 30 |
| 9 | S1 | 16 | 23 | 30 |
| 9 | Ü11 | 16 | 23 | 30 |
| 10 | BR | 18 | 25 | 32 |
| 10 | S1 | 18 | 25 | 32 |
| 10 | Ü11 | 118 | 125 | 132 |
| 11 | ohne | 118 | 125 | 132 |
| 12 | ohne | 17 | 24 | 31 |

Die alten `MSS`- und Sammeltrack-Schwellen sind damit für 2026/27 **ersetzt**,
nicht weiter als Ersatzwerte benutzt. Ein Schüler mit nicht definiertem Jahrgang/Track
erhält `REVIEW_REQUIRED`, kein erfundenes Zertifikatsniveau.

## Verpflichtungen

Die ausgefüllten Zellen der Tabelle (Zeilen 3–20) erzeugen 67 jahrgangs-
und trackbezogene Einträge in `event_requirements`. Die Spalte `target_coins`
bildet den in der Tabelle angegebenen Wert ab; die Spalte `minimum_count` ist
bei den Netzwerkpartner-Workshops **2**, sonst **1**. Pflichtpraktika werden als
separater Typ `PRAKTIKUM` geführt und dürfen mehrfach abgerechnet werden.

**Wichtig für Abnahme:** Einzelne in BSO bereits erfasste Veranstaltungsnamen müssen
dem Pflichttitel **exakt entsprechen** oder über Äquivalenzgruppen zugeordnet sein.
Die vorgeschriebene Zahl an Netzwerkpartner-Workshops zählt dagegen unabhängig
vom konkreten Namen **verschiedene Workshop-Titel**. Freigegebene Pflichtpraktika
werden gesondert als `PRAKTIKUM` gezählt. Die Excel-Zahlen werden nicht nachträglich
auf bereits gebuchte Veranstaltungs-Münzen angewandt.

Bitte fachlich prüfen, ob die Schwellen für 10/Ü11 (118/125/132) und 12/ohne
(17/24/31) trotz unterschiedlicher Jahrgangsstände genau so gewollt sind;
die Anwendung folgt derzeit absichtlich **wortgetreu** dem neuen Tabellenblatt.

## Bedienungsänderungen

- „ohne“ als Track bei Schülererfassung; „KA“ ebenfalls auswählbar
- CSV-Export aus jedem Praktikumsblock: Name, Klasse, Betrieb; Klassenfilter optional
- Veranstaltungen bearbeiten und nur ohne Münzbuchungen löschen
- Praktikumsblöcke umbenennen, vor Münzvergabe Termin korrigieren; Block nur
  ohne finale Münzvergabe löschen (vorläufige Zuordnungen werden entfernt)
- Veranstaltungsfilter im Klassenraster gegen unübersichtliche lange Auswahl
- Bestehende Zertifikate werden nach Regelmigration zur **Neubewertung** markiert

## Technische Abnahme

`mvn verify`/GitHub-CI validiert Kompilierung und Unit-Tests.
Zusätzlich mit **fiktiven Schülerdaten** testen: Importe mit „ohne“;
11 exakte Grenzfälle; zwei Workshops; ein Pflichtpraktikum; CSV-Export;
Umbenennung; Löschschutz; neue Zertifikatsversionen und historisches PDF/ODT.
Diese Anwendung bleibt bis zu Schul-/IT-/Datenschutzfreigabe im Testbetrieb.

# Praktikumsblöcke – vorplanen und nach Abschluss abrechnen

## Bedienung (RC1-Testbetrieb)

1. Auf der Startseite unter **Praktikumsblöcke** einen Block mit **Bezeichnung**, **Klasse**, **Art** (Pflicht oder freiwilliges Zusatzpraktikum) sowie **gemeinsamem Beginn und Ende** erstellen.
2. Block öffnen: Alle aktiven Schülerinnen und Schüler der gewählten Klasse erscheinen in einer Tabelle. Bei den teilnehmenden Personen den **Betrieb** eintragen und **Betriebe vorab speichern** drücken. Alle anderen Zeilen leer lassen. Es entstehen **keine Münzen**.
3. Auch nach dem Speichern können noch nicht abgerechnete Betriebe verändert werden. Bereits endgültig angerechnete Einträge sind geschützt.
4. **Erst nach dem Enddatum** die Zeilen mit bereits gespeicherten Betrieben auswählen, je Person die **anerkannten Tage** prüfen (Pflicht 1–10; Zusatz 1–5) und über **Ausgewählte Praktika endgültig abrechnen** bestätigen. Erst jetzt werden 10 Münzen pro anerkanntem Tag gutgeschrieben.
5. Teilweise Abrechnung ist möglich: Fehlende Teilnahmen können später geprüft und separat endgültig gebucht werden.

## Datenhaltung

Flyway V8 ergänzt `internship_blocks` und `internship_placements`. Frühere direkte, endgültig angerechnete Einträge in `internships` bleiben unverändert. Ein Platzierungseintrag speichert zunächst nur Betrieb, Person und die zum Zeitpunkt der Planung offene Schulbesuchsepisode. `recognized_days` und `internship_id` werden erst bei endgültiger Abrechnung gesetzt. `CertificateService` berücksichtigt nach wie vor nur `internships.finalised=true`.

API:
- `GET /api/internship-blocks`: Liste aller Blöcke
- `POST /api/internship-blocks`: Block anlegen
- `GET /api/internship-blocks/{id}/placements`: Klassenraster
- `POST /api/internship-blocks/{id}/placements`: Betriebe für ausgewählte Personen speichern, ohne Münzen
- `POST /api/internship-blocks/{id}/finalise`: ausgewählte Platzierungen endgültig mit Tagen abrechnen

Eine Abrechnung wird transaktional durchgeführt. Bestehende Regeln (ehemaliger Einzelbuchungsendpunkt) für Pflichtpraktika pro Münzblock und Zusatzpraktika pro Schuljahr bleiben wirksam. Die Abrechnung nach Enddatum, Aktivstatus, Klassen- und Episodenprüfung verhindert voreilige und versehentliche Buchungen. **Versehentlich endgültige Buchungen erfordern einen gesonderten Korrekturprozess**; keine automatische Löschung.

## Testfälle vor produktivem Einsatz

- Block mit Zukunftsdatum und zwei Betrieben speichern: Schülerkonten bleiben unverändert, Zertifikatsberechnung ohne neuen Praktikumsbetrag.
- Vor Enddatum eine Abrechnung versuchen: HTTP 400 und keine Teilbuchung.
- Nach Enddatum 2 Schüler mit 10 und 8 anerkannten Tagen abrechnen: 100 und 80 Münzen, jeweils genau einmal.
- Erneute Abrechnung derselben Zeile: Fehler, keine Doppelbuchung.
- Zusatzpraktikum mit mehr als 5 Tagen: Fehler.
- Andere Schulbesuchsepisode oder archivierter Schüler: blockieren und manuelle Prüfung.
- Ein weiterer Vorabeintrag für einen bereits im selben Block finalisierten Schüler bleibt unveränderbar.
- Bestehende historische Praktika bleiben nach Flyway V8 erhalten.

**Freigabe:** Nur fiktive Daten im RC1-Testbetrieb. Vor Echtdaten Datenschutz-, Backup- und fachliche Abnahme durchführen.

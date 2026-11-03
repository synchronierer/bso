# CODEx-MASTER-AUFTRAG: BSO-System zur Produktionsreife entwickeln

**Auftraggeber:** IGS Am Nanstein  
**Projekt:** `synchronierer/bso`  
**Priorität:** Hoch – möglichst viel in einem zusammenhängenden Entwicklungsdurchgang liefern.  
**Ausführung:** Codex, bevorzugt GPT-5.6 mit Denkaufwand *Medium*, sofern in der lokalen Codex-Installation verfügbar. Bei anderem Modell dieselben Qualitäts- und Sicherheitsanforderungen einhalten.  
**Verbindliche Fachquelle:** [docs/PFLICHTENHEFT.md](PFLICHTENHEFT.md). Der folgende Auftrag präzisiert **Umsetzung, Tests und Betriebsreife**; er ersetzt nicht die Fachregeln im Pflichtenheft.

## 0. Direkte Arbeitsanweisung

**Arbeite autonom und implementiere die größtmögliche zusammenhängende, funktionsfähige Version.** Keine Serie von Rückfragen, kein bloßes Planungsdokument und keine bloßen Platzhalter. Prüfe den Code, baue das System und führe Tests aus. Falls eine funktionale Entscheidung nicht im Pflichtenheft steht, wähle die sicherere, nachvollziehbare, datenerhaltende Lösung und dokumentiere die Annahme. Fachregeln aus den 115 Entscheidungen dürfen nicht frei verändert werden.

Schließe den Arbeitsauftrag in einem **Feature-Branch mit Pull Request**, nicht durch ungetestetes Überschreiben des produktiv genutzten `main` oder automatisches Deployment. Der Prototyp läuft bereits auf einem Linux-Schulserver mit Docker Compose, PostgreSQL und externem Nginx-Reverse-Proxy. **Der auf dem Server vorhandene Datenbestand und andere Container/Dienste dürfen nicht beschädigt werden.**

Wichtige Produktziele:
1. **Einfachster Lehrer-Workflow:** XLSX/CSV für Schüler und Veranstaltungen → Importvorschau/Differenzen → Klasse + Veranstaltung auswählen → Schülerinnen/Schüler als Raster markieren, je Schüler ggf. Münzen ändern → Entwurf automatisch gespeichert → eine abschließende Sammelbuchung.
2. **Fachlich exakt:** sämtliche BSO-Münzregeln inkl. Schuljahre, Doppelzählungsschutz, Praktika, zwei Blöcke, Wiederholungen und stichtagsbezogene Zertifikatsstufen.
3. **Dokumente vollständig:** fertige PDF/DOCX-Zertifikate, Einzel- und Sammelverarbeitung, Schulbriefkopf und optionale hochgeladene Signatur ohne Einchecken privater Grafiken.
4. **Betriebssicherheit:** Authentifizierung, Session/Proxy/HTTPS, Berechtigungen, Datenintegrität, Audit, Backups, Restore, Tests, sichere Migrationen.
5. **Abnahmefähigkeit:** Testmatrix + Installations-/Update-/Rollback-Anleitung + Freigabematrix; keine Behauptung von Produktionsreife, wenn Prüfkriterien fehlen.

## 1. Bestandsaufnahme – vor jeder Codeänderung

- Repository vollständig lesen: `pom.xml`, `src/main/**`, `compose.yaml`, `deploy/**`, `.github/workflows/**`, `docs/PFLICHTENHEFT.md`.
- Bereits implementierte Demo: Spring Boot 3.5.7/Java 21, Spring Security, PostgreSQL, CSV-Vorschau/Import, Klassenraster, Docker Compose, einfache Benutzerverwaltung und Nginx-Forwarded-Header-Konfiguration. Einige vorhandene Implementierungen sind **funktionale Prototypen** mit Sicherheits-/Fachlücken. Nicht annehmen, dass sie verifiziert sind.
- **Lücken zuerst identifizieren und anschließend schließen**, statt neue parallele unverbundene Module zu bauen. Bei anfälligen vorhandenen Lösungen gezielt refaktorieren.
- Prüfe aktuelle APIs und Abhängigkeiten, vorhandene CI-Läufe und Kompilierbarkeit. Richte reproduzierbare Tests ein, bevorzugt Java-/Postgres-Integrationstests.
- Schütze bestehende Testdaten: vor destruktiven Änderungen ein Backup-/Restore-Verfahren dokumentieren und Migrationen additiv durchführen. **Nie `docker compose down -v` als Standard-Update oder Rollback.**
- Keine echten Daten, private IP-/Netztopologie, Passwörter, Unterschriften, Dumps oder Zertifikatsschlüssel im öffentlichen GitHub-Repository.

## 2. Architektur und Datenbank – zuerst die belastbare Grundlage

Zielarchitektur: modularer Monolith mit klar getrennten Bereichen `auth`, `students`, `schoolyears`, `events`, `coins`, `internships`, `certificates`, `reporting`, `audit`, `admin`. Java 21 / Spring Boot / PostgreSQL / Docker Compose beibehalten. Weboberfläche desktop-optimiert, deutschsprachig, zügig und für schulische Nutzung verständlich.

### 2.1 Daten und Migration

- PostgreSQL-Schemamigrationen durch Flyway/Liquibase, von vorhandener Demo ohne Datenverlust möglich. Entscheide explizit, wie vorhandene bereits durch `schema.sql` erzeugte Tabellen in Baseline/Migrationen überführt werden. Bestehende `students`, `events`, `coin_grants`, `app_users` nicht blind löschen oder umbenennen; erst Datenmodell analysieren und eine geprüfte Transformationsmigration erstellen.
- Studentendatensätze haben dauerhafte Verwaltungs-ID als **Text** (führende Nullen); schulinternen Personenbezug, getrennte **Schulbesuchsepisoden**, Klassenzuordnungen/Bildungsgänge mit Wirksamkeitsdatum.
- Schuljahre mit verbindlicher Datumsdefinition, Start 2026/27. **Block Sek I 7–10; Oberstufe 11–13**, beim Einstieg in Klasse 11 neuer Block bei Null. Historische Blockwerte erhalten.
- Ereignis-Durchführungen getrennt von **Äquivalenzgruppen** (transitive Gleichartigkeit), mögliche Typen Pflicht/Wahlpflicht/Workshop; Pflichtzuordnung je Jahrgang/Bildungsgang.
- Münzkonten als revisionsfähiges Journal/Ledger: exakte `NUMERIC`/`BigDecimal`-Werte (0,5er-Schritte), fachliches Wirksamkeitsdatum vs. Erstellungszeitpunkt, Ersteller, Schuljahr, Block, Herkunft (Event/Praktikum/extern/Korrektur), Ursprungswert und Gültigkeits-/Ausschlussstatus. Nicht mit `double` rechnen.
- Korrekturen, Storno, Event-Merge, Klassenwiederholung und Rücknahme ohne stille Überschreibung; Audit-Vorher/Nachher.
- Zertifikatsregeln mit Versions-/Gültigkeitsmodell, ausgestellte Zertifikate mit Versionen, Snapshot der zugrunde liegenden Münzwerte/Regeln, Dokumentdateien geschützt abgelegt, Status `AKTUELL`, `PRUEFUNG_ERFORDERLICH`, `ERSETZT`, `UNGUELTIG_WIEDERHOLUNG`.
- Indexe, Fremdschlüssel, Checks, Sperrkonzept, transaktionssichere Eindeutigkeit. Idempotente Import- und Buchungsoperationen mit Schutz bei parallelen Lehrkräften.

### 2.2 Historie und Schuljahreswechsel

- Schülerimport CSV **und XLSX**, UTF-8, Semikolon/Komma, robuste Datums-/Dezimalformate, Kopfzeilen-Mapping für echte Schulverwaltungsdaten, Fehlermeldungen pro Zeile, Vorschau und explizite Bestätigung. Geänderte Datensätze **nicht still ersetzen**. Vorgeschlagene Klassen- und Bildungsgangänderungen mit Wirksamkeitsdatum.
- Schuljahreswechsel durch Lehrkraft ausgelöst: Vorschau, automatische Versetzungsvorschläge, verpflichtender neuer Schülerimport, individuell klären wer fehlt (aktiv vs. ausgeschieden), Abschluss erst nach vollständiger Klärung, Backup vorher.
- Ausschiedene archiviert, keine neuen Münzbuchungen; Rückkehr erzeugt neue Schulbesuchsepisode mit null Münzen, vorherige getrennt. Neue Zugänge starten bei null; externe Leistungen nur als einzeln begründete Herkunfts-/Münzdatensätze.
- Klassenwiederholung: *alle* Münzen aus dem vorherigen Durchgang der wiederholten Klassenstufe sofort aus dem wirksamen Blocksaldo ausschließen, alte Ausstellungen auf `UNGUELTIG_WIEDERHOLUNG`, Historie behalten, Rücknahme reversibel und auditiert; Münzgrenzen nicht erneut addieren. **Erwartung mit Tests für Wiederholung in 9 BR, Klassenwechsel während Jahr und Wechsel Sek I→Oberstufe.**
- Drei Jahre nach Ausscheiden als konfigurierbare Retention-Vorgabe mit rechtlichem Freigabevorbehalt; Löschautomatik zunächst **nicht aktivieren**, bevor DSB/Schulverwaltung Frist und Ausnahmen freigeben. Löschvorschau und Löschprotokoll bevorzugen.

## 3. Münz- und Praktikumsregeln – testgetrieben und deterministisch

- Veranstaltung: Pflichtangaben Name, Enddatum, Münzwert; Standard Wahlpflicht; mindestens 0,5 und in 0,5er-Schritten. >20 mit Warnung und bestätigender Aktion **nicht komplett verbieten**. Nach erster Buchung Eventname/Enddatum nur über protokollierte Korrekturfunktion. Katalogmünzwertänderung nur zukünftig.
- Klassenseitige Buchung nur nach Abschlussdatum des Events, individuelle Verringerung gegenüber Standard zulässig; externe tatsächliche Teilnahmeverwaltung außerhalb des Systems. Teilleistungsberechnung bei mehrtägigen Events kaufmännisch auf 0,5.
- Wiederholte Teilnahme derselben Wahlpflicht-Äquivalenzgruppe in einem Schuljahr nur einmal gewertet; jede Durchführung eigenständig. Gleiche Workshops maximal einmal/Jahr, **verschiedene** Netzwerkpartner-Workshops je 2 Münzen unbegrenzt; 2 verschiedene Mindest- statt Höchstanforderung.
- Mandatory Praktikum je Block ein Mal, 10 Münzen/anerkanntem Tag, max. 10 Tage = 100; Sek I und Oberstufe unabhängig. Erste finale Pflichtpraktikumsabrechnung ist verbindlich; keine weiteren Nachholtage, nur auditierte Fehlerkorrektur. Zusatzpraktikum max. 5 Tage = 50 Münzen, **ein** anrechenbares Zusatzpraktikum/Schuljahr.
- Event-Abbruchstatus verhindert Neubuchungen; Altbuchungen `PRUEFUNG_ERFORDERLICH`.
- Pflichtveranstaltungen überwachen, aber **keine weitere Zertifikatsausgabesperre** außer Münzgrenze.
- Rückdatierte Einträge und Münzkorrekturen markieren alle zum Stichtag betroffenen Zertifikate überprüfungsbedürftig; keine automatische unsichtbare Dateikorrektur.
- Bestehende Demo-Routen `/api/grants` und Raster-Batch müssen dieselben **serverseitigen** Regeln erzwingen, sonst deaktivieren. Nicht nur im Frontend prüfen.
- Automatisches Speichern **nur Entwurf**; endgültige Buchung als explizite atomare, idempotente Transaktion mit verständlicher Rückmeldung.

## 4. Kumulative Zertifikatsgrenzen – exakte Abnahme-Werte

| Jahrgang/Bildungsgang | Erfolg | Besonderer Erfolg | Auszeichnung |
|---|---:|---:|---:|
| 8 BR | 11 | 18 | 22 |
| 8 V10/S1/Ü11 | 10 | 17 | 21 |
| 9 BR | 128 | 151 | 171 |
| 9 V10/S1/Ü11 | 24 | 47 | 67 |
| 10 S1/Ü11 | 40 | 70 | 97 |
| 11 MSS | 116 | 137 | 153 |
| 12 MSS | 132 | 160 | 183 |
| 13 MSS | kein Zertifikat | kein Zertifikat | kein Zertifikat |

Alle Werte gelten als **kumulative Untergrenzen im aktuellen Block**, nicht als nur in diesem Jahr erworbene Münzen. Grad 12 enthält bereits die addierten Werte aus 11+12; bei Wiederholung eines Jahrgangs keine erneute Schwellenaddition. Fehlende/mehrdeutige Regel = Ausstellung gesperrt, Zählung erlaubt. Unter Mindestwert = `Noch kein Zertifikat`, nächster Schwellenabstand angezeigt.

Implementiere versionsfähige Regelverwaltung durch alle fachlich berechtigten Lehrer, Änderungen für definiertes künftiges Schuljahr, vollständiges Audit; Zertifikatsberechnung nach gewähltem Stichtag und historischem Bildungsgang.

## 5. Dokumentenausgabe

- PDF **und** editierbares DOCX, identisch bezüglich Inhalt, Layout professionell, maximal 2 A4-Seiten. Slogan `Orientierung – Entscheidung – Bewerbung`.
- Offizieller Schulkopf von **IGS Am Nanstein**, Konrad-Adenauer-Straße 10, 66849 Landstuhl; Telefon 06371-403530; Fax 06371-4035317; `sekretariat@igs-landstuhl.de`; `www.igs-landstuhl.de`. Bildlogo/Letterhead aus ursprünglich bereitgestellter Vorlage, falls Datei auf dem Codex-Server rechtmäßig verfügbar; andernfalls Konfigurationsupload und **kein erfundenes Logo**.
- PDF/DOCX korrekt mit Namen, Klasse, Jahrgang, aktuellem Schuljahr, aktueller Stufe, *nur diesjähriger* Eventliste, Vorjahresmünzen des aktuellen Blocks, Jahresmünzen, Blocksaldo; Praktika getrennt nach Typ, Unternehmen, Zeitraum, Tagen und Coins.
- Optionale gekennzeichnete Entwurfs-PDF-Vorschau; endgültige Ausstellung erst nach serverseitiger Datenprüfung (ohne zusätzliche Freigabe). Zertifikate nur oberhalb Mindestgrenze; kein Jahrgang-13-Zertifikat.
- Signaturdatei nur durch Admin verwalten, verschlüsselt/geschützt speichern, Unterschriftsverwendung pro Ausstellung loggen. Nicht über rohe öffentliche URLs abrufbar; keine Signatur ins Repo.
- Klassenausgabe nach Klasse→Nachname→Vorname; ein Sammel-PDF + ZIP aus Einzeldokumenten PDF/DOCX. Bei fehlerhaften einzelnen Datensätzen Liste zeigen und Lehrkraft zwischen nur korrekten Ausgaben oder Gesamtstopp wählen.
- Stichtags- und Versionierungssystem; alte Versionen bleiben archiviert, nicht überschreiben.
- PDF-Ausgabe und DOCX-Ausgabe als integrierter End-to-End-Test mit textuellen Inhalten, Seitenlimit, Sortierung und numerischen Grenzwerten; reproducible font/layout installation im Dockerfile. Keine ungesicherten externen Dokumentrenderer starten.

## 6. Benutzerrollen, Sicherheit und Datenschutz

Vorhandenen Spring-Security-Code kritisch auditieren und korrigieren. Er ist noch keine Abnahme.

- Eigene Benutzer; `ADMIN` verwaltet Benutzer, Upload Unterschrift. `TEACHER` kann alle Schülerdaten, Fachregeln und Zertifikate bearbeiten. Admin kann fachliche Funktionen ebenfalls nutzen.
- Admin legt dauerhafte Passwörter ohne Wechselpflicht an; freiwillige Passwortänderung für Nutzer; Passwörter gesalzen mit zeitgemäßem Passwortencoder; keine Geheimnisse in Logs, Git oder URL.
- Fehlversuche auditieren, **keine dauerhafte automatische Kontosperre**. Pro-IP/Benutzer-Ratenbegrenzung ohne ungeprüfte Weitergabe von `X-Forwarded-For`. Keine unbeschränkt wachsenden In-Memory-Sammlungen.
- 30 Minuten Inaktivität; Warnung vor Ablauf; verlässlich **serverseitige** Session-Zeit und sichere Session-Cookies `Secure`, `HttpOnly`, `SameSite`, CSRF für jede mutierende Aktion.
- Nginx terminiert HTTPS; Spring Boot muss `X-Forwarded-Proto/Host/Port` berücksichtigen, aber die Header dürfen ausschließlich von einem vertrauenswürdigen Reverse Proxy stammen. Der Webcontainer darf auf Hostseite nur **127.0.0.1** exponiert werden, niemals `0.0.0.0`, sobald produktiv. Nginx auf gesondertem HTTPS-Port, ohne andere HTTPS-Dienste auf dem Host umzubauen.
- Zertifikat mit IP-SAN und regulärer Vertrauenskette der verwalteten Schulgeräte. **Ein per Browserausnahme akzeptiertes selbstsigniertes Zertifikat ist keine hinreichende Freigabe**. Bestehender Reverse-Proxy-Server des Schulservers wird außerhalb GitHub durch IT administriert.
- Auditlog unveränderlich für Münzen/Korrekturen/Schuljahreswechsel/Zertifikate/Signatur/Benutzer/Regeln. Sorgfältiger Datenschutz: keine personenbezogenen Daten in öffentlichen URL-Parametern, Fehlerseiten, Beispiel-Dateien, CI-Artefakten, Server-Logs oder öffentlichen Repos.
- Einvernehmliche Berechtigungsprüfung: Lehrer dürfen alle BSO-Fachdaten einsehen/editieren, Admin allein Benutzerverwaltung und Signaturgrafik. Jede API wird serverseitig geschützt.
- Durch Fachentscheidung **Datensatzsperren**: während Bearbeitung anderer Lehrer ausgesperrt, Ablauf bei Verbindungsabbruch, zusätzliche Optimistic-Lock-/Transaktionsprüfung gegen verlorene Updates; keine Datenhaltung über Browser allein.
- Spezieller Schutz von Uploads (Typ, Größe, Malware, Verzeichniszugriff), PDF/DOCX und Dateiablage, HTTP-Security-Header und Redirects.

## 7. Praxis-UX für Lehrkräfte (zentrales Kriterium)

Das bestehende Klassenraster/CSV-Import ist fachlich unvollständig und soll **professionell überarbeitet** werden. Bitte keinen administrativen Formulardschungel bauen.

- Startseite: Schule/Schuljahr, Suchfeld, Klasse wählen, „CSV importieren“, „Veranstaltung wählen“, „Münzen vergeben“, „Zertifikate“, „Berichte“.
- Schülerstammdaten-CSV/XLSX mit Vorschau „neu/geändert/identisch/fehlerhaft“, exportierbarem Fehlerbericht, Abgleich per stabiler ID. Import nicht bei jedem Aufruf sichtbar machen, aber in 1–2 Klicks erreichbar.
- Veranstaltungsimport analog; Typ Pflicht/Wahlpflicht, Jahrgang/Bildungsgang, Workshopzuordnung, Datum und Standardwert erfassen; jede tatsächliche Durchführung separat.
- Klasse→Event→Raster: sortierte Schüler, Standardmünzen, massenweise Auswählen/Abwählen, individuelle Korrektur in 0,5er-Schritten, Hinweis „schon vergeben“, kompakte Summen, automatischer Entwurf, große eindeutige endgültige Schaltfläche und Rückmeldung. Doppeltklick/Retry darf nicht doppelt buchen.
- Desktop-Tastaturbenutzung, sichtbare Validierung, keine unnötigen Dialoge; leicht verständliche deutsche Beschriftung, Barrierefreiheit nach angemessenem Standard.
- Schülerdetailseite mit Münzblöcken, Vorjahres-/Jahressummen, Zertifikatsstufe, Veranstaltungen, Praktika und Audit-Historie.
- Klassenlisten mit Filtern und Status, CSV/XLSX Export. Übersicht der Mindestanforderungen und fälligen Zertifikate.

## 8. Berichte und Administration

- Filter Jahrgang, Klasse, Bildungsgang, Schuljahr, Münzbereich, Zertifikatsstatus und fehlende Schwelle.
- Diagramme/Tabellen mit PDF-Berichten, XLSX und CSV exportierbar; Schutz gleichermaßen wie Fachdaten.
- Einrichtungsassistent (nur initial oder schrittweise) mit Admin, Startjahr 2026/27, Regeln, Schulkopf/Signatur und Erstimport.
- Backups täglich automatisch (PostgreSQL + Zertifikats- und Signaturablage), 30 Tage Vorhaltung, vor Klassenwechsel und Updates zusätzlich; keine Passwörter im Backupskript, Zugriffsrechte, Tests der Wiederherstellung auf separater Instanz. Backup-Ziele **nicht** im öffentlichen Repo hardcoden.
- Kontrollierter Updateablauf inkl. Schema-Migration, Vorab-Backup und Rollbackplan. Kein Datenverlust und keine Beeinträchtigung fremder Container.
- Ausschiedene und Zertifikate aufbewahren während Schulzeit + grundsätzlich 3 Jahre nach Austritt, **nur vorbehaltlich DSB-Rechtsprüfung**; automatische Löschung erst danach aktivieren.

## 9. Verbindliche Testfälle und Definition of Done

Die T01–T25 aus [docs/PFLICHTENHEFT.md](PFLICHTENHEFT.md) sind **Pflicht**; zusätzlich:

- `mvn verify` grün, PostgreSQL-Integrationstests grün, Container-Build grün und reproduzierbar; Datenbankmigration von der aktuellen Demo mit **nichtleerem** Testdatenbestand nachgewiesen.
- CSV: Duplikat-IDs, führende Nullen, BOM, Semikolon/Komma, fehlerhafte Zeilen, wiederholter Import, Klassen-/Bildungsgangwechsel, Transaktions-Rollback, CSV-Injection bei Export.
- Raster: 30 Schüler in einer Batchtransaktion, Entwurf→Wiederaufnahme→Finalisierung, Konkurrenz zweier Lehrkräfte, Netzabbruch, doppelte POST-Anfragen, Klassenmitgliedschaft am fachlichen Eventdatum, korrekter Schuljahresblock.
- Münzen: 0,5-Grenzfälle, 1,25→1,5 Rundung, Null-/Negativwerte abweisen, Standardwert nach bereits gebuchten Münzen geändert, Event-Äquivalenz über mehrere Verknüpfungen, Workshops Mindestwert vs. unbegrenzt.
- Praktika: max. 100 Pflicht je Block, max. 50 freiwillig pro Jahr; zweite Buchung und Nachträge korrekt abgewiesen, auditierte Fehlerkorrektur zugelassen.
- Wiederholung: sofortiger Ausschluss bisheriger Buchungen derselben Klassenstufe und Ungültigkeitsstatus; Rücknahme korrekt und auditierbar.
- Zertifikat: Klasse 9 BR Schwellen 127,5/128, 150,5/151, 170,5/171; Klasse 10 S1 39,5/40; Klasse 12 131,5/132; Jahrgang 13 gesperrt; fehlende Regel gesperrt. Stichtag, Rückdatierung, Versionierung, Batch mit 3 Fehlern, maximal 2 Seiten, geschützte Signatur.
- Security: ungeauthentifizierte Lese-/Schreibzugriffe gesperrt; TEACHER kein Adminzugriff; 30-Minuten-Timeout; CSRF auf Upload/Import/Änderung; HTTP→HTTPS/Port-Weiterleitung ohne Herabstufung; sichere Cookies; Zugriff auf fremde/ungültige Objekte; Schutz vor path traversal/SQL injection.
- Ein produktionsnahes `docker compose up -d --build` auf **separater Testinstanz** nachweisen; gesicherte Backups zurückspielen und Daten/Dateien validieren.
- README, Installations-/Update-/Restore-Handbuch in Deutsch mit konkreten Kommandos; **keine** Anleitung zum Öffnen eines ungesicherten BSO-Ports für Echtdaten.
- Unvollständige Punkte klar auf einer `docs/RELEASE_READINESS.md` dokumentieren mit Ampel je Kriterium, reproduzierbaren Testresultaten und einer Liste echter Freigabe-Blocker.

**Fachliche Abnahme:** BSO-Koordination. **Technische Abnahme:** Schul-IT. Beide müssen freigeben; ein CI-Grün oder „funktioniert im Browser“ ersetzt diese Freigaben nicht.

## 10. Arbeitsreihenfolge für einen einzigen größeren Codex-Durchgang

1. Code- und Sicherheitsanalyse, Datenbankmigration ohne Löschung, Testgrundgerüst.
2. Fachmodell, Ledger, Schuljahre, Klassen-/Trackhistorie, Prüfregeln, Praktika, Wiederholungen; mit Tests.
3. CSV/XLSX-Import, Event-Import, Klassenraster, Drafts, Sperren; einfacher Workflow.
4. Schwellen, Zertifikatsberechnung, PDF/DOCX, Signaturablage, Versionierung/Sammelausgabe.
5. Administration, Audit, Backup/Restore, Reports, Onboarding und Korrekturen der Sicherheit.
6. End-to-End-Tests, CI, Dokumentation, Release-Gate, Pull Request und konkrete Upgrade-Kommandos.

**Arbeite so weit wie möglich durch.** Ist der Gesamtumfang nicht in einem Durchgang realisierbar, implementiere einen **vollständig funktionsfähigen vertikalen Kern** (Import → Klassenerfassung → regelkonformes Ledger → Stufenauswertung → PDF/DOCX) inklusive Tests; vermeide die Abgabe nur vieler halbfertiger Screens. Danach weitere Anforderungen nach Priorität abarbeiten. Hinterlasse dokumentiert, was implementiert, geprüft und offen ist.

## 11. Übergabeformat des Agents

Am Ende im Pull Request bzw. `docs/RELEASE_READINESS.md`:
- Implementierte Anforderungen mit Verweisen auf Code und Tests.
- Testergebnisse (`mvn verify`, Integration, Docker), Migrationsnachweis, Backup-/Restore-Nachweis.
- Sicherheitsrisiken und blockierende Abnahmen, ohne Beschönigung.
- Betriebssichere Update-Anleitung für bestehenden Server einschließlich Erhalt der DB und des internen HTTPS-Reverse-Proxys.
- **Keine automatische Produktionsfreigabe**, keine Echtdatenimporte, keine unkontrollierte Änderung der anderen Docker-Dienste.

**Wichtig:** Die spätere Produktivnahme soll **möglichst bald** erfolgen – aber nicht durch Weglassen von Authentifizierung, sicherer Zertifikatsvertrauensstellung, Zugriffsschutz, Backup/Restore, Datenschutzprüfung oder gemeinsamer Schulfreigabe.

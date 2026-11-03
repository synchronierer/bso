# Pflichtenheft – BSO-Münz- und Zertifikatssystem

**IGS Am Nanstein · Version 1.0 · Stand 03.11.2026 · Status: fachliche Anforderungsbasis, technisch noch nicht abgenommen**

> Dieses Repository ist öffentlich. **Keine Schülerdaten, Zugangsdaten, Zertifikate realer Personen oder Unterschriftsgrafiken einchecken.** Das Dokument beschreibt die zu entwickelnde Anwendung, keine bereits implementierte Funktion.

## 1. Ziel und Geltungsbereich

Das System verwaltet BSO-Münzen, BSO-Veranstaltungen, Praktika, Pflichtveranstaltungs-Erfüllung, Schülerakten, Zertifikatsgrenzen und Ausgaben von Zertifikaten an der Integrierten Gesamtschule Am Nanstein. Die Lehrkräfte nutzen eine zentrale Webanwendung auf PCs/Laptops im internen Schulnetz. Einzel- und Sammelzertifikate werden als PDF und DOCX erzeugt. Die fachliche Grundlage sind das bereitgestellte BSO-Konzept, die Zertifikatsvorlage, der offizielle Schulbriefkopf sowie die Entscheidungen aus 115 Anforderungsfragen (einschließlich späterer Korrekturen).

**Verbindliche Auslegungsregel:** Spätere ausdrückliche Festlegungen ersetzen widersprechende frühere Antworten und Vorlagenwerte. Technische, rechtliche und redaktionelle Abweichungen werden als offene Abnahmepunkte gekennzeichnet.

## 2. Fachliche Kernregeln

### 2.1 Münzblöcke und Start

- Offizieller Systemstart ist **2026/2027**. Bei Einführung beginnen alle Bestands-Schülerinnen und -Schüler mit **0 Münzen**; frühere Leistungen werden nicht nachträglich importiert.
- **Block Sekundarstufe I:** Jahrgänge 7–10. Innerhalb dieses Blocks werden Münzen über Schuljahre kumuliert; Jahrgang 7 erwirbt 2026/2027 keine Münzen.
- **Block Oberstufe:** Jahrgänge 11–13. Bei Eintritt in Jahrgang 11 beginnt ein neuer, unabhängiger Münzstand bei 0. Münzen aus 7–10 bleiben historisch sichtbar, zählen aber nicht für Oberstufenzertifikate.
- Jahrgang 13: Münzen erfassen, **keine neuen Zertifikate ausstellen**.
- Die Jahrgangs-/Bildungsgang-Zertifikatsgrenzen werden innerhalb des Blocks kumulativ gebildet (nicht erneut in einem Wiederholungsjahr); es gelten die explizit freigegebenen Werte in Abschnitt 3.
- Der Gesamtstand ist die Summe der im jeweiligen Block wirksamen Münzbuchungen, abzüglich bei Klassenwiederholung ausgeschlossener Buchungen; keine stillen Überschreibungen.

### 2.2 Münzwerte, Teilnahmen und Veranstaltungen

- Neue Veranstaltungen: Pflichtangaben **Name, Abschlussdatum, Münzwert**. System vergibt eine dauerhafte Event-ID. Schuljahr wird aus dem Abschlussdatum ableitbar geführt; andere Felder optional.
- Standardtyp **Wahlpflicht**; Lehrkräfte können ausdrücklich **Pflicht** mit Zieljahrgang/-bildungsgang festlegen. Pflichtveranstaltungen sind jahrgangsgebunden; Wahlpflichtangebote jahrgangsübergreifend.
- Jede Durchführung ist ein eigenständiger Eventdatensatz. Gleichartige Durchführungen können durch Referenz auf ein bereits vorhandenes Angebot verbunden werden (transitive Gleichartigkeit). Dieselbe Wahlpflicht-Angebotsgruppe darf **pro Schüler und Schuljahr einmal** Münzen bringen; verschiedene Netzwerkpartner-Workshops gelten als verschiedene Angebote.
- Normale Veranstaltungswerte mindestens **0,5 Münzen** in **0,5er-Schritten**. Werte über 20 erfordern bestätigte Plausibilitätswarnung. Praktika folgen gesonderten Regeln.
- Lehrkräfte erfassen **keine Anwesenheitslisten** im System. Nach Abschluss einer Veranstaltung markieren sie Empfänger in einer klassenfilterbaren Liste; der Standardmünzwert wird vorgeschlagen und kann individuell **nach unten in 0,5er-Schritten** angepasst werden. Erst durch ausdrückliches „Endgültig verbuchen“ entstehen Buchungen. Eingabeentwürfe werden automatisch gespeichert.
- Bei mehrtägigen Veranstaltungen werden extern ermittelte Teilnahmetage anteilig bewertet; das Ergebnis wird **kaufmännisch auf die nächsten 0,5 Münzen** gerundet. Die Anwendung dokumentiert nur die final vergebenen Münzen, nicht die Anwesenheitstage.
- Münzbuchungen gelten fachlich ab **Abschlussdatum** der Veranstaltung; technischer Erfassungszeitpunkt und Benutzer werden getrennt gespeichert.
- Änderungen des Katalog-Münzwerts gelten nur für künftige Vergaben. Bereits gebuchte Münzwerte werden als unveränderliche historische Werte gespeichert, abgesehen von protokollierten Einzelkorrekturen.
- Veranstaltungsname und Abschlussdatum sind ab erster Buchung normal gesperrt; Änderungen nur über eine gesonderte protokollierte Korrekturfunktion. Datumsänderungen lösen eine Prüfung betroffener Zertifikate aus.
- Alle Lehrkräfte dürfen neue Veranstaltungen und Münzwerte anlegen und bestehende nach den Regeln korrigieren.
- Duplikate können nach prüfender Vorschau zusammengeführt werden; doppelte Münzansprüche sind auszuschließen. Abgesagte Events bleiben mit Status „Abgesagt“, blockieren neue Buchungen und markieren vorhandene Buchungen zur Prüfung.

### 2.3 Netzwerkpartner-Workshops

- **Zwei verschiedene Workshops je einschlägiger Jahrgangsstufe sind Mindestanforderung**, keine Höchstgrenze.
- Jeder unterschiedliche anerkannte Workshop bringt **2 Münzen**. Zusätzliche unterschiedliche Workshops dürfen ohne Jahresobergrenze gezählt werden.
- Eine erneute Teilnahme am **gleichen** Workshop zählt innerhalb eines Schuljahres maximal einmal; weitere Schuljahre können erneut angerechnet werden.
- Die Erfüllungsanzeige überwacht die Mindestzahl; fehlende Pflichtangebote sind **kein** zusätzliches Zertifikatshindernis.

### 2.4 Praktika

- Pflichtpraktikum **Sek I:** einmal bis **100 Münzen**, 10 Münzen je anerkanntem Tag, maximal 10 Tage.
- Pflichtpraktikum **Oberstufe:** unabhängig davon einmal bis **100 Münzen**, gleiche Bewertung.
- Freiwilliges Zusatzpraktikum: **10 Münzen je anerkanntem Tag**, höchstens **50 Münzen pro Praktikum**; maximal **ein** anrechenbares freiwilliges Praktikum pro Schuljahr (damit max. 50 Münzen pro Schuljahr).
- Bei Pflichtpraktika gilt die **erste endgültige Speicherung** der Münzen als Abrechnungsschluss; nachgeholte Tage werden anschließend nicht zusätzlich angerechnet. Fehlerhafte Eingaben dürfen über eine protokollierte Korrektur berichtigt werden.
- Praktika werden mit Art, Betrieb, Zeitraum, anerkannten Tagen, Münzen und Schüler-/Schuljahr-/Blockzuordnung geführt. Auf Zertifikaten eigener Praktikumsabschnitt mit diesen Angaben für das aktuelle Schuljahr.
- Die Prüfung verhindert doppelte Pflichtpraktikumsabrechnung je Block, Überschreiten von Obergrenzen sowie mehr als ein Zusatzpraktikum pro Schuljahr.
- Die fachliche Abgrenzung von regulären Pflichtpraktika und einzelnen Schulbesuchsepisoden muss beim Klassenwechsel erhalten bleiben.

### 2.5 Pflichtveranstaltungsmonitoring

- Zuordnung von Pflichtangeboten zu Jahrgang und Bildungsgang ist konfigurierbar.
- Anzeige der bereits erfüllten und noch fehlenden Pflichtangebote; **Zertifikate hängen ausschließlich von Münzgrenzen ab**, nicht von der Vollständigkeit des Pflichtkatalogs.
- Bei Bildungsgangwechsel gelten bis zum Wechsel die bisherigen Anforderungen, danach die neuen. Historische Anforderungen bleiben erhalten; keine rückwirkende Umdeutung durch spätere Wechsel.
- Keine gesonderte Nachholfunktion für frühere Pflichtangebote. Spätere Buchungen gelten regulär am tatsächlichen Abschlussdatum.

## 3. Zertifikatsgrenzen

Alle Werte sind **Untergrenzen**, sodass halbe Münzen lückenlos eingestuft werden. Wenn keine aktive, eindeutig gültige Regel für Kombination aus Schuljahr, Stichtag, Jahrgang und Bildungsgang vorhanden ist, wird die Zertifikatsausgabe gesperrt, die Münzberechnung aber fortgesetzt. Änderungen werden versioniert und gelten erst ab einem definierten Schuljahr; bereits ausgestellte Zertifikate werden nicht automatisch überschrieben.

| Jahrgang / Bildungsgang | Mit Erfolg ab | Mit besonderem Erfolg ab | Mit Auszeichnung ab |
|---|---:|---:|---:|
| 8 BR | 11 | 18 | 22 |
| 8 V10/S1/Ü11 | 10 | 17 | 21 |
| 9 BR | 128 | 151 | 171 |
| 9 V10/S1/Ü11 | 24 | 47 | 67 |
| 10 S1 / Ü11 | 40 | 70 | 97 |
| 11 MSS | 116 | 137 | 153 |
| 12 MSS | 132 | 160 | 183 |
| 13 MSS | – | – | – |

- Klasse 9: explizit bestätigt kumulativ aus Jahrgang 8 + Jahrgang 9; Klasse 10 analog aus V10/S1/Ü11 + Jahrgang 10.
- Stufe 12: explizit bestätigt aus entsprechenden Stufengrenzen 11 + 12 (116+16, 137+23, 153+30).
- Jahrgang 7: im Startjahr keine Münzen, keine Zertifikatsregeln.
- Unter Mindestgrenze: **kein Zertifikat**, Status „Noch kein Zertifikat“, aktueller Blockmünzstand und fehlende Münzen zur nächsten Stufe.
- Alle berechtigten Lehrkräfte dürfen Regelversionen erstellen/ändern; jede Änderung erhält Zeitstempel, Benutzer, alten/neuen Wert und Gültigkeitsbeginn.
- Bei Bildungsgangwechsel ist die zum **Zertifikatsstichtag** zutreffende aktuelle Zuordnung für die Regelwahl maßgeblich; bereits erworbene Münzen bleiben wirksam.

## 4. Schülerverwaltung und Lebenszyklus

- Stammdaten werden aus **XLSX/CSV** importiert; manuelle Ergänzung/Korrektur möglich.
- Maßgeblicher Identifikator ist die über alle Schuljahre stabile **Schüler-ID aus der Schulverwaltung** (als Zeichenfolge speichern; führende Nullen erhalten). Keine Zuordnung allein anhand des Namens.
- Folgeimporte zeigen Änderungen an Namen, Klassen und Bildungsgängen mit Vorschau und verlangen Bestätigung.
- Unterjährige Klassenwechsel erhalten Münzen und historische Klassenbezüge; ab Wechsel gilt die neue Klasse.
- Unterjährige Bildungsgangwechsel werden mit Wirksamkeitsdatum gespeichert; Pflichten gelten vor/nach Wechsel wie beschrieben.
- Schuljahreswechsel: gestarteter, prüfbarer und bestätigungspflichtiger Ablauf mit automatischen Versetzungsvorschlägen und **verpflichtendem XLSX/CSV-Abgleich**. Abweichungen sind zu bestätigen. Im Import fehlende Schüler sind einzeln als aktiv/ausgeschieden zu klären; solange offene Fälle existieren, kein Abschluss.
- Ausgeschiedene werden archiviert und von neuen Buchungen ausgeschlossen. Rückkehr: **neue Schulbesuchsepisode mit 0 Münzen**, alte Episode getrennt und unverändert archiviert. Dieselbe Schulverwaltungs-ID bleibt als Personenzuordnung erhalten.
- Aufbewahrung: während des Schulbesuchs und grundsätzlich **drei Jahre nach Ausscheiden**; danach Löschung der personenbezogenen Daten/Zertifikate **vorbehaltlich geltender rechtlicher Aufbewahrungspflichten**. Fristbeginn pro Episode und Behandlung erneuter Schulaufnahme sind vor Produktivsetzung datenschutzrechtlich zu prüfen.
- Neue Schüler beginnen mit null Münzen. Externe Leistungen dürfen von Lehrkräften **einzeln und individuell** bewertet und mit Herkunft und anerkanntem Münzwert dokumentiert werden; **keine Dateiablage von Nachweisen** notwendig. Blockzuordnung verpflichtend.

### 4.1 Klassenwiederholung (spätere Spezialregel geht vor)

- Bei bestätigter Wiederholung werden **sämtliche** Münzen aus dem bisherigen Durchgang **dieser wiederholten Klassenstufe** aus dem wirksamen Münzstand ausgeschlossen, sowohl Pflicht- als auch Wahlpflichtmünzen. Münzen aus früheren Klassenstufen bleiben erhalten.
- Die Münzen des Wiederholungsjahres müssen neu erworben werden. Die Zertifikatsgrenzen werden wegen der Wiederholung **nicht ein weiteres Mal addiert**.
- Historische Buchungen werden nicht gelöscht. Bestätigung der Wiederholung wirkt **sofort**, nicht erst beim Schuljahreswechsel.
- Bereits ausgestellte Zertifikate erhalten den **Verwaltungsstatus „Ungültig wegen Klassenwiederholung“**, bleiben aber zur Nachvollziehbarkeit archiviert.
- Rücknahme einer versehentlich bestätigten Wiederholung stellt ausgeschlossene Buchungen und soweit konsistent Zertifikatsstatus wieder her; vollständiges Audit.
- **Prüfpunkt:** Wie spätere, noch innerhalb der wiederholten Jahrgangsstufe stattfindende Schulbesuchsepisoden und zugeordnete Buchungen voneinander unterschieden werden, ist durch Testfälle abzusichern.

## 5. Zertifikate und Dokumentenerzeugung

- Zertifikate jederzeit zu frei wählbarem **Stichtag**. Nur bis zu diesem Tag fachlich gültige Münzen und zum Stichtag gültige Schüler-/Regeldaten zählen.
- Einzel- und Klassen-/Jahrgangsausgabe als **PDF plus bearbeitbares DOCX**.
- Zertifikat enthält den offiziellen **Schulkopf der IGS Am Nanstein** (aus bereitgestellter Briefvorlage), das Leitmotiv „Orientierung – Entscheidung – Bewerbung“, Name, Klasse/Jahrgang, korrekt formulierte Zertifikatsstufe, Veranstaltungsliste des aktuellen Schuljahres, Münzen aus Vorjahren **des aktuellen Blocks**, Münzen des aktuellen Schuljahres, Blockgesamtstand, eigenen Praktikumsabschnitt sowie BSO-Unterschrift.
- Sprachstandard: **„Mit Erfolg erworben“**, **„Mit besonderem Erfolg erworben“**, **„Mit Auszeichnung erworben“**; nicht „Mit Auszeichnungen“.
- Maximal **2 DIN-A4-Seiten** pro Zertifikat, ohne unleserliche Schrift oder abgeschnittene Tabellen. Längere Ausgaben sind als Prüf-/Layoutfehler zu melden, nicht still zu kürzen.
- Eingebettete Unterschriftsgrafik: nur Administration darf sie hochladen/austauschen; Lehrkräfte dürfen sie ausschließlich bei autorisierter Zertifikatserstellung nutzen. Verwendung protokollieren. Keine Unterschriftsgrafik in GitHub.
- Vorschau als gekennzeichnetes Entwurfs-PDF **optional**; nach erfolgreicher automatischer Prüfung kann sofort ausgestellt werden.
- Klassenstapel: sortiert nach **Klasse, Nachname, Vorname**. Ausgaben: ein **Sammel-PDF** und ein **ZIP** mit Einzel-PDF und Einzel-DOCX. Jedes Zertifikat beginnt auf einer neuen Seite; zur korrekten Druckreihenfolge und zu Duplexdruck ist der Satzspiegel zu testen.
- Bei fehlerhaften einzelnen Datensätzen zeigt das System die Fehler an und lässt die Lehrkraft entscheiden: **nur fehlerfreie Zertifikate** ausgeben oder **gesamten Stapel zurückstellen**. Keine Ausgabe eines als fehlerhaft eingestuften Zertifikats.
- Nachträgliche Buchungen/Korrekturen, die einen Zertifikatsstichtag betreffen, setzen betroffene Zertifikate auf **„Prüfung erforderlich“**. Eine neue Version wird nur durch bewusste Lehrkraftaktion erstellt; alte Version bleibt als „Ersetzt“ erhalten.
- Zertifikatversionen und Status werden nachvollziehbar gespeichert; bereits ausgegebene Dateien werden nicht heimlich ersetzt.
- Zertifikate und Versionen unterliegen derselben datenschutzrechtlich zu prüfenden Aufbewahrungsregel wie Schülerakten.

## 6. Bedienoberfläche und Auswertungen

- Browser-Webanwendung, nur für **Desktop/Laptop** optimiert.
- Globale Suche nach ID, Vorname, Nachname; Filter nach Klasse, Jahrgang, Bildungsgang, Schuljahr, Münzstand, Zertifikatsstufe und fehlender Mindestgrenze.
- Klassenübersicht: Schüler, Klasse, Bildungsgang, Jahresmünzen, Blockgesamtstand, Zertifikatsstufe, Differenz zur nächsten Stufe.
- Individuelle BSO-Akte: Schulbesuchsepisoden, Historie, wirksame/ausgeschlossene Münzen, Veranstaltungen, Praktika, externe Anerkennungen, Zertifikate samt Status/Version.
- Veranstaltungserfassung über klassenfilterbare Schülerliste mit Kontrollkästchen und individuell reduziertem Münzwert; Entwürfe werden automatisch zwischengespeichert, Buchung nur nach endgültiger Bestätigung.
- Berichte und Statistiken für BSO-Koordination und Schulleitung: Münzverteilung, Zertifikatsstufen, fehlende Mindestgrenzen, Pflichtangebote, Praktika, Veranstaltungen, Jahrgangs- und Klassenvergleiche. Diagramme und Berichte als **PDF**, Tabellen zusätzlich **XLSX/CSV**.

## 7. Rechte, Datenschutz und IT-Sicherheit

- Eigene Benutzerverwaltung mit individuellen Benutzernamen/Passwörtern.
- Administration richtet Benutzerkonten ein, vergibt dauerhafte Startpasswörter **ohne erzwungenen Erstwechsel**, kann sie zurücksetzen; Nutzer dürfen ihre Passwörter selbst ändern.
- Alle angemeldeten Lehrkräfte haben **gleiche fachliche Rechte**, einschließlich aller Schülerdaten, Münz- und Regeländerungen, Veranstaltungen sowie Zertifikatsausgaben; **Benutzerverwaltung und Unterschriftsgrafik** nur durch Administration.
- Passwortspeicherung nur als aktuelle, gesalzene Passwort-Hashes; keine Klartextpasswörter, keine Geheimnisse im Repository.
- Fehlgeschlagene Logins protokollieren; keine automatische **Kontosperre**. Technische Ratenbegrenzung zum Schutz gegen automatisierte Angriffe vorsehen.
- **30 Minuten Inaktivität** bis zur automatischen Abmeldung, mit Vorwarnung; Sitzungen serverseitig beenden.
- Zentrale Zugriffs- und Änderungsprotokolle: Benutzer, Zeitpunkt, Objekt-ID, alter/neuer Wert, Aktion, bei Korrekturen kein verpflichtender Begründungstext.
- Vorübergehende **Datensatzsperren** während der Bearbeitung; Freigabe beim Speichern/Abbruch, automatisches Ablaufverfahren bei Netzverlust. Technische Transaktions-/Versionsprüfung als zusätzliches Sicherheitsnetz empfohlen.
- HTTPS auf interner **Server-IP**; gültiges, auf die IP ausgestelltes und von Schulclients vertrautes TLS-Zertifikat; keine öffentliche Erreichbarkeit.
- Datenschutzfreigabe, Löschkonzept, Protokoll-Aufbewahrung, Backup-Verschlüsselung, Berechtigungskonzept für serverseitige Zugriffe und geeignete technische Schutzmaßnahmen sind vor Echtbetrieb von Schule/Schul-IT zu prüfen.

## 8. Technik, Betrieb und Datenmodell

- **Java-basierte Webanwendung** mit PostgreSQL in eigenem Datenbankcontainer; Bereitstellung per **Docker Compose** auf Linux-Schulserver.
- Logische Komponenten: Browser-Frontend, Java-Backend/API, PostgreSQL, Dokumentenrenderer/PDF-/DOCX-Export, Dateiablage für Zertifikate und Unterschriftsgrafik, Backup-/Restore-Prozess.
- Mindest-Datenobjekte: `User`, `Student`, `EnrollmentEpisode`, `ClassAssignment`, `TrackAssignment`, `SchoolYear`, `CoinBlock`, `Event`, `EventEquivalence`, `CoinLedgerEntry`, `Internship`, `ExternalRecognition`, `ThresholdRuleVersion`, `Certificate`, `CertificateVersion`, `AuditEvent`, `EditLock`, `Draft`, `BackupJob`.
- Geldähnliche Münzberechnung als **exakte Dezimalarithmetik**, keine binären Fließkommawerte; durchgehende Transaktionsgrenzen, einzigartige IDs, referenzielle Integrität und eindeutige Geschäftsregeln.
- **Täglich automatische Sicherungen**, Aufbewahrung 30 Tage; gesondertes Backup **vor Schuljahreswechsel** und vor jedem Softwareupdate; dokumentierte vollständige Wiederherstellung inkl. Dokumentenablage/Schlüsselmaterial, regelmäßige Restore-Tests. Sicherungen vor unberechtigtem Zugriff schützen.
- Updates nur kontrolliert durch Administration/Schul-IT mit Backup, Schema-Migration, Wiederherstellungsplan.
- Einrichtungsassistent: Administrationskonto, 2026/2027, Grenzwerte, Schulkopf, Unterschrift, erster Schülerimport.
- Pilotbetrieb mit **kleiner Gruppe echter Schülerdaten** im geschützten Schulnetz. Nach Bereinigung von Testeinträgen und erfolgreicher Prüfung wird dieselbe Pilotdatenbank produktiv weiterverwendet; sicherstellen, dass keine Testbuchung die Zertifikate verfälscht.
- Finale Freigabe **gemeinsam durch BSO-Koordination und Schul-IT**.

## 9. Nichtfunktionale Anforderungen und Abnahmekriterien

| ID | Abnahmetest | Erwartetes Ergebnis |
|---|---|---|
| T01 | Neuer Schüler 2026/27 | Block 7–10 bzw. ab 11 getrennt bei 0 |
| T02 | Übergang Klasse 10 → 11 | Oberstufenstand 0; Sek-I-Historie lesbar, nicht addiert |
| T03 | Klasse 9 BR 127,5 / 128 / 150,5 / 151 / 170,5 / 171 | korrekte lückenlose Stufenklassifikation |
| T04 | Klasse 10 S1 39,5 / 40 / 69,5 / 70 / 96,5 / 97 | korrekte Stufen |
| T05 | Stufe 12 131,5 / 132 / 159,5 / 160 / 182,5 / 183 | korrekte Stufen |
| T06 | Kein gültiger Grenzwert | Münzberechnung zulässig, Zertifikat blockiert |
| T07 | Eventwert später geändert | alte Buchung unverändert, neue Buchung mit neuem Wert |
| T08 | Wiederholte Wahlpflicht-Gruppe im selben Schuljahr | zweite gleichartige Buchung blockiert |
| T09 | Drei verschiedene Netzwerkpartner-Workshops | 6 Münzen, Pflichtminimum von 2 erfüllt |
| T10 | Praktikum Pflicht 8 Tage, danach Nachtrag 2 Tage | 80 Münzen; kein zusätzlicher Nachtrag, außer protokollierter Fehlerkorrektur |
| T11 | Zwei freiwillige Praktika im selben Schuljahr | zweite Anrechnung blockiert |
| T12 | Klassenwiederholung bestätigt | Buchungen des ersten Klassen-Durchgangs ausgeschlossen, alte Zertifikate ungültig, Historie erhalten |
| T13 | Wiederholung versehentlich rückgängig gemacht | wirksame Buchungen und konsistente Status wiederhergestellt, Audit vorhanden |
| T14 | Klassen-/Bildungsgangwechsel mitten im Jahr | Datumshistorie und Regelzuordnung richtig |
| T15 | Zwei Lehrkräfte bearbeiten denselben Datensatz | zweite gesperrt; kein verlorenes Update |
| T16 | Browserunterbrechung bei nicht verbuchter Liste | Entwurf rekonstruierbar, keine Doppelbuchung |
| T17 | Nachtrag für früheren Zertifikatsstichtag | Zertifikat „Prüfung erforderlich“, keine automatische Neuversion |
| T18 | 28er-Klasse mit 3 Datenfehlern | Lehrkraft wählt 25 fehlerfreie oder Abbruch; vollständiger Prüfbericht |
| T19 | DOCX/PDF Sammelausgabe | gleicher Inhalt, Sortierung, höchstens 2 A4-Seiten je Zertifikat |
| T20 | Backup/Restore auf leerer Testinstanz | Datensätze, Historie, Versionen, Signaturablage und Dokumente vollständig wiederhergestellt |
| T21 | Schülerimport mit gleicher ID, geänderten Namen | Differenzvorschau vor Bestätigung; keine Dublette |
| T22 | Schüler fehlt im Folgeimport | Jahreswechsel bis zur Einzelklärung gesperrt |
| T23 | Rückkehr nach Ausscheiden | neue Episode/0 Münzen, frühere Episode historisch getrennt |
| T24 | Ungültige Münzwerte / 20,5 | nicht-halber Wert abgewiesen; 20,5 mit bestätigter Warnung |
| T25 | Abgesagte Veranstaltung mit Altbuchung | neue Buchung gesperrt; bestehende zur Prüfung markiert |

**Mindestfreigabekriterien:** Alle kritischen T01–T25 bestanden, keine offenen datenschutzrechtlichen oder sicherheitstechnischen Blocker, Wiederherstellung nachgewiesen, beide Freigaben dokumentiert.

## 10. Ausgangsmaterial und Migrationsentscheidung

Bereitgestellt: `BSO-KOnzeptTexzt.docx` (fachliche Ziele, Veranstaltungen, ursprüngliche Grenzwerte); `ZertifikatVorlage.docx` (Textgliederung); `BK_Kania.docx` (offizieller Briefkopf); `bso_paket_ai-generated.zip` mit `BSO_Vorlage.ods`, `BSO_Makro.bas`, `BSO_Anleitung.txt` und `BSO_DEMO_Zertifikate.odt`.

Die alte Calc-/Basic-Variante ist **keine produktionsreife Zertifikatssoftware**: Die Basis-Makros zur Ausgabe, Aktualisierung und zum Jahreswechsel sind Platzhalter; die fachliche Prüfung ist unvollständig. Die Daten- und Veranstaltungsstruktur darf als **Referenz** dienen, wird aber in einem relationalen Datenmodell neu umgesetzt. Eine Migration von früheren Münzständen ist wegen des festgelegten Neustarts **nicht vorgesehen**.

## 11. Offene Punkte / Freigabevorbehalte

1. **Rechtlich:** Die genannte dreijährige Löschfrist und das öffentliche Auftreten der Unterschrift müssen mit Schulverwaltung/Datenschutzbeauftragten abgeglichen werden; die Regel ist keine Rechtsauskunft.
2. **Organisatorisch:** Identität und legitime Verwahrung der Unterschriftsgrafik, Freigabe des offiziellen Schulkopfs und datenschutzkonforme Verarbeitung realer Pilotdaten.
3. **Technisch:** Linux-Distribution/Version, Docker-Berechtigung, Port, feste IP und TLS-Vertrauensstellung, interner Speicher/Backupziel, Nutzer-/Lastprofil.
4. **Fachlich:** Gültige Veranstaltungskatalogeinträge je Jahrgang für 2026/2027, Abgrenzung Werkstatt-/Netzwerkpartner-Angebote, Umgang mit Änderungen eines Pflichtpraktikums nach endgültiger Buchung (nur Korrektur), interne Regeländerungen nach bereits erfolgten Buchungen.
5. **Konsistenz:** In Konzepttabellen ergeben addierte Einzelwerte teils 0,5 Münzen mehr als angegebene Jahressummen. Die hier ausdrücklich bestätigten Zertifikatsgrenzen haben Vorrang; Eventkatalogwerte werden separat validiert.
6. **Druck:** Ob Sammel-PDF bei Duplexdruck Leerseiten zum rechtsseitigen Zertifikatsbeginn einfügen soll, ist bei Abnahme anhand des Druckprozesses festzulegen.
7. **Sicherheit:** Keine automatische Kontosperre und gleiche Fachrechte für alle Lehrkräfte sind gewünschte Anforderungen mit erhöhtem Risiko; vor Echtbetrieb ist eine dokumentierte Sicherheits-/Datenschutzprüfung erforderlich.

## 12. Abnahme und Change-Control

Jede Änderung nach dieser Version wird als Anforderung mit ID, Anlass, Verantwortlichem, Datum, Auswirkungen auf Daten/Zertifikate und Testfall geführt. BSO-Koordination prüft fachlich und gestalterisch, Schul-IT prüft Installation, Betrieb, Backups, IT-Sicherheit und Datenzugriffe. **Produktivfreigabe nur gemeinsam.**

**Hinweis:** Dieses Pflichtenheft ist die fachlich-technische Spezifikation. Erstellung des Dokuments und Ablage auf GitHub bedeuten **nicht**, dass die Anwendung bereits entwickelt, installiert, getestet oder freigegeben wurde.

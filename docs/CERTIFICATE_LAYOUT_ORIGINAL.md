# Zertifikats-Layout: verbindlicher Nachbesserungsauftrag

Stand 03.11.2026. Fachabnahme ausstehend. Es gibt hochgeladene Referenzen: `BK_Kania.docx` (offizieller Briefkopf, enthält auch eine **private Unterschrift**, deshalb keinesfalls vollständig in das öffentliche Repository committen), `ZertifikatVorlage.docx` und ein PDF/DOCX-Testzertifikat mit fiktiven Daten.

**Bereits vorgenommen:** `CertificateService` formatiert Stichtag, Veranstaltungsdaten, Praktikumszeiträume und das Ausstellungsdatum als vollständig ausgeschriebenes deutsches Datum (`3. November 2026`). Der Fuß lautet ausschließlich **`Landstuhl, den 3. November 2026`**, **ohne „Ort/Datum“**. Die Änderung ist im Branch committet; Build und Sichtprüfung stehen aus.

## Noch umzusetzen

1. **Original-Schulbriefkopf verwenden:** aus `BK_Kania.docx` **nur die vorhandene Header-Tabelle und das offizielle Schullogo** übernehmen. Nicht die in diesem Brief enthaltene Unterschrift oder personenspezifischen Brieftext. Original in einer privaten Ablage verarbeiten; für das Repo ggf. nur den bereinigten Header als freigegebene Vorlage nach ausdrücklicher Schulfreigabe einchecken. Der Briefkopf enthält links das IGS-Am-Nanstein-Logo und rechts die amtliche Bezeichnung „Integrierte Gesamtschule Am Nanstein“, Anschrift Konrad-Adenauer-Straße 10 / 66849 Landstuhl, Telefon 06371-403530, Telefax 06371-4035317, sekretariat@igs-landstuhl.de, www.igs-landstuhl.de. Der derzeitige frei erfundene Vollbreiten-Banner ist **nicht** der offizielle Kopf.
2. **PDF und DOCX müssen pixel-/umbruchnah identisch sein.** Die aktuelle doppelte Implementierung mit unabhängigen PDFBox- und Apache-POI-Zeichnern erfüllt das **nicht**. Eine führende DOCX-Vorlage aufbauen, die die bevorzugte visuelle Sprache des aktuellen PDF übernimmt (dunkelblaue Tabellenköpfe, klare Abschnittsüberschriften, gute Weißräume). Das PDF aus dem **gleichen final gerenderten DOCX** über eine kontrollierte, isolierte Konvertierung erzeugen (z. B. LibreOffice headless als streng beschränkter lokaler Konverter mit Timeout, eigener Temp-Ablage, kein Netzwerk, keine Shell-Interpolation, Rechte des unprivilegierten Dienstkontos). Alternativ eine nachweislich layoutgleiche Single-Source-Rendering-Lösung nutzen. Security/Containergrößen dokumentieren.
3. Verbindliche Reihenfolge: originaler Schulkopf, Titel BSO-ZERTIFIKAT, Slogan „Orientierung – Entscheidung – Bewerbung“, Name/Klasse/Jahrgang/Bildungsgang, Stichtag/Schuljahr, Zertifikatsstufe, Vorjahres-/Jahres-/Blockmünzen, Veranstaltungen des Schuljahres, Praktika, „Landstuhl, den …“, Unterschriftslinie/Funktionsbezeichnung. Keine echte Signaturgrafik einchecken.
4. **Deutsch ausgeschriebene Monatsnamen überall**, auch Datumsspalten und Praktikumszeiträume. Keine ISO-Datumswerte in Druckdokumenten. Keine Beschriftung „Ort/Datum“.
5. PDF und DOCX mit mehreren fiktiven Testfällen erzeugen, DOCX mit LibreOffice rendern, gleiche Seitenzahl sowie ein visuelles und strukturelles Layoutvergleichsprotokoll erstellen; keine abgeschnittenen Tabellen, maximal zwei A4-Seiten; bei zu viel Inhalt verständliche Fehler.
6. Versionierung erhalten. Beim neuen Layout müssen vorhandene archivierte Zertifikate unverändert bleiben; neue Ausgabe über explizite neue Version.

## Hinweis zur Bereitstellung der Originalvorlagen

Die auf der ChatGPT-Seite vorhandene bereinigte Vorlage `BSO_Originalbriefkopf_bereinigt.docx` bewahrt den originalen DOCX-Header mit Logo und entfernt den privaten Briefinhalt, die Unterschrift **und die eingebetteten Signatur-Bildbytes**. Sie muss vom Betreiber **privat in den Codex-Arbeitsbereich** kopiert werden, falls Codex nicht auf den ursprünglichen Upload zugreifen kann. Kein Geheimnis oder Signatur in GitHub hochladen. Prüfen, ob der originale Schulbriefkopf selbst zur Veröffentlichung freigegeben ist.

**Abnahmeziel:** Beim Ausdruck derselben zertifizierten Person dürfen DOCX und PDF weder bei Kopf, Farben, Tabellen, Zeilenumbruch noch beim Fuß unterschiedlich aussehen.

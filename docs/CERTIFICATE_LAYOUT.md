# Zertifikatslayout RC1

Die verbindliche Layoutbeschreibung `docs/CERTIFICATE_LAYOUT_ORIGINAL.md` ist Bestandteil des Branches. Die private Datei `BSO_Originalbriefkopf_bereinigt.docx` (ebenso `BK_Kania.docx` und `ZertifikatVorlage.docx`) war in der isolierten Arbeitskopie jedoch nicht vorhanden. Deshalb wurden keine unbekannten Logos, Briefkopfdateien oder Signaturgrafiken erfunden.

Die Ausgabe verwendet eine zentrale DOCX-Layoutquelle. Sie wird über `BSO_CERTIFICATE_TEMPLATE` beziehungsweise `bso.certificate.template-path` aus einem privaten, signaturbereinigten Template geladen. Die PDF wird im Container ausschließlich aus dem erzeugten DOCX mit LibreOffice headless erzeugt; damit gibt es keine zweite PDF-Layoutdefinition. Ohne private Vorlage bleibt der sichere, textbasierte Fallback aktiv:

- dunkelblauer offizieller Schulkopf mit vollständiger Anschrift und Kontaktdaten;
- goldene Akzentfarbe, Arial/Helvetica-Schriftfamilie und klare Abschnittsüberschriften;
- identische Stammdaten-/Münz- und Veranstaltungstabellen in PDF und DOCX;
- getrennte Abschnitte für Veranstaltungen und Praktika;
- Vorjahresmünzen, Jahresmünzen und Gesamtmünzen des aktuellen Blocks;
- Stichtag, Schuljahr, Zertifikatsstufe, Slogan und Unterschriftszeile;
- PDF-Ausgabe bricht bei Überschreitung des Satzspiegels mit einem Layoutfehler ab, statt Inhalte abzuschneiden.
- LibreOffice läuft mit eigenem temporärem Benutzerprofil, 30-Sekunden-Timeout und temporärem Arbeitsverzeichnis; die Anwendung benötigt keinen Netzwerkzugriff für die Konvertierung.
- Sämtliche sichtbaren Datumsangaben werden mit ausgeschriebenen deutschen Monatsnamen formatiert; die Schlusszeile lautet dynamisch `Landstuhl, den <Tag>. <Monat> <Jahr>`.
- Die frühere Bezeichnung `Ort/Datum` wird nicht mehr ausgegeben.

## Qualitätssicherung

Mit fiktiven Daten wurden zwei Varianten erzeugt: Anna Beispiel mit Workshop und Pflichtpraktikum sowie Ben Beispiel mit Bewerbungstraining. Beide PDF-/DOCX-Paare wurden erzeugt. PDFBox bestätigte jeweils eine Seite, das dynamische deutsche Datum und das Fehlen von `Ort/Datum`; Apache POI bestätigte die DOCX-Inhalte. Die gerenderten PDF-Seiten wurden visuell verglichen: identische Tabellenstruktur, Seitenumbruch und Satzspiegel, keine abgeschnittenen Texte und sichtbare Unterschriftszeile.

Die echte Briefkopfvorlage und eine freigegebene Unterschriftsgrafik müssen vor der Produktivfreigabe durch BSO-Koordination/Schul-IT bereitgestellt und separat abgenommen werden. Sie gehören nicht in Git.

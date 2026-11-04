# Zertifikatslayout RC1

Die verbindliche Layoutbeschreibung `docs/CERTIFICATE_LAYOUT_ORIGINAL.md` ist Bestandteil des Branches. Die private, signaturbereinigte Datei `BSO_Originalbriefkopf_bereinigt.docx` wird außerhalb Git unter `/opt/bso-private` verwahrt und im Compose-Dienst nur read-only nach `/run/bso-private/` eingebunden. Die Vorlage und die daraus erzeugten Dokumente werden nicht in Git aufgenommen.

Die Ausgabe verwendet eine zentrale DOCX-Layoutquelle. Sie wird über `BSO_CERTIFICATE_TEMPLATE` beziehungsweise `bso.certificate.template-path` aus dem privaten, signaturbereinigten Template geladen. Sobald die Vorlage aktiv ist, wird kein zusätzlicher Ersatzbriefkopf erzeugt; Header, Logo, Margins und Seitenlayout stammen aus der Original-DOCX. Die PDF wird im Container ausschließlich aus dem erzeugten DOCX mit LibreOffice headless erzeugt; damit gibt es keine zweite PDF-Layoutdefinition. Ohne private Vorlage bleibt der sichere, textbasierte Fallback aktiv:

`compose.yaml` bindet standardmäßig `/opt/bso-private/BSO_Originalbriefkopf_bereinigt.docx` als read-only Volume ein und setzt im Container `BSO_CERTIFICATE_TEMPLATE=/run/bso-private/BSO_Originalbriefkopf_bereinigt.docx`. Für einen anderen privaten Ablageort wird ausschließlich `BSO_CERTIFICATE_TEMPLATE_HOST` außerhalb von Git gesetzt.

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

Mit fiktiven Daten wurde bei aktivierter Originalvorlage Clara Vorlage, Jahrgang 9 BR, mit einem Zukunftstag erzeugt. Das PDF hat eine A4-Seite; die DOCX enthält denselben Original-Header, das IGS-Logo, die Tabellen und die Schlusszeile. Die gerenderte PDF-Seite wurde visuell geprüft; der PDF-Render stammt direkt aus derselben DOCX und hat daher identische Tabellen, Seitenumbrüche und Margins. Zusätzlich wurden die DOCX-Struktur, Headertext, eingebettetes Logo, deutsches Datum und das Fehlen von `Ort/Datum` geprüft.

Die echte Briefkopfvorlage und eine freigegebene Unterschriftsgrafik müssen vor der Produktivfreigabe durch BSO-Koordination/Schul-IT bereitgestellt und separat abgenommen werden. Sie gehören nicht in Git.

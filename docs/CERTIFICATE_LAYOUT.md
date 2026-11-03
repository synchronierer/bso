# Zertifikatslayout RC1

Die im Branch genannten Dateien `BK_Kania.docx` und `ZertifikatVorlage.docx` waren im Arbeitsbereich nicht vorhanden und konnten daher nicht unverändert eingebettet werden. Es wurden keine unbekannten Logos, Briefkopfdateien oder Signaturgrafiken erfunden.

Die Ausgabe verwendet deshalb eine zentrale Layoutdefinition in `CertificateService`:

- dunkelblauer offizieller Schulkopf mit vollständiger Anschrift und Kontaktdaten;
- goldene Akzentfarbe, Arial/Helvetica-Schriftfamilie und klare Abschnittsüberschriften;
- identische Stammdaten-/Münz- und Veranstaltungstabellen in PDF und DOCX;
- getrennte Abschnitte für Veranstaltungen und Praktika;
- Vorjahresmünzen, Jahresmünzen und Gesamtmünzen des aktuellen Blocks;
- Stichtag, Schuljahr, Zertifikatsstufe, Slogan und Unterschriftszeile;
- PDF-Ausgabe bricht bei Überschreitung des Satzspiegels mit einem Layoutfehler ab, statt Inhalte abzuschneiden.

## Qualitätssicherung

Mit fiktiven Daten wurden Varianten mit Anna Beispiel, Jahrgang 9 BR, Berufsorientierungs-Workshop sowie einem Pflichtpraktikum bei Fiktiv GmbH erzeugt. PDFBox bestätigte eine Seite und die Pflichttexte; Apache POI bestätigte Schulkopf, Tabellen und Zertifikatsinhalte im DOCX. Die gerenderte PDF-Seite wurde visuell geprüft: keine abgeschnittenen Tabellen oder Texte, klare Hierarchie und sichtbare Unterschriftszeile.

Die echte Briefkopfvorlage und eine freigegebene Unterschriftsgrafik müssen vor der Produktivfreigabe durch BSO-Koordination/Schul-IT bereitgestellt und separat abgenommen werden. Sie gehören nicht in Git.

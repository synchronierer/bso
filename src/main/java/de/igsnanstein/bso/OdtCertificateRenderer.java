package de.igsnanstein.bso;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.zip.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;

/** Renders certificate contents into the private official ODT master,
 * preserving embedded school letterhead and approved signature artwork. */
@Component
public class OdtCertificateRenderer {
    private static final String TEXT="urn:oasis:names:tc:opendocument:xmlns:text:1.0";
    private static final String TABLE="urn:oasis:names:tc:opendocument:xmlns:table:1.0";
    private static final String OFFICE="urn:oasis:names:tc:opendocument:xmlns:office:1.0";
    private static final Locale DE=Locale.GERMANY;
    private final Path path;

    OdtCertificateRenderer(@Value("${BSO_CERTIFICATE_ODT_TEMPLATE:/run/bso-private/BeispielZertifikat1.odt}") String path) {
        this.path=Path.of(path);
    }

    byte[] render(String name,String className,String total,String level,LocalDate date,
                  List<List<String>> events,List<List<String>> internships) {
        if(!Files.isRegularFile(path))
            throw new IllegalStateException("Private ODT-Zertifikatsvorlage fehlt: "+path);
        try {
            Map<String,byte[]> entries=new LinkedHashMap<>();
            try(ZipInputStream zin=new ZipInputStream(Files.newInputStream(path))) {
                ZipEntry entry;
                while((entry=zin.getNextEntry())!=null){
                    if(entry.isDirectory())continue;
                    entries.put(entry.getName(),zin.readAllBytes());
                }
            }
            if(!entries.containsKey("content.xml")||!entries.containsKey("styles.xml"))
                throw new IllegalStateException("ODT-Vorlage ist unvollständig");
            // The supplied template embeds C. Morgenstern's signature; do not import any
            // signature into source control or load it from a public resource.
            boolean signature=entries.keySet().stream().anyMatch(n->n.startsWith("Pictures/")&&n.endsWith(".jpg"));
            if(!signature)throw new IllegalStateException("ODT-Vorlage enthält keine freigegebene Unterschrift");
            Document xml=parse(entries.get("content.xml"));
            NodeList body=xml.getElementsByTagNameNS(OFFICE,"text");
            if(body.getLength()!=1)throw new IllegalStateException("ODT-Vorlage enthält keinen eindeutigen Textbereich");
            Element root=(Element)body.item(0);
            List<Element> paragraphs=directChildren(root,TEXT,"p");
            replaceFirst(paragraphs,"Max Mustermann, 8a",name+", "+className);
            replaceFirst(paragraphs,"hat das Berufs- und Studienorientierungszertifikat",
                "hat das Berufs- und Studienorientierungszertifikat der IGS Am Nanstein mit einer Gesamtmünzzahl von "+total+" Münzen");
            replaceFirst(paragraphs,"mit Auszeichnung erworben.",level);
            replaceFirst(paragraphs,"25. Juni 2026","Landstuhl, den "+format(date));
            List<Element> tables=directChildren(root,TABLE,"table");
            if(tables.size()<2)throw new IllegalStateException("Die ODT-Vorlage benötigt zwei Tabellen");
            fillTable(xml,tables.get(0),events,4);
            fillTable(xml,tables.get(1),internships,4);
            entries.put("content.xml",serialize(xml));
            ByteArrayOutputStream result=new ByteArrayOutputStream();
            try(ZipOutputStream out=new ZipOutputStream(result)){
                byte[] mime=entries.remove("mimetype");
                if(mime==null)throw new IllegalStateException("ODT-MIME-Typ fehlt");
                ZipEntry mimeEntry=new ZipEntry("mimetype");
                mimeEntry.setMethod(ZipEntry.STORED);
                mimeEntry.setSize(mime.length);
                mimeEntry.setCompressedSize(mime.length);
                java.util.zip.CRC32 checksum=new java.util.zip.CRC32();
                checksum.update(mime);mimeEntry.setCrc(checksum.getValue());
                out.putNextEntry(mimeEntry);out.write(mime);out.closeEntry();
                for(var item:entries.entrySet()){
                    out.putNextEntry(new ZipEntry(item.getKey()));
                    out.write(item.getValue());out.closeEntry();
                }
            }
            return result.toByteArray();
        }catch(IOException ex){throw new IllegalStateException("ODT konnte nicht erstellt werden",ex);}
    }

    private static Document parse(byte[] bytes){
        try{
            DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            f.setFeature("http://xml.org/sax/features/external-general-entities",false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
            return f.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
        }catch(Exception ex){throw new IllegalStateException("Ungültiges ODT-XML",ex);}
    }
    private static byte[] serialize(Document xml){
        try{
            TransformerFactory factory=TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET,"");
            Transformer t=factory.newTransformer();
            t.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
            ByteArrayOutputStream output=new ByteArrayOutputStream();
            t.transform(new DOMSource(xml),new StreamResult(output));
            return output.toByteArray();
        }catch(Exception ex){throw new IllegalStateException("ODT konnte nicht serialisiert werden",ex);}
    }
    private static List<Element> directChildren(Element parent,String ns,String localName){
        List<Element> result=new ArrayList<>();
        for(Node child=parent.getFirstChild();child!=null;child=child.getNextSibling())
            if(child instanceof Element e&&ns.equals(e.getNamespaceURI())&&localName.equals(e.getLocalName()))
                result.add(e);
        return result;
    }
    private static void replaceFirst(List<Element> paragraphs,String original,String replacement){
        for(Element element:paragraphs)if(element.getTextContent().contains(original)){
            // Leave ODT paragraph styles (spacing, size, alignment) intact.
            element.setTextContent(replacement);
            return;
        }
        throw new IllegalStateException("Textstelle in ODT-Vorlage fehlt: "+original);
    }
    private static void fillTable(Document xml,Element table,List<List<String>> rows,int columns){
        List<Element> existing=directChildren(table,TABLE,"table-row");
        if(existing.size()<2)throw new IllegalStateException("Tabellenvorlage enthält keine Datenzeile");
        Element pattern=existing.get(1);
        for(int i=existing.size()-1;i>=1;i--)table.removeChild(existing.get(i));
        List<List<String>> data=rows.isEmpty()
            ? List.of(List.of("–","–","Keine Einträge","0")) : rows;
        for(List<String> values:data){
            if(values.size()!=columns)throw new IllegalArgumentException("Tabellenzeile hat falsche Spaltenzahl");
            Element clone=(Element)pattern.cloneNode(true);
            List<Element> cells=directChildren(clone,TABLE,"table-cell");
            if(cells.size()<columns)throw new IllegalStateException("ODT-Vorlage enthält zu wenige Tabellenspalten");
            for(int i=0;i<columns;i++){
                List<Element> ps=directChildren(cells.get(i),TEXT,"p");
                if(ps.isEmpty()){
                    Element p=xml.createElementNS(TEXT,"text:p");p.setTextContent(values.get(i));cells.get(i).appendChild(p);
                }else{
                    ps.get(0).setTextContent(values.get(i));
                    for(int j=ps.size()-1;j>0;j--)cells.get(i).removeChild(ps.get(j));
                }
            }
            table.appendChild(clone);
        }
    }
    private static String format(LocalDate date){
        return date.getDayOfMonth()+". "+date.getMonth().getDisplayName(TextStyle.FULL,DE)+" "+date.getYear();
    }
}

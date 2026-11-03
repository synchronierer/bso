package de.igsnanstein.bso;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CertificateService {
    private static final String SLOGAN="Orientierung – Entscheidung – Bewerbung";
    private static final String SCHOOL="IGS Am Nanstein · Konrad-Adenauer-Straße 10 · 66849 Landstuhl";
    private final JdbcTemplate db;
    CertificateService(JdbcTemplate db) { this.db=db; }

    public record Generated(long studentPk, String level, byte[] pdf, byte[] docx, int version) {}

    @Transactional
    public Generated issue(long studentPk, LocalDate asOf, String actor, boolean newVersion) {
        Map<String,Object> student=student(studentPk);
        int grade=((Number)student.get("grade")).intValue();
        String track=(String)student.get("track");
        BigDecimal coins=coinTotal(studentPk,asOf).add(internshipTotal(studentPk,asOf));
        CoinRules.Level level=CoinRules.levelFor(coins,grade,track);
        if(level==CoinRules.Level.NONE || level==CoinRules.Level.REVIEW_REQUIRED)
            throw new IllegalArgumentException("Für diesen Stichtag besteht kein ausstellbares Zertifikat");
        Map<String,Object> payload=new LinkedHashMap<>();
        payload.put("studentId",student.get("student_id")); payload.put("name",student.get("first_name")+" "+student.get("last_name"));
        payload.put("class",student.get("class_name")); payload.put("grade",grade); payload.put("track",track);
        payload.put("asOf",asOf.toString()); payload.put("coins",coins.toPlainString()); payload.put("level",level.name());
        int certificateId;
        Map<String,Object> existingRow=db.query("SELECT id,status FROM certificates WHERE student_pk=?",rs->{return rs.next()?Map.of("id",rs.getLong("id"),"status",rs.getString("status")):null;},studentPk);
        if(existingRow!=null && "PRUEFUNG_ERFORDERLICH".equals(existingRow.get("status")) && !newVersion)
            throw new IllegalArgumentException("Zertifikat muss nach einer Datenkorrektur neu geprüft und versioniert werden");
        Integer existing=existingRow==null?null:((Long)existingRow.get("id")).intValue();
        if(existing==null) {
            db.update("INSERT INTO certificates(student_pk,status,issued_by) VALUES (?,'AKTUELL',?)",studentPk,actor);
            certificateId=db.queryForObject("SELECT id FROM certificates WHERE student_pk=? AND status='AKTUELL'",Integer.class,studentPk);
        } else certificateId=existing;
        Integer last=db.queryForObject("SELECT COALESCE(MAX(version),0) FROM certificate_versions WHERE certificate_id=?",Integer.class,certificateId);
        if(last>0 && !newVersion) {
            Map<String,Object> old=db.queryForMap("SELECT level,pdf_data,docx_data,version FROM certificate_versions WHERE certificate_id=? AND version=?",certificateId,last);
            return new Generated(studentPk,(String)old.get("level"),(byte[])old.get("pdf_data"),(byte[])old.get("docx_data"),(Integer)old.get("version"));
        }
        byte[] pdf=pdf(student,payload,asOf,coins,level);
        byte[] docx=docx(student,payload,asOf,coins,level);
        String snapshot=toJson(payload);
        db.update("INSERT INTO certificate_versions(certificate_id,version,as_of,level,snapshot,pdf_data,docx_data) VALUES (?,?,?, ?,CAST(? AS JSONB),?,?)",
            certificateId,last+1,asOf,level.name(),snapshot,pdf,docx);
        db.update("INSERT INTO audit_events(actor,action,object_type,object_id,before_json,after_json) VALUES (?,?,?, ?,CAST('null' AS JSONB),CAST(? AS JSONB))",
            actor,"CERTIFICATE_ISSUE","certificate",Long.toString(certificateId),snapshot);
        return new Generated(studentPk,level.name(),pdf,docx,last+1);
    }

    private Map<String,Object> student(long pk) {
        List<Map<String,Object>> rows=db.queryForList("SELECT id,student_id,first_name,last_name,class_name,grade,track FROM students WHERE id=? AND active=true",pk);
        if(rows.isEmpty()) throw new IllegalArgumentException("Schüler nicht vorhanden oder archiviert");
        return rows.get(0);
    }
    private BigDecimal coinTotal(long pk,LocalDate asOf) {
        BigDecimal value=db.queryForObject("SELECT COALESCE(SUM(g.coins),0) FROM coin_grants g JOIN events e ON e.id=g.event_id WHERE g.student_pk=? AND g.excluded=false AND e.end_date<=? AND e.status<>'ABGEBROCHEN'",BigDecimal.class,pk,asOf);
        return value==null?BigDecimal.ZERO:value;
    }
    private BigDecimal internshipTotal(long pk,LocalDate asOf) {
        BigDecimal value=db.queryForObject("SELECT COALESCE(SUM(coins),0) FROM internships WHERE student_pk=? AND finalised=true AND ends_on<=?",BigDecimal.class,pk,asOf);
        return value==null?BigDecimal.ZERO:value;
    }
    private List<Map<String,Object>> events(long pk,LocalDate asOf) { return db.queryForList("SELECT e.title,e.end_date,g.coins FROM coin_grants g JOIN events e ON e.id=g.event_id WHERE g.student_pk=? AND e.end_date<=? ORDER BY e.end_date,e.title",pk,asOf); }
    private List<Map<String,Object>> internships(long pk,LocalDate asOf) { return db.queryForList("SELECT kind,company,starts_on,ends_on,recognized_days,coins FROM internships WHERE student_pk=? AND finalised=true AND ends_on<=? ORDER BY starts_on",pk,asOf); }

    private byte[] pdf(Map<String,Object> student,Map<String,Object> payload,LocalDate asOf,BigDecimal coins,CoinRules.Level level) {
        try(PDDocument document=new PDDocument(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            PDPage page=new PDPage(PDRectangle.A4); document.addPage(page);
            try(PDPageContentStream stream=new PDPageContentStream(document,page)) {
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),16); stream.beginText(); stream.newLineAtOffset(50,780); stream.showText(SCHOOL); stream.endText();
                int y=735; y=line(stream,"BSO-Zertifikat",20,y,true); y-=10; y=line(stream,SLOGAN,12,y,false); y-=20;
                y=line(stream,"Hiermit wird bescheinigt:",12,y,false); y=line(stream,"Name: "+payload.get("name"),12,y,false); y=line(stream,"Klasse/Jahrgang: "+payload.get("class")+" / "+payload.get("grade"),12,y,false);
                y=line(stream,"Bildungsgang: "+payload.get("track"),12,y,false); y-=10; y=line(stream,"Stufe: "+levelText(level),15,y,true); y=line(stream,"Münzstand zum "+asOf+": "+coins+" Münzen",12,y,false); y-=15;
                y=line(stream,"Veranstaltungen im Stichtagszeitraum:",11,y,true);
                for(Map<String,Object> event:events(((Number)student.get("id")).longValue(),asOf)) { if(y<125) throw new IllegalArgumentException("Zertifikat enthält zu viele Veranstaltungen für eine Seite"); y=line(stream,event.get("end_date")+" · "+event.get("title")+" · "+event.get("coins")+" Münzen",9,y,false); }
                y-=8; y=line(stream,"Praktika:",11,y,true);
                for(Map<String,Object> internship:internships(((Number)student.get("id")).longValue(),asOf)) { if(y<95) throw new IllegalArgumentException("Zertifikat enthält zu viele Praktika für eine Seite"); y=line(stream,internship.get("kind")+" · "+internship.get("company")+" · "+internship.get("recognized_days")+" Tage · "+internship.get("coins")+" Münzen",9,y,false); }
                line(stream,"Ort/Datum: Landstuhl, "+asOf,10,55,false); line(stream,"BSO-Koordination",10,55-18,false);
            }
            document.save(out); return out.toByteArray();
        } catch(Exception ex) { throw new IllegalStateException("PDF konnte nicht erzeugt werden",ex); }
    }
    private int line(PDPageContentStream stream,String text,float size,int y,boolean bold) throws java.io.IOException { stream.setFont(new PDType1Font(bold?Standard14Fonts.FontName.HELVETICA_BOLD:Standard14Fonts.FontName.HELVETICA),size); stream.beginText(); stream.newLineAtOffset(50,y); stream.showText(text.replace("–","-")); stream.endText(); return y-(int)(size+7); }
    private byte[] docx(Map<String,Object> student,Map<String,Object> payload,LocalDate asOf,BigDecimal coins,CoinRules.Level level) {
        try(XWPFDocument doc=new XWPFDocument(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            XWPFParagraph title=doc.createParagraph(); title.setAlignment(ParagraphAlignment.CENTER); title.createRun().setText(SCHOOL);
            XWPFParagraph heading=doc.createParagraph(); heading.setAlignment(ParagraphAlignment.CENTER); XWPFRun run=heading.createRun(); run.setBold(true); run.setFontSize(18); run.setText("BSO-Zertifikat");
            doc.createParagraph().createRun().setText(SLOGAN);
            for(String text:List.of("Hiermit wird bescheinigt:","Name: "+payload.get("name"),"Klasse/Jahrgang: "+payload.get("class")+" / "+payload.get("grade"),"Bildungsgang: "+payload.get("track"),"Stufe: "+levelText(level),"Münzstand zum "+asOf+": "+coins+" Münzen","Veranstaltungen:")) doc.createParagraph().createRun().setText(text);
            for(Map<String,Object> event:events(((Number)student.get("id")).longValue(),asOf)) doc.createParagraph().createRun().setText(event.get("end_date")+" · "+event.get("title")+" · "+event.get("coins")+" Münzen");
            doc.createParagraph().createRun().setText("Praktika:");
            for(Map<String,Object> internship:internships(((Number)student.get("id")).longValue(),asOf)) doc.createParagraph().createRun().setText(internship.get("kind")+" · "+internship.get("company")+" · "+internship.get("recognized_days")+" Tage · "+internship.get("coins")+" Münzen");
            doc.createParagraph().createRun().setText("Ort/Datum: Landstuhl, "+asOf+" · BSO-Koordination"); doc.write(out); return out.toByteArray();
        } catch(Exception ex) { throw new IllegalStateException("DOCX konnte nicht erzeugt werden",ex); }
    }
    private String levelText(CoinRules.Level level) { return switch(level) { case SUCCESS->"Mit Erfolg erworben"; case SPECIAL_SUCCESS->"Mit besonderem Erfolg erworben"; case DISTINCTION->"Mit Auszeichnung erworben"; default->level.name(); }; }
    private String toJson(Map<String,Object> values) { StringBuilder json=new StringBuilder("{"); boolean first=true; for(var entry:values.entrySet()){if(!first)json.append(',');first=false;json.append('"').append(entry.getKey()).append("\":\"").append(String.valueOf(entry.getValue()).replace("\"","\\\"")).append('"');} return json.append('}').toString(); }
}

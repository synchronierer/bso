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
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CertificateService {
    private static final String SLOGAN="Orientierung – Entscheidung – Bewerbung";
    private static final String SCHOOL="IGS Am Nanstein";
    private static final String ADDRESS="Konrad-Adenauer-Straße 10 · 66849 Landstuhl";
    private static final String CONTACT="Telefon 06371-403530 · Fax 06371-4035317 · sekretariat@igs-landstuhl.de · www.igs-landstuhl.de";
    private static final String FONT="Arial";
    private static final PDColor NAVY=new PDColor(new float[]{0.07f,0.19f,0.34f},PDDeviceRGB.INSTANCE);
    private static final PDColor GOLD=new PDColor(new float[]{0.78f,0.57f,0.18f},PDDeviceRGB.INSTANCE);
    private static final PDColor BLACK=new PDColor(new float[]{0f,0f,0f},PDDeviceRGB.INSTANCE);
    private static final PDColor WHITE=new PDColor(new float[]{1f,1f,1f},PDDeviceRGB.INSTANCE);
    private final JdbcTemplate db;
    CertificateService(JdbcTemplate db){this.db=db;}
    public record Generated(long studentPk,String level,byte[] pdf,byte[] docx,int version){}
    private record EventLine(String date,String title,BigDecimal coins){}
    private record PracticeLine(String kind,String company,String period,int days,BigDecimal coins){}
    private record Breakdown(String schoolYear,BigDecimal previous,BigDecimal annual,BigDecimal total,List<EventLine> events,List<PracticeLine> practices){}

    @Transactional
    public Generated issue(long studentPk,LocalDate asOf,String actor,boolean newVersion){
        Map<String,Object> student=student(studentPk);int grade=((Number)student.get("grade")).intValue();
        Breakdown b=breakdown(studentPk,asOf,grade);CoinRules.Level level=CoinRules.levelFor(b.total(),grade,(String)student.get("track"));
        if(level==CoinRules.Level.NONE||level==CoinRules.Level.REVIEW_REQUIRED)throw new IllegalArgumentException("Für diesen Stichtag besteht kein ausstellbares Zertifikat");
        Map<String,Object> payload=new LinkedHashMap<>();payload.put("studentId",student.get("student_id"));payload.put("name",student.get("first_name")+" "+student.get("last_name"));payload.put("class",student.get("class_name"));payload.put("grade",grade);payload.put("track",student.get("track"));payload.put("asOf",asOf);payload.put("schoolYear",b.schoolYear());payload.put("previous",b.previous());payload.put("annual",b.annual());payload.put("total",b.total());payload.put("level",level.name());
        Map<String,Object> existing=db.query("SELECT id,status FROM certificates WHERE student_pk=?",rs->rs.next()?Map.of("id",rs.getLong(1),"status",rs.getString(2)):null,studentPk);
        if(existing!=null&&"PRUEFUNG_ERFORDERLICH".equals(existing.get("status"))&&!newVersion)throw new IllegalArgumentException("Zertifikat muss nach einer Korrektur neu geprüft und versioniert werden");
        int certificateId;if(existing==null){db.update("INSERT INTO certificates(student_pk,status,issued_by) VALUES (?,'AKTUELL',?)",studentPk,actor);certificateId=db.queryForObject("SELECT id FROM certificates WHERE student_pk=?",Integer.class,studentPk);}else certificateId=((Long)existing.get("id")).intValue();
        Integer last=db.queryForObject("SELECT COALESCE(MAX(version),0) FROM certificate_versions WHERE certificate_id=?",Integer.class,certificateId);
        if(last>0&&!newVersion){Map<String,Object> old=db.queryForMap("SELECT level,pdf_data,docx_data,version FROM certificate_versions WHERE certificate_id=? AND version=?",certificateId,last);return new Generated(studentPk,(String)old.get("level"),(byte[])old.get("pdf_data"),(byte[])old.get("docx_data"),(Integer)old.get("version"));}
        byte[] pdf=pdf(student,payload,b,level),docx=docx(student,payload,b,level);String snapshot=toJson(payload);
        db.update("INSERT INTO certificate_versions(certificate_id,version,as_of,level,snapshot,pdf_data,docx_data) VALUES (?,?,?, ?,CAST(? AS JSONB),?,?)",certificateId,last+1,asOf,level.name(),snapshot,pdf,docx);
        db.update("INSERT INTO audit_events(actor,action,object_type,object_id,before_json,after_json) VALUES (?,?,?, ?,CAST('null' AS JSONB),CAST(? AS JSONB))",actor,"CERTIFICATE_ISSUE","certificate",Long.toString(certificateId),snapshot);
        return new Generated(studentPk,level.name(),pdf,docx,last+1);
    }

    private Map<String,Object> student(long pk){List<Map<String,Object>> rows=db.queryForList("SELECT id,student_id,first_name,last_name,class_name,grade,track FROM students WHERE id=? AND active=true",pk);if(rows.isEmpty())throw new IllegalArgumentException("Schüler nicht vorhanden oder archiviert");return rows.get(0);}
    private Breakdown breakdown(long pk,LocalDate asOf,int grade){
        Map<String,Object> year=db.queryForMap("SELECT name,starts_on,ends_on FROM school_years WHERE starts_on<=? AND ends_on>=? ORDER BY starts_on DESC LIMIT 1",asOf,asOf);LocalDate start=((java.sql.Date)year.get("starts_on")).toLocalDate();int blockStart=grade>=11?11:7;
        String ep="(SELECT ep.id FROM enrollment_episodes ep WHERE ep.student_pk=? AND ep.starts_on<=? AND (ep.ends_on IS NULL OR ep.ends_on>=?) ORDER BY ep.starts_on DESC,ep.id DESC LIMIT 1)";
        BigDecimal previous=money("SELECT COALESCE(SUM(g.coins),0) FROM coin_grants g JOIN events e ON e.id=g.event_id WHERE g.episode_id="+ep+" AND g.excluded=false AND g.student_grade BETWEEN ? AND ? AND e.end_date<? AND e.status<>'ABGEBROCHEN'",pk,asOf,asOf,blockStart,grade-1,start);
        previous=previous.add(money("SELECT COALESCE(SUM(i.coins),0) FROM internships i WHERE i.episode_id="+ep+" AND i.finalised=true AND i.ends_on<?",pk,asOf,asOf,start));
        BigDecimal annual=money("SELECT COALESCE(SUM(g.coins),0) FROM coin_grants g JOIN events e ON e.id=g.event_id WHERE g.episode_id="+ep+" AND g.excluded=false AND e.end_date>=? AND e.end_date<=? AND e.status<>'ABGEBROCHEN'",pk,asOf,asOf,start,asOf);
        annual=annual.add(money("SELECT COALESCE(SUM(i.coins),0) FROM internships i WHERE i.episode_id="+ep+" AND i.finalised=true AND i.ends_on>=? AND i.ends_on<=?",pk,asOf,asOf,start,asOf));
        List<EventLine> events=db.query("SELECT e.end_date,e.title,g.coins FROM coin_grants g JOIN events e ON e.id=g.event_id WHERE g.episode_id="+ep+" AND g.excluded=false AND e.end_date>=? AND e.end_date<=? AND e.status<>'ABGEBROCHEN' ORDER BY e.end_date,e.title",rs->{List<EventLine> r=new ArrayList<>();while(rs.next())r.add(new EventLine(rs.getDate(1).toLocalDate().toString(),rs.getString(2),rs.getBigDecimal(3)));return r;},pk,asOf,asOf,start,asOf);
        List<PracticeLine> practices=db.query("SELECT kind,company,starts_on,ends_on,recognized_days,coins FROM internships WHERE episode_id="+ep+" AND finalised=true AND ends_on>=? AND ends_on<=? ORDER BY starts_on",rs->{List<PracticeLine> r=new ArrayList<>();while(rs.next())r.add(new PracticeLine(rs.getString(1),rs.getString(2),rs.getDate(3).toLocalDate()+" – "+rs.getDate(4).toLocalDate(),rs.getInt(5),rs.getBigDecimal(6)));return r;},pk,asOf,asOf,start,asOf);
        return new Breakdown((String)year.get("name"),previous,annual,previous.add(annual),events,practices);
    }
    private BigDecimal money(String sql,Object... args){BigDecimal v=db.queryForObject(sql,BigDecimal.class,args);return v==null?BigDecimal.ZERO:v;}

    private byte[] pdf(Map<String,Object> student,Map<String,Object> p,Breakdown b,CoinRules.Level level){try(PDDocument doc=new PDDocument();ByteArrayOutputStream out=new ByteArrayOutputStream()){PDPage page=new PDPage(PDRectangle.A4);doc.addPage(page);try(PDPageContentStream s=new PDPageContentStream(doc,page)){header(s);int y=690;y=text(s,"BSO-ZERTIFIKAT",20,50,y,NAVY,true)-12;y=text(s,SLOGAN,11,50,y,GOLD,false)-18;y=text(s,"Hiermit wird bescheinigt",11,50,y,NAVY,true)-8;y=summary(s,p,b,level,y);y-=15;y=text(s,"VERANSTALTUNGEN IM SCHULJAHR "+b.schoolYear(),10,50,y,NAVY,true)-5;y=eventTable(s,b.events(),y);y-=12;y=text(s,"PRAKTIKA",10,50,y,NAVY,true)-5;y=practiceTable(s,b.practices(),y);if(y<100)throw new IllegalArgumentException("Zertifikat überschreitet den maximalen Seitenumfang");text(s,"Ort/Datum: Landstuhl, "+p.get("asOf"),9,50,72,BLACK,false);text(s,"Unterschrift BSO-Koordination",9,385,72,BLACK,false);s.setStrokingColor(GOLD);s.moveTo(385,88);s.lineTo(545,88);s.stroke();}doc.save(out);return out.toByteArray();}catch(Exception ex){if(ex instanceof IllegalArgumentException)throw (IllegalArgumentException)ex;throw new IllegalStateException("PDF konnte nicht erzeugt werden",ex);}}
    private void header(PDPageContentStream s)throws java.io.IOException{s.setNonStrokingColor(NAVY);s.addRect(0,742,595,100);s.fill();text(s,SCHOOL,20,50,800,WHITE,true);text(s,ADDRESS,9,50,780,WHITE,false);text(s,CONTACT,7,50,762,WHITE,false);}
    private int summary(PDPageContentStream s,Map<String,Object> p,Breakdown b,CoinRules.Level level,int y)throws java.io.IOException{String[][] rows={{"Name",String.valueOf(p.get("name")),"Klasse",String.valueOf(p.get("class"))},{"Jahrgang",String.valueOf(p.get("grade")),"Bildungsgang",String.valueOf(p.get("track"))},{"Stichtag",String.valueOf(p.get("asOf")),"Schuljahr",b.schoolYear()},{"Zertifikatsstufe",levelText(level),"", ""},{"Vorjahresmünzen",b.previous().toPlainString(),"Jahresmünzen",b.annual().toPlainString()},{"Gesamtmünzen",b.total().toPlainString(),"", ""}};return grid(s,rows,50,y,495,22);}
    private int eventTable(PDPageContentStream s,List<EventLine> rows,int y)throws java.io.IOException{String[][] x=new String[rows.size()+1][3];x[0]=new String[]{"Datum","Veranstaltung","Münzen"};for(int i=0;i<rows.size();i++)x[i+1]=new String[]{rows.get(i).date(),rows.get(i).title(),rows.get(i).coins().toPlainString()};return grid(s,x,50,y,495,18);}
    private int practiceTable(PDPageContentStream s,List<PracticeLine> rows,int y)throws java.io.IOException{String[][] x=new String[rows.size()+1][4];x[0]=new String[]{"Art / Betrieb","Zeitraum","Tage","Münzen"};for(int i=0;i<rows.size();i++)x[i+1]=new String[]{rows.get(i).kind()+" / "+rows.get(i).company(),rows.get(i).period(),Integer.toString(rows.get(i).days()),rows.get(i).coins().toPlainString()};return grid(s,x,50,y,495,18);}
    private int grid(PDPageContentStream s,String[][] rows,int x,int y,int width,int rowH)throws java.io.IOException{int cols=rows[0].length;int[] w=cols==4?new int[]{210,160,55,70}:cols==3?new int[]{80,345,70}:new int[]{125,125,125,120};for(int r=0;r<rows.length;r++){int xx=x;for(int c=0;c<cols;c++){s.setNonStrokingColor(r==0?NAVY:(r%2==0?new PDColor(new float[]{.96f,.97f,.98f},PDDeviceRGB.INSTANCE):WHITE));s.addRect(xx,y-rowH,w[c],rowH);s.fill();s.setStrokingColor(new PDColor(new float[]{.78f,.81f,.85f},PDDeviceRGB.INSTANCE));s.addRect(xx,y-rowH,w[c],rowH);s.stroke();text(s,rows[r][c],8,xx+6,y-13,r==0?WHITE:BLACK,r==0);xx+=w[c];}y-=rowH;}return y;}
    private int text(PDPageContentStream s,String value,float size,int x,int y,PDColor color,boolean bold)throws java.io.IOException{s.setNonStrokingColor(color);s.setFont(new PDType1Font(bold?Standard14Fonts.FontName.HELVETICA_BOLD:Standard14Fonts.FontName.HELVETICA),size);s.beginText();s.newLineAtOffset(x,y);s.showText(safe(value));s.endText();return y-(int)(size+5);}
    private String safe(String s){return s.replace("–","-").replace("·","|").replace("—","-");}

    private byte[] docx(Map<String,Object> student,Map<String,Object> p,Breakdown b,CoinRules.Level level){try(XWPFDocument doc=new XWPFDocument();ByteArrayOutputStream out=new ByteArrayOutputStream()){setMargins(doc);XWPFParagraph header=doc.createParagraph();header.setAlignment(ParagraphAlignment.CENTER);XWPFRun h=header.createRun();h.setBold(true);h.setFontFamily(FONT);h.setFontSize(20);h.setColor("123153");h.setText(SCHOOL);XWPFRun sub=header.createRun();sub.addBreak();sub.setFontFamily(FONT);sub.setFontSize(9);sub.setText(ADDRESS);sub.addBreak();sub.setText(CONTACT);XWPFParagraph slogan=doc.createParagraph();slogan.setAlignment(ParagraphAlignment.CENTER);XWPFRun sr=slogan.createRun();sr.setItalic(true);sr.setFontFamily(FONT);sr.setColor("B38A2E");sr.setText(SLOGAN);XWPFParagraph title=doc.createParagraph();title.setAlignment(ParagraphAlignment.CENTER);XWPFRun tr=title.createRun();tr.setBold(true);tr.setFontFamily(FONT);tr.setFontSize(22);tr.setColor("123153");tr.setText("BSO-ZERTIFIKAT");XWPFParagraph intro=doc.createParagraph();intro.setAlignment(ParagraphAlignment.CENTER);intro.createRun().setText("Hiermit wird bescheinigt");addDetails(doc,p,b,level);addHeading(doc,"Veranstaltungen im Schuljahr "+b.schoolYear());addEventTable(doc,b.events());addHeading(doc,"Praktika");addPracticeTable(doc,b.practices());XWPFParagraph foot=doc.createParagraph();foot.setSpacingBefore(360);foot.createRun().setText("Ort/Datum: Landstuhl, "+p.get("asOf"));foot.createRun().addBreak();foot.createRun().setText("Unterschrift BSO-Koordination: ____________________________________");doc.write(out);return out.toByteArray();}catch(Exception ex){throw new IllegalStateException("DOCX konnte nicht erzeugt werden",ex);}}
    private void setMargins(XWPFDocument doc){var sect=doc.getDocument().getBody().addNewSectPr();var pg=sect.addNewPgMar();pg.setTop(650);pg.setBottom(650);pg.setLeft(900);pg.setRight(900);}
    private void addDetails(XWPFDocument doc,Map<String,Object> p,Breakdown b,CoinRules.Level level){XWPFTable t=doc.createTable(6,4);String[][] x={{"Name",String.valueOf(p.get("name")),"Klasse",String.valueOf(p.get("class"))},{"Jahrgang",String.valueOf(p.get("grade")),"Bildungsgang",String.valueOf(p.get("track"))},{"Stichtag",String.valueOf(p.get("asOf")),"Schuljahr",b.schoolYear()},{"Zertifikatsstufe",levelText(level),"", ""},{"Vorjahresmünzen",b.previous().toPlainString(),"Jahresmünzen",b.annual().toPlainString()},{"Gesamtmünzen",b.total().toPlainString(),"", ""}};for(int r=0;r<x.length;r++)for(int c=0;c<4;c++)setCell(t.getRow(r).getCell(c),x[r][c],c%2==0);}
    private void addHeading(XWPFDocument doc,String value){XWPFParagraph p=doc.createParagraph();p.setSpacingBefore(180);XWPFRun r=p.createRun();r.setBold(true);r.setFontFamily(FONT);r.setColor("123153");r.setText(value.toUpperCase(Locale.ROOT));}
    private void addEventTable(XWPFDocument doc,List<EventLine> rows){XWPFTable t=doc.createTable(rows.size()+1,3);String[][] x=new String[rows.size()+1][3];x[0]=new String[]{"Datum","Veranstaltung","Münzen"};for(int i=0;i<rows.size();i++)x[i+1]=new String[]{rows.get(i).date(),rows.get(i).title(),rows.get(i).coins().toPlainString()};fillTable(t,x);}
    private void addPracticeTable(XWPFDocument doc,List<PracticeLine> rows){XWPFTable t=doc.createTable(rows.size()+1,4);String[][] x=new String[rows.size()+1][4];x[0]=new String[]{"Art / Betrieb","Zeitraum","Tage","Münzen"};for(int i=0;i<rows.size();i++)x[i+1]=new String[]{rows.get(i).kind()+" / "+rows.get(i).company(),rows.get(i).period(),Integer.toString(rows.get(i).days()),rows.get(i).coins().toPlainString()};fillTable(t,x);}
    private void fillTable(XWPFTable t,String[][] x){for(int r=0;r<x.length;r++)for(int c=0;c<x[r].length;c++)setCell(t.getRow(r).getCell(c),x[r][c],r==0);}
    private void setCell(XWPFTableCell cell,String value,boolean heading){cell.removeParagraph(0);XWPFParagraph p=cell.addParagraph();p.setSpacingAfter(0);XWPFRun r=p.createRun();r.setFontFamily(FONT);r.setFontSize(heading?9:8);r.setBold(heading);r.setColor(heading?"FFFFFF":"1D2733");r.setText(value);var shd=cell.getCTTc().addNewTcPr().addNewShd();shd.setFill(heading?"123153":"F3F5F7");}
    private String levelText(CoinRules.Level level){return switch(level){case SUCCESS->"Mit Erfolg erworben";case SPECIAL_SUCCESS->"Mit besonderem Erfolg erworben";case DISTINCTION->"Mit Auszeichnung erworben";default->level.name();};}
    private String toJson(Map<String,Object> values){StringBuilder j=new StringBuilder("{");boolean first=true;for(var e:values.entrySet()){if(!first)j.append(',');first=false;j.append('"').append(e.getKey()).append("\":\"").append(String.valueOf(e.getValue()).replace("\"","\\\"")).append('"');}return j.append('}').toString();}
}

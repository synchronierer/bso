package de.igsnanstein.bso;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** CSV-Import mit Vorschau. Keine Datenbankänderung vor ausdrücklicher Bestätigung. */
@RestController
@RequestMapping("/api/import")
public class CsvImportController {
    private final JdbcTemplate db;
    CsvImportController(JdbcTemplate db){this.db=db;}

    record CsvRequest(String csv, Boolean confirmHighValue){}
    record Preview(List<Map<String,Object>> rows,int added,int updated,int unchanged,int errors){}
    private record Row(String action, Map<String,Object> data, String error){}

    @PostMapping("/students/preview")
    public Preview previewStudents(@RequestBody CsvRequest request){
        return preview(request,"students");
    }
    @PostMapping("/events/preview")
    public Preview previewEvents(@RequestBody CsvRequest request){
        return preview(request,"events");
    }
    @PostMapping("/students/confirm")
    @Transactional
    public Map<String,Object> importStudents(@RequestBody CsvRequest request){
        Preview p=preview(request,"students");
        if(p.errors()>0) throw bad("Import enthält Fehler; zuerst korrigieren");
        int changed=0;
        for(Map<String,Object> r:p.rows()){
            String action=(String)r.get("action");
            if("UNCHANGED".equals(action))continue;
            String id=(String)r.get("studentId");
            String first=(String)r.get("firstName"),last=(String)r.get("lastName");
            String klass=(String)r.get("className"), track=(String)r.get("track");
            int grade=(int)r.get("grade");
            if("ADD".equals(action)){
                db.update("INSERT INTO students(student_id,first_name,last_name,class_name,grade,track) VALUES (?,?,?,?,?,?)",
                    id,first,last,klass,grade,track);
            }else {
                db.update("UPDATE students SET first_name=?,last_name=?,class_name=?,grade=?,track=? WHERE student_id=?",
                    first,last,klass,grade,track,id);
            }
            changed++;
        }
        return Map.of("message","Schülerimport abgeschlossen","changed",changed);
    }
    @PostMapping("/events/confirm")
    @Transactional
    public Map<String,Object> importEvents(@RequestBody CsvRequest request){
        Preview p=preview(request,"events");
        if(p.errors()>0) throw bad("Import enthält Fehler; zuerst korrigieren");
        if(p.rows().stream().anyMatch(row -> row.containsKey("warning")) && !Boolean.TRUE.equals(request.confirmHighValue()))
            throw bad("Veranstaltungen über 20 Münzen benötigen eine ausdrückliche Bestätigung");
        int changed=0;
        for(Map<String,Object> r:p.rows()){
            if(!"ADD".equals(r.get("action")))continue;
            db.update("INSERT INTO events(title,end_date,coins,kind) VALUES (?,CAST(? AS DATE),?,'WAHLPFLICHT')",
                r.get("title"),r.get("endDate"),new BigDecimal((String)r.get("coins")));
            changed++;
        }
        return Map.of("message","Veranstaltungsimport abgeschlossen","changed",changed);
    }

    private Preview preview(CsvRequest req,String type){
        if(req==null || req.csv()==null || req.csv().isBlank() || req.csv().length()>2_000_000)
            throw bad("Bitte CSV-Datei bis maximal 2 MB auswählen");
        String source=req.csv().replaceFirst("^\\uFEFF","");
        char delimiter=source.lines().findFirst().orElse("").contains(";")?';':',';
        CSVFormat fmt=CSVFormat.DEFAULT.builder()
            .setDelimiter(delimiter).setHeader().setSkipHeaderRecord(true)
            .setIgnoreSurroundingSpaces(true).setTrim(true).build();
        List<Map<String,Object>> out=new ArrayList<>();
        int adds=0,updates=0,unchanged=0,errors=0;
        Set<String> keys=new HashSet<>();
        try(CSVParser parser=fmt.parse(new StringReader(source))){
            if(parser.getHeaderNames().isEmpty())throw bad("CSV-Kopfzeile fehlt");
            for(CSVRecord record:parser){
                if(record.getRecordNumber()>3000)throw bad("Maximal 3000 Datenzeilen pro Import");
                Map<String,Object> row=new LinkedHashMap<>();
                String action;
                try {
                    if("students".equals(type)){
                        String id=field(record,"studentId","SchuelerID","SchülerID","Schüler-ID","ID");
                        String first=field(record,"firstName","Vorname"), last=field(record,"lastName","Nachname");
                        String klass=field(record,"className","Klasse");
                        String gradeS=field(record,"grade","Jahrgang");
                        String track=field(record,"track","Bildungsgang");
                        int grade=Integer.parseInt(gradeS);
                        if(id.isBlank()||first.isBlank()||last.isBlank()||klass.isBlank()||track.isBlank()
                                ||grade<7 || grade>13)throw new IllegalArgumentException("Pflichtfeld fehlt oder Jahrgang ungültig");
                        if(id.length()>80||first.length()>150||last.length()>150||klass.length()>50||track.length()>30)
                            throw new IllegalArgumentException("Feld zu lang");
                        if(!keys.add(id))throw new IllegalArgumentException("Doppelte Schüler-ID in CSV");
                        row.put("studentId",id);row.put("firstName",first);row.put("lastName",last);
                        row.put("className",klass);row.put("grade",grade);row.put("track",track);
                        List<Map<String,Object>> existing=db.queryForList(
                            "SELECT first_name,last_name,class_name,grade,track FROM students WHERE student_id=?",id);
                        if(existing.isEmpty())action="ADD";
                        else {
                            Map<String,Object> old=existing.get(0);
                            action=Objects.equals(old.get("first_name"),first)
                                && Objects.equals(old.get("last_name"),last)
                                && Objects.equals(old.get("class_name"),klass)
                                && ((Number)old.get("grade")).intValue()==grade
                                && Objects.equals(old.get("track"),track)?"UNCHANGED":"UPDATE";
                        }
                    } else {
                        String title=field(record,"title","Veranstaltung","Name");
                        String end=field(record,"endDate","Enddatum","Abschlussdatum","Datum");
                        String coinsS=field(record,"coins","Muenzen","Münzen","Münzwert").replace(',','.');
                        if(title.isBlank()||title.length()>255)throw new IllegalArgumentException("Name fehlt oder zu lang");
                        LocalDate.parse(end);
                        BigDecimal coins=new BigDecimal(coinsS);
                        if(coins.compareTo(new BigDecimal("0.5"))<0 || coins.remainder(new BigDecimal("0.5")).signum()!=0)
                            throw new IllegalArgumentException("Münzwert nur in 0,5er-Schritten ab 0,5");
                        if(coins.compareTo(new BigDecimal("20"))>0)
                            row.put("warning","Über 20 Münzen – bestätigungspflichtig");
                        String key=title+"|"+end;
                        if(!keys.add(key))throw new IllegalArgumentException("Doppelte Veranstaltung in CSV");
                        row.put("title",title);row.put("endDate",end);row.put("coins",coins.toPlainString());
                        Integer n=db.queryForObject("SELECT COUNT(*) FROM events WHERE title=? AND end_date=CAST(? AS DATE)",
                             Integer.class,title,end);
                        action=(n!=null && n>0)?"UNCHANGED":"ADD";
                    }
                }catch(Exception ex){
                    action="ERROR";
                    row.put("error",ex.getMessage()==null?"Ungültige Daten":ex.getMessage());
                    row.put("line",record.getRecordNumber()+1);
                }
                row.put("action",action);
                out.add(row);
                switch(action){
                    case "ADD"->adds++;
                    case "UPDATE"->updates++;
                    case "UNCHANGED"->unchanged++;
                    default->errors++;
                }
            }
        } catch(ResponseStatusException ex){throw ex;}
        catch(Exception ex){throw bad("CSV konnte nicht gelesen werden: "+ex.getMessage());}
        return new Preview(out,adds,updates,unchanged,errors);
    }
    private String field(CSVRecord record,String...names){
        for(String name:names){
            for(String header:record.getParser().getHeaderNames()){
                if(header.equalsIgnoreCase(name))return record.get(header).trim();
            }
        }
        throw new IllegalArgumentException("Fehlende Spalte: "+names[0]);
    }
    private ResponseStatusException bad(String msg){
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,msg);
    }
}

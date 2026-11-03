package de.igsnanstein.bso;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

/** XLSX counterpart to the preview-first CSV import. No write occurs during preview. */
@RestController
@RequestMapping("/api/import/xlsx")
public class XlsxImportController {
    private final JdbcTemplate db;
    private final DataFormatter formatter = new DataFormatter(Locale.GERMANY);
    XlsxImportController(JdbcTemplate db) { this.db = db; }

    record Preview(List<Map<String,Object>> rows, int added, int updated, int unchanged, int errors, String sha256) {}

    @PostMapping("/{kind}/preview")
    public Preview preview(@PathVariable String kind, @RequestPart("file") MultipartFile file) {
        return parse(kind, file);
    }

    @PostMapping("/{kind}/confirm")
    @Transactional
    public Map<String,Object> confirm(@PathVariable String kind, @RequestPart("file") MultipartFile file,
                                      @RequestParam(defaultValue="false") boolean confirmHighValue, Authentication auth) {
        Preview preview = parse(kind, file);
        if (preview.errors() > 0) throw bad("Import enthält Fehler; zuerst korrigieren");
        if (preview.rows().stream().anyMatch(row -> row.containsKey("warning")) && !confirmHighValue)
            throw bad("Veranstaltungen über 20 Münzen benötigen eine ausdrückliche Bestätigung");
        int changed = 0;
        for (Map<String,Object> row : preview.rows()) {
            if ("UNCHANGED".equals(row.get("action"))) continue;
            if ("students".equals(kind)) {
                if ("ADD".equals(row.get("action"))) db.update("""
                    INSERT INTO students(student_id,first_name,last_name,class_name,grade,track)
                    VALUES (?,?,?,?,?,?)""", row.get("studentId"),row.get("firstName"),row.get("lastName"),
                    row.get("className"),row.get("grade"),row.get("track"));
                else db.update("""
                    UPDATE students SET first_name=?,last_name=?,class_name=?,grade=?,track=?,version=version+1
                    WHERE student_id=?""", row.get("firstName"),row.get("lastName"),row.get("className"),
                    row.get("grade"),row.get("track"),row.get("studentId"));
            } else if ("ADD".equals(row.get("action"))) {
                db.update("INSERT INTO events(title,end_date,coins,kind) VALUES (?,CAST(? AS DATE),?,?)",
                    row.get("title"),row.get("endDate"),row.get("coins"),row.get("kind"));
            }
            changed++;
        }
        db.update("""
            INSERT INTO import_runs(kind,content_hash,status,row_count,created_by)
            VALUES (?,?, 'CONFIRMED',?,?) ON CONFLICT(kind,content_hash)
            DO UPDATE SET status='CONFIRMED',row_count=EXCLUDED.row_count,created_by=EXCLUDED.created_by""",
            kind,preview.sha256(),preview.rows().size(),auth.getName());
        return Map.of("message","XLSX-Import abgeschlossen","changed",changed,"hash",preview.sha256());
    }

    private Preview parse(String kind, MultipartFile file) {
        if (!Set.of("students","events").contains(kind)) throw bad("Unbekannter XLSX-Importtyp");
        if (file == null || file.isEmpty() || file.getSize() > 10_000_000)
            throw bad("Bitte eine XLSX-Datei bis maximal 10 MB auswählen");
        List<Map<String,Object>> rows = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        int added=0, updated=0, unchanged=0, errors=0;
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) throw bad("XLSX-Kopfzeile fehlt");
            Map<String,Integer> columns = headers(header);
            for (int i=1; i<=sheet.getLastRowNum(); i++) {
                if (rows.size() >= 3000) throw bad("Maximal 3000 Datenzeilen pro Import");
                Row source = sheet.getRow(i);
                if (source == null || isEmpty(source)) continue;
                Map<String,Object> row = new LinkedHashMap<>();
                String action;
                try {
                    if ("students".equals(kind)) {
                        String id=value(source,columns,"studentId","SchuelerID","Schüler-ID","ID");
                        String first=value(source,columns,"firstName","Vorname");
                        String last=value(source,columns,"lastName","Nachname");
                        String klass=value(source,columns,"className","Klasse");
                        int grade=Integer.parseInt(value(source,columns,"grade","Jahrgang"));
                        String track=value(source,columns,"track","Bildungsgang");
                        if (id.isBlank()||first.isBlank()||last.isBlank()||klass.isBlank()||track.isBlank()||grade<7||grade>13)
                            throw new IllegalArgumentException("Pflichtfeld fehlt oder Jahrgang ungültig");
                        if (!keys.add(id)) throw new IllegalArgumentException("Doppelte Schüler-ID in XLSX");
                        row.put("studentId",id);row.put("firstName",first);row.put("lastName",last);
                        row.put("className",klass);row.put("grade",grade);row.put("track",track);
                        List<Map<String,Object>> old=db.queryForList("SELECT first_name,last_name,class_name,grade,track FROM students WHERE student_id=?",id);
                        if (old.isEmpty()) action="ADD";
                        else action=Objects.equals(old.get(0).get("first_name"),first)
                            && Objects.equals(old.get(0).get("last_name"),last)
                            && Objects.equals(old.get(0).get("class_name"),klass)
                            && ((Number)old.get(0).get("grade")).intValue()==grade
                            && Objects.equals(old.get(0).get("track"),track) ? "UNCHANGED" : "UPDATE";
                    } else {
                        String title=value(source,columns,"title","Veranstaltung","Name");
                        LocalDate end=LocalDate.parse(value(source,columns,"endDate","Enddatum","Datum"));
                        BigDecimal coins=new BigDecimal(value(source,columns,"coins","Muenzen","Münzen","Münzwert").replace(',','.'));
                        String eventKind=optional(source,columns,"kind","Art").toUpperCase(Locale.ROOT);
                        if (eventKind.isBlank()) eventKind="WAHLPFLICHT";
                        CoinRules.requireCoinValue(coins,false);
                        if (title.isBlank() || title.length()>255 || !Set.of("PFLICHT","WAHLPFLICHT","WORKSHOP").contains(eventKind))
                            throw new IllegalArgumentException("Name oder Veranstaltungsart ungültig");
                        String key=title+"|"+end;
                        if (!keys.add(key)) throw new IllegalArgumentException("Doppelte Veranstaltung in XLSX");
                        row.put("title",title);row.put("endDate",end.toString());row.put("coins",coins);row.put("kind",eventKind);
                        if (coins.compareTo(new BigDecimal("20")) > 0) row.put("warning","Über 20 Münzen – bestätigungspflichtig");
                        Integer count=db.queryForObject("SELECT COUNT(*) FROM events WHERE title=? AND end_date=?",Integer.class,title,end);
                        action=count!=null&&count>0?"UNCHANGED":"ADD";
                    }
                } catch (Exception ex) {
                    action="ERROR"; row.put("error",ex.getMessage()==null?"Ungültige Daten":ex.getMessage()); row.put("line",i+1);
                }
                row.put("action",action); rows.add(row);
                switch(action) { case "ADD" -> added++; case "UPDATE" -> updated++; case "UNCHANGED" -> unchanged++; default -> errors++; }
            }
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) { throw bad("XLSX konnte nicht gelesen werden: "+ex.getMessage()); }
        return new Preview(rows,added,updated,unchanged,errors,sha256(file));
    }

    private Map<String,Integer> headers(Row row) {
        Map<String,Integer> result=new HashMap<>();
        for (int i=0;i<row.getLastCellNum();i++) result.put(normalize(formatter.formatCellValue(row.getCell(i))),i);
        return result;
    }
    private String value(Row row,Map<String,Integer> columns,String... names) {
        for(String name:names) { Integer i=columns.get(normalize(name)); if(i!=null) return formatter.formatCellValue(row.getCell(i)).trim(); }
        throw new IllegalArgumentException("Fehlende Spalte: "+names[0]);
    }
    private String optional(Row row,Map<String,Integer> columns,String... names) {
        for(String name:names) { Integer i=columns.get(normalize(name)); if(i!=null) return formatter.formatCellValue(row.getCell(i)).trim(); }
        return "";
    }
    private String normalize(String value) { return value==null?"":value.replace("\uFEFF","").trim().toLowerCase(Locale.ROOT); }
    private boolean isEmpty(Row row) { for(int i=0;i<row.getLastCellNum();i++) if(!formatter.formatCellValue(row.getCell(i)).isBlank()) return false; return true; }
    private String sha256(MultipartFile file) { try { byte[] digest=MessageDigest.getInstance("SHA-256").digest(file.getBytes()); return HexFormat.of().formatHex(digest); } catch(Exception ex) { throw bad("Datei konnte nicht geprüft werden"); } }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
}

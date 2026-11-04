package de.igsnanstein.bso;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.commons.csv.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Reusable internship company directory; importing does not touch pupils or coins. */
@RestController
@RequestMapping("/api/internship-companies")
public class InternshipCompanyController {
    private final JdbcTemplate db;
    InternshipCompanyController(JdbcTemplate db) { this.db=db; }

    record Preview(List<Map<String,Object>> rows,int added,int unchanged,int errors) {}

    @GetMapping
    public List<Map<String,Object>> list() {
        return db.queryForList("SELECT id,name FROM internship_companies ORDER BY lower(name),id");
    }

    @PostMapping("/preview")
    public Preview preview(@RequestPart("file") MultipartFile file) { return parse(file); }

    @PostMapping("/confirm")
    @Transactional
    public Map<String,Object> confirm(@RequestPart("file") MultipartFile file,Authentication auth) {
        Preview p=parse(file);
        if(p.errors()>0) throw bad("Betriebsimport enthält Fehler; bitte zuerst korrigieren");
        int changed=0;
        for(Map<String,Object> row:p.rows()){
            if(!"ADD".equals(row.get("action")))continue;
            changed+=db.update("""
                INSERT INTO internship_companies(name) VALUES (?)
                ON CONFLICT DO NOTHING
                """,row.get("name"));
        }
        db.update("""
            INSERT INTO audit_events(actor,action,object_type,object_id,before_json,after_json)
            VALUES (?, 'INTERNSHIP_COMPANIES_IMPORT', 'internship_companies', ?,
                    CAST('null' AS JSONB),to_jsonb(CAST(? AS TEXT)))
            """,auth.getName(),Integer.toString(changed),Integer.toString(changed));
        return Map.of("message","Praktikumsbetriebe übernommen","changed",changed);
    }

    private Preview parse(MultipartFile file) {
        if(file==null||file.isEmpty()||file.getSize()>10_000_000)
            throw bad("Bitte eine CSV- oder XLSX-Datei bis 10 MB auswählen");
        String filename=Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        try{
            List<String> names;
            if(filename.endsWith(".csv"))names=csv(file);
            else if(filename.endsWith(".xlsx"))names=xlsx(file);
            else throw bad("Nur CSV und XLSX werden unterstützt");
            if(names.size()>3000)throw bad("Maximal 3000 Betriebe je Datei");
            Set<String> seen=new HashSet<>();
            List<Map<String,Object>> out=new ArrayList<>();
            int added=0,unchanged=0,errors=0;
            for(int i=0;i<names.size();i++){
                String name=names.get(i).trim();
                Map<String,Object> row=new LinkedHashMap<>();
                row.put("line",i+2);row.put("name",name);
                String key=name.toLowerCase(Locale.ROOT);
                String action;
                if(name.isEmpty()||name.length()>200){
                    action="ERROR";row.put("error","Betriebsname fehlt oder ist länger als 200 Zeichen");errors++;
                }else if(!seen.add(key)){
                    action="ERROR";row.put("error","Doppelter Betrieb innerhalb der Importdatei");errors++;
                }else{
                    Integer n=db.queryForObject("SELECT COUNT(*) FROM internship_companies WHERE lower(btrim(name))=?",Integer.class,key);
                    action=n!=null&&n>0?"UNCHANGED":"ADD";
                    if("ADD".equals(action))added++;else unchanged++;
                }
                row.put("action",action);out.add(row);
            }
            return new Preview(out,added,unchanged,errors);
        }catch(ResponseStatusException ex){throw ex;}
        catch(Exception ex){throw bad("Datei konnte nicht gelesen werden: "+ex.getMessage());}
    }

    private List<String> csv(MultipartFile file) throws IOException {
        String source=new String(file.getBytes(),StandardCharsets.UTF_8).replaceFirst("^\\uFEFF","");
        char delimiter=source.lines().findFirst().orElse("").contains(";")?';':',';
        CSVFormat format=CSVFormat.DEFAULT.builder().setDelimiter(delimiter).setHeader()
            .setSkipHeaderRecord(true).setTrim(true).setIgnoreSurroundingSpaces(true).get();
        try(CSVParser parser=format.parse(new StringReader(source))){
            String col=column(parser.getHeaderNames());
            List<String> names=new ArrayList<>();
            for(CSVRecord record:parser){
                if(names.size()>=3000)throw bad("Maximal 3000 Betriebe je Datei");
                names.add(record.get(col));
            }
            return names;
        }
    }

    private List<String> xlsx(MultipartFile file) throws IOException {
        try(XSSFWorkbook book=new XSSFWorkbook(file.getInputStream())){
            if(book.getNumberOfSheets()==0)throw bad("XLSX enthält kein Arbeitsblatt");
            Sheet sheet=book.getSheetAt(0);
            Row header=sheet.getRow(0);
            if(header==null)throw bad("Kopfzeile fehlt");
            DataFormatter format=new DataFormatter(Locale.GERMANY);
            int column=-1;
            for(Cell cell:header)if(isNameHeader(format.formatCellValue(cell))){column=cell.getColumnIndex();break;}
            if(column<0)throw bad("Spalte 'Betrieb' oder 'name' fehlt");
            List<String> names=new ArrayList<>();
            for(int i=1;i<=sheet.getLastRowNum();i++){
                Row row=sheet.getRow(i);
                if(row==null)continue;
                String value=format.formatCellValue(row.getCell(column)).trim();
                if(value.isBlank()&&row.getLastCellNum()<0)continue;
                if(names.size()>=3000)throw bad("Maximal 3000 Betriebe je Datei");
                names.add(value);
            }
            return names;
        }
    }

    private String column(List<String> headers){
        for(String h:headers)if(isNameHeader(h))return h;
        throw bad("Spalte 'Betrieb' oder 'name' fehlt");
    }
    private boolean isNameHeader(String s){
        return s!=null&&Set.of("betrieb","name","company","betriebsname")
            .contains(s.replace("\uFEFF","").trim().toLowerCase(Locale.ROOT));
    }
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}

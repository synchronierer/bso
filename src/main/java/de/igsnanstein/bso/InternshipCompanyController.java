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

/** User-imported internship companies, with short labels and sorting. No pupil data. */
@RestController
@RequestMapping("/api/internship-companies")
public class InternshipCompanyController {
    private final JdbcTemplate db;
    InternshipCompanyController(JdbcTemplate db) { this.db=db; }

    record Preview(List<Map<String,Object>> rows,int added,int updated,int unchanged,int errors) {}
    private record Company(String shortName,String name,String sortText,int line) {}

    @GetMapping
    public List<Map<String,Object>> list() {
        return db.queryForList("""
            SELECT id,name,short_name AS "shortName",sort_order AS "sortOrder"
            FROM internship_companies
            ORDER BY sort_order NULLS LAST, lower(short_name),id
            """);
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
            if("ADD".equals(row.get("action"))){
                changed+=db.update("""
                    INSERT INTO internship_companies(name,short_name,sort_order) VALUES (?,?,?)
                    ON CONFLICT DO NOTHING
                    """,row.get("name"),row.get("shortName"),row.get("sortOrder"));
            }else if("UPDATE".equals(row.get("action"))){
                changed+=db.update("""
                    UPDATE internship_companies SET short_name=?,sort_order=?
                    WHERE lower(btrim(name))=lower(btrim(?))
                    """,row.get("shortName"),row.get("sortOrder"),row.get("name"));
            }
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
            List<Company> companies;
            if(filename.endsWith(".csv"))companies=csv(file);
            else if(filename.endsWith(".xlsx"))companies=xlsx(file);
            else throw bad("Nur CSV und XLSX werden unterstützt");
            if(companies.size()>3000)throw bad("Maximal 3000 Betriebe je Datei");
            Set<String> seen=new HashSet<>();
            List<Map<String,Object>> out=new ArrayList<>();
            int added=0,updated=0,unchanged=0,errors=0;
            for(Company company:companies){
                String name=company.name().trim();
                String shortName=company.shortName().trim();
                if(shortName.isEmpty())shortName=name;
                Map<String,Object> row=new LinkedHashMap<>();
                row.put("line",company.line());row.put("name",name);row.put("shortName",shortName);
                String key=name.toLowerCase(Locale.ROOT);
                String action;
                Integer order=null;
                String error=null;
                if(name.isEmpty()||name.length()>200||shortName.isEmpty()||shortName.length()>200)
                    error="Betriebsname oder Kürzel fehlt bzw. überschreitet 200 Zeichen";
                else if(!company.sortText().isBlank())try{
                    order=Integer.valueOf(company.sortText().trim());
                    if(order<0)error="Sortiernummer darf nicht negativ sein";
                }catch(NumberFormatException ex){error="Sortiernummer muss eine ganze Zahl sein";}
                if(error==null&&!seen.add(key))error="Doppelter Betrieb innerhalb der Importdatei";
                if(error!=null){
                    action="ERROR";row.put("error",error);errors++;
                }else{
                    Integer n=db.queryForObject("SELECT COUNT(*) FROM internship_companies WHERE lower(btrim(name))=?",Integer.class,key);
                    if(n==null||n==0){action="ADD";added++;}
                    else{
                        List<Map<String,Object>> old=db.queryForList("""
                            SELECT short_name,sort_order FROM internship_companies
                            WHERE lower(btrim(name))=?
                            """,key);
                        if(old.isEmpty() || (Objects.equals(old.get(0).get("short_name"),shortName)
                            && Objects.equals(old.get(0).get("sort_order"),order))){
                            action="UNCHANGED";unchanged++;
                        }else{action="UPDATE";updated++;}
                    }
                }
                row.put("sortOrder",order);row.put("action",action);out.add(row);
            }
            return new Preview(out,added,updated,unchanged,errors);
        }catch(ResponseStatusException ex){throw ex;}
        catch(Exception ex){throw bad("Datei konnte nicht gelesen werden: "+ex.getMessage());}
    }

    private List<Company> csv(MultipartFile file) throws IOException {
        String source=new String(file.getBytes(),StandardCharsets.UTF_8).replaceFirst("^\\uFEFF","");
        char delimiter=source.lines().findFirst().orElse("").contains(";")?';':',';
        CSVFormat format=CSVFormat.DEFAULT.builder().setDelimiter(delimiter).setHeader()
            .setSkipHeaderRecord(true).setTrim(true).setIgnoreSurroundingSpaces(true).get();
        try(CSVParser parser=format.parse(new StringReader(source))){
            String nameCol=column(parser.getHeaderNames(),"betriebename","betrieb","name","company","betriebsname");
            String shortCol=columnOptional(parser.getHeaderNames(),"betriebekürzel","betriebekuerzel","kürzel","kuerzel","shortname");
            String sortCol=columnOptional(parser.getHeaderNames(),"sortiernummer","sortorder","sortierung");
            List<Company> companies=new ArrayList<>();
            for(CSVRecord record:parser){
                if(companies.size()>=3000)throw bad("Maximal 3000 Betriebe je Datei");
                String name=record.get(nameCol);
                companies.add(new Company(shortCol==null?name:record.get(shortCol),name,
                    sortCol==null?"":record.get(sortCol),(int)record.getRecordNumber()+1));
            }
            return companies;
        }
    }

    private List<Company> xlsx(MultipartFile file) throws IOException {
        try(XSSFWorkbook book=new XSSFWorkbook(file.getInputStream())){
            if(book.getNumberOfSheets()==0)throw bad("XLSX enthält kein Arbeitsblatt");
            Sheet sheet=book.getSheetAt(0);
            Row header=sheet.getRow(0);
            if(header==null)throw bad("Kopfzeile fehlt");
            DataFormatter fmt=new DataFormatter(Locale.GERMANY);
            Map<String,Integer> cols=new HashMap<>();
            for(Cell cell:header)cols.put(norm(fmt.formatCellValue(cell)),cell.getColumnIndex());
            Integer nameCol=first(cols,"betriebename","betrieb","name","company","betriebsname");
            if(nameCol==null)throw bad("Spalte 'Betriebename' oder 'Betrieb' fehlt");
            Integer shortCol=first(cols,"betriebekürzel","betriebekuerzel","kürzel","kuerzel","shortname");
            Integer sortCol=first(cols,"sortiernummer","sortorder","sortierung");
            List<Company> companies=new ArrayList<>();
            for(int i=1;i<=sheet.getLastRowNum();i++){
                Row row=sheet.getRow(i);
                if(row==null)continue;
                String name=fmt.formatCellValue(row.getCell(nameCol)).trim();
                if(name.isEmpty()&&row.getPhysicalNumberOfCells()==0)continue;
                if(companies.size()>=3000)throw bad("Maximal 3000 Betriebe je Datei");
                companies.add(new Company(shortCol==null?name:fmt.formatCellValue(row.getCell(shortCol)),
                    name,sortCol==null?"":fmt.formatCellValue(row.getCell(sortCol)),i+1));
            }
            return companies;
        }
    }
    private String column(List<String> names,String...options){
        String result=columnOptional(names,options);
        if(result==null)throw bad("Spalte 'Betriebename' oder 'Betrieb' fehlt");
        return result;
    }
    private String columnOptional(List<String> names,String...options){
        for(String name:names)for(String option:options)
            if(norm(name).equals(norm(option)))return name;
        return null;
    }
    private Integer first(Map<String,Integer> cols,String...options){
        for(String option:options)if(cols.containsKey(norm(option)))return cols.get(norm(option));
        return null;
    }
    private String norm(String value){
        return value.replace("\uFEFF","").trim().toLowerCase(Locale.ROOT);
    }
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}

package de.igsnanstein.bso;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {
    private final JdbcTemplate db;
    private final CertificateService certificates;
    CertificateController(JdbcTemplate db,CertificateService certificates){this.db=db;this.certificates=certificates;}

    @GetMapping("/{studentPk}")
    public Map<String,Object> status(@PathVariable long studentPk,@RequestParam(defaultValue="") String asOf) {
        LocalDate date=date(asOf); Map<String,Object> student=db.queryForMap("SELECT id,student_id,first_name,last_name,class_name,grade,track FROM students WHERE id=? AND active=true",studentPk);
        java.math.BigDecimal coins=db.queryForObject("SELECT COALESCE(SUM(g.coins),0) FROM coin_grants g JOIN events e ON e.id=g.event_id WHERE g.student_pk=? AND g.excluded=false AND e.end_date<=?",java.math.BigDecimal.class,studentPk,date);
        java.math.BigDecimal practice=db.queryForObject("SELECT COALESCE(SUM(coins),0) FROM internships WHERE student_pk=? AND finalised=true AND ends_on<=?",java.math.BigDecimal.class,studentPk,date);
        coins=coins.add(practice); int grade=((Number)student.get("grade")).intValue(); String track=(String)student.get("track");
        return Map.of("studentId",student.get("student_id"),"name",student.get("first_name")+" "+student.get("last_name"),"date",date.toString(),"coins",coins,"level",CoinRules.levelFor(coins,grade,track).name(),"nextDistance",String.valueOf(CoinRules.nextThresholdDistance(coins,grade,track)));
    }
    @GetMapping(value="/{studentPk}/pdf",produces="application/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable long studentPk,@RequestParam(defaultValue="") String asOf,@RequestParam(defaultValue="false") boolean newVersion,Authentication auth){
        CertificateService.Generated generated=certificates.issue(studentPk,date(asOf),auth.getName(),newVersion);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=BSO-Zertifikat-"+studentPk+".pdf").body(generated.pdf());
    }
    @GetMapping(value="/{studentPk}/docx",produces="application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    public ResponseEntity<byte[]> docx(@PathVariable long studentPk,@RequestParam(defaultValue="") String asOf,@RequestParam(defaultValue="false") boolean newVersion,Authentication auth){
        CertificateService.Generated generated=certificates.issue(studentPk,date(asOf),auth.getName(),newVersion);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=BSO-Zertifikat-"+studentPk+".docx").body(generated.docx());
    }
    @GetMapping(value="/class/{className}.zip",produces="application/zip")
    public ResponseEntity<byte[]> classZip(@PathVariable String className,@RequestParam(defaultValue="") String asOf,@RequestParam(defaultValue="false") boolean onlyValid,Authentication auth){
        LocalDate date=date(asOf); List<Map<String,Object>> students=db.queryForList("SELECT id,student_id,last_name,first_name FROM students WHERE class_name=? AND active=true ORDER BY class_name,last_name,first_name,id",className);
        List<byte[]> pdfs=new ArrayList<>(); List<String> pdfNames=new ArrayList<>(); ByteArrayOutputStream zipBytes=new ByteArrayOutputStream();
        try(ZipOutputStream zip=new ZipOutputStream(zipBytes)) {
            for(Map<String,Object> student:students) try {
                CertificateService.Generated generated=certificates.issue(((Number)student.get("id")).longValue(),date,auth.getName(),false);
                String base=student.get("last_name")+"_"+student.get("first_name")+"_"+student.get("id");
                add(zip,base+".pdf",generated.pdf()); add(zip,base+".docx",generated.docx()); pdfs.add(generated.pdf());pdfNames.add(base+".pdf");
            } catch(IllegalArgumentException ex) { if(!onlyValid) throw ex; }
            if(!pdfs.isEmpty()) { ByteArrayOutputStream merged=new ByteArrayOutputStream(); PDFMergerUtility merger=new PDFMergerUtility(); for(byte[] pdf:pdfs) merger.addSource(new RandomAccessReadBuffer(pdf)); merger.setDestinationStream(merged); merger.mergeDocuments(null); add(zip,"Sammelausgabe.pdf",merged.toByteArray()); }
        } catch(Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Sammelausgabe konnte nicht erstellt werden",ex); }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=BSO-"+className+"-Zertifikate.zip").body(zipBytes.toByteArray());
    }
    private void add(ZipOutputStream zip,String name,byte[] bytes)throws IOException{zip.putNextEntry(new ZipEntry(name.replaceAll("[^A-Za-z0-9._-]","_")));zip.write(bytes);zip.closeEntry();}
    private LocalDate date(String input){try{return input==null||input.isBlank()?LocalDate.now():LocalDate.parse(input);}catch(Exception ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Stichtag muss JJJJ-MM-TT sein");}}
}

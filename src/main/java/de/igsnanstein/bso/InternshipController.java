package de.igsnanstein.bso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/internships")
public class InternshipController {
    private final JdbcTemplate db;
    InternshipController(JdbcTemplate db){this.db=db;}
    public record Request(long studentPk,String kind,String company,String startsOn,String endsOn,int recognizedDays,boolean finalised){}

    @GetMapping("/student/{studentPk}")
    public Object list(@PathVariable long studentPk){return db.queryForList("SELECT id,kind,company,starts_on AS \"startsOn\",ends_on AS \"endsOn\",recognized_days AS \"recognizedDays\",coins,finalised FROM internships WHERE student_pk=? ORDER BY starts_on",studentPk);}

    @PostMapping
    @Transactional
    public Map<String,Object> create(@RequestBody Request request,Authentication auth){
        if(request==null || request.studentPk()<=0 || request.company()==null || request.company().isBlank() || request.company().length()>200)
            throw bad("Schüler, Betrieb und Zeitraum sind Pflichtfelder");
        String kind=request.kind()==null?"":request.kind().toUpperCase();
        if(!kind.equals("PFLICHT")&&!kind.equals("ZUSATZ")) throw bad("Praktikumsart ungültig");
        LocalDate starts=parse(request.startsOn()), ends=parse(request.endsOn());
        if(ends.isBefore(starts)||request.recognizedDays()<1||(kind.equals("PFLICHT")&&request.recognizedDays()>10)||(kind.equals("ZUSATZ")&&request.recognizedDays()>5))
            throw bad("Anerkannte Tage überschreiten die fachliche Grenze");
        if(!request.finalised()) throw bad("Bitte Praktika nur durch ausdrückliche endgültige Buchung abrechnen");
        if(ends.isAfter(LocalDate.now())) throw bad("Praktikum darf erst nach seinem Enddatum endgültig verbucht werden");
        BigDecimal coins=BigDecimal.valueOf(request.recognizedDays()*10L);
        try {
            Long schoolYear=db.query("SELECT id FROM school_years WHERE starts_on<=? AND ends_on>=? ORDER BY starts_on DESC LIMIT 1",rs->rs.next()?rs.getLong(1):null,ends,ends);
            if(schoolYear==null) throw bad("Kein Schuljahr für das Praktikumsende konfiguriert");
            int count=db.queryForObject("SELECT COUNT(*) FROM students WHERE id=? AND active=true",Integer.class,request.studentPk());
            if(count!=1) throw bad("Schüler nicht vorhanden oder archiviert");
            Long episode=db.queryForObject("SELECT id FROM enrollment_episodes WHERE student_pk=? AND ends_on IS NULL ORDER BY id DESC LIMIT 1",Long.class,request.studentPk());
            if(episode==null) throw bad("Schüler hat keine offene Schulbesuchsepisode");
            // Serialize internship submissions for the same attendance episode.
            db.queryForObject("SELECT id FROM enrollment_episodes WHERE id=? FOR UPDATE",Long.class,episode);
            Integer grade=db.queryForObject("SELECT grade FROM students WHERE id=?",Integer.class,request.studentPk());
            String block=grade<=10?"SEK_I":"OBERSTUFE";
            if(kind.equals("PFLICHT")) {
                Integer prior=db.queryForObject("SELECT COUNT(*) FROM internships WHERE episode_id=? AND kind='PFLICHT' AND finalised AND coin_block=?",Integer.class,episode,block);
                if(prior!=null && prior>0) throw bad("Pflichtpraktikum wurde für diesen Münzblock bereits abgerechnet");
            } else {
                Integer prior=db.queryForObject("SELECT COUNT(*) FROM internships WHERE episode_id=? AND school_year_id=? AND kind='ZUSATZ' AND finalised",Integer.class,episode,schoolYear);
                if(prior!=null && prior>0) throw bad("Freiwilliges Praktikum wurde in diesem Schuljahr bereits abgerechnet");
            }
            int changed=db.update("INSERT INTO internships(student_pk,episode_id,school_year_id,kind,coin_block,company,starts_on,ends_on,recognized_days,coins,finalised,recorded_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                request.studentPk(),episode,schoolYear,kind,block,request.company().trim(),starts,ends,request.recognizedDays(),coins,request.finalised(),auth.getName());
            db.update("INSERT INTO audit_events(actor,action,object_type,object_id,before_json,after_json) VALUES (?,?,?, ?,CAST('null' AS JSONB),CAST(? AS JSONB))",
                auth.getName(),"INTERNSHIP_CREATE","student",Long.toString(request.studentPk()),"{\"kind\":\""+kind+"\",\"coins\":"+coins+"}");
            return Map.of("message","Praktikum gespeichert","coins",coins,"changed",changed);
        } catch(org.springframework.dao.DataIntegrityViolationException ex){throw bad("Dieses Praktikum wurde für den maßgeblichen Zeitraum bereits endgültig angerechnet");}
    }
    private LocalDate parse(String value){try{return LocalDate.parse(value);}catch(Exception ex){throw bad("Datum muss JJJJ-MM-TT sein");}}
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}

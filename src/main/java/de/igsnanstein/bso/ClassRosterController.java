package de.igsnanstein.bso;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import java.time.LocalDate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/roster")
public class ClassRosterController {
    private final JdbcTemplate db;
    private final ObjectMapper mapper;
    ClassRosterController(JdbcTemplate db,ObjectMapper mapper){this.db=db;this.mapper=mapper;}

    record Assignment(long studentPk,boolean selected,BigDecimal coins){}
    record Batch(long eventId,String className,List<Assignment> assignments){}
    @GetMapping
    public Map<String,Object> roster(@RequestParam String className,@RequestParam long eventId,Authentication auth){
        ensureClass(className);
        Map<String,Object> event=event(eventId);
        List<Map<String,Object>> students=db.queryForList("""
            SELECT s.id,s.first_name AS "firstName",s.last_name AS "lastName",
            s.student_id AS "studentId",g.coins AS "alreadyGranted"
            FROM students s LEFT JOIN enrollment_episodes ep ON ep.student_pk=s.id AND ep.ends_on IS NULL
            LEFT JOIN coin_grants g ON g.student_pk=s.id AND g.episode_id=ep.id AND g.event_id=?
            WHERE s.class_name=? ORDER BY s.last_name,s.first_name,s.id
            """,eventId,className);
        List<Map<String,Object>> drafts=db.queryForList(
                "SELECT entries::text AS entries FROM roster_drafts WHERE username=? AND event_id=? AND class_name=?",
                auth.getName(),eventId,className);
        Object draft=drafts.isEmpty()?List.of():drafts.get(0).get("entries");
        return Map.of("event",event,"students",students,"draft",draft==null?List.of():draft);
    }
    @GetMapping("/classes")
    public List<String> classes(){
        return db.query("SELECT DISTINCT class_name FROM students ORDER BY class_name",
                (rs,n)->rs.getString(1));
    }
    @PostMapping("/draft")
    @Transactional
    public Map<String,String> saveDraft(@RequestBody Batch b,Authentication auth){
        validate(b,false);
        String json;
        try{json=mapper.writeValueAsString(b.assignments());}
        catch(Exception ex){throw bad("Entwurf nicht speicherbar");}
        db.update("""
           INSERT INTO roster_drafts(username,event_id,class_name,entries) VALUES(?,?,?,CAST(? AS JSONB))
           ON CONFLICT(username,event_id,class_name)
           DO UPDATE SET entries=EXCLUDED.entries,updated_at=now()
           """,auth.getName(),b.eventId(),b.className(),json);
        return Map.of("message","Entwurf gespeichert");
    }
    @PostMapping("/confirm")
    @Transactional
    public Map<String,Object> confirm(@RequestBody Batch b,Authentication auth){
        Map<String,Object> ev=event(b.eventId());
        BigDecimal standard=(BigDecimal)ev.get("coins");
        Object endValue=ev.get("endDate");
        if(endValue instanceof java.sql.Date end && end.toLocalDate().isAfter(LocalDate.now()))
            throw bad("Buchung erst nach dem Veranstaltungsende möglich");
        if(ev.containsKey("status") && !"AKTIV".equals(ev.get("status")))
            throw bad("Veranstaltung ist beendet oder abgebrochen");
        List<Assignment> chosen=validate(b,true);
        if(chosen.isEmpty())throw bad("Keine Schülerinnen oder Schüler ausgewählt");
        for(Assignment a:chosen){
            if(a.coins()==null || a.coins().compareTo(BigDecimal.ZERO)<0
                || a.coins().compareTo(standard)>0
                || a.coins().remainder(new BigDecimal("0.5")).signum()!=0)
                throw bad("Ungültige individuelle Münzen");
            int exists=db.queryForObject("""
                SELECT COUNT(*) FROM coin_grants WHERE student_pk=? AND event_id=?
                """,Integer.class,a.studentPk(),b.eventId());
            if(exists>0)throw bad("Münzen für mindestens eine Person bereits verbucht; bitte Raster neu laden");
        }
        try {
            for(Assignment a:chosen) {
                db.update("""
                    INSERT INTO coin_grants(student_pk,event_id,coins,recorded_by,student_grade,episode_id)
                    SELECT s.id,?,?,?,s.grade,ep.id FROM students s JOIN enrollment_episodes ep ON ep.student_pk=s.id AND ep.ends_on IS NULL WHERE s.id=? AND s.active=true""",
                    b.eventId(),a.coins(),auth.getName(),a.studentPk());
            }
        } catch (DataIntegrityViolationException ex) {
            throw bad("Die Buchung wurde parallel verändert; Raster bitte neu laden");
        }
        db.update("DELETE FROM roster_drafts WHERE username=? AND event_id=? AND class_name=?",
            auth.getName(),b.eventId(),b.className());
        return Map.of("message","Münzen für "+chosen.size()+" Personen verbucht","count",chosen.size());
    }

    private List<Assignment> validate(Batch b,boolean checkExisting){
        if(b==null || b.eventId()<=0)throw bad("Veranstaltung fehlt");
        ensureClass(b.className());
        event(b.eventId());
        if(b.assignments()==null || b.assignments().size()>200)throw bad("Maximal 200 Einträge pro Klasse");
        Set<Long> ids=new HashSet<>();
        List<Assignment> chosen=new ArrayList<>();
        for(Assignment a:b.assignments()){
            if(a==null || a.studentPk()<=0 || !ids.add(a.studentPk()))throw bad("Doppelte/ungültige Schüler-ID");
            Integer count=db.queryForObject("SELECT COUNT(*) FROM students WHERE id=? AND class_name=?",
                    Integer.class,a.studentPk(),b.className());
            if(count==0)throw bad("Schüler gehört nicht zu dieser Klasse");
            if(a.selected())chosen.add(a);
        }
        return chosen;
    }
    private Map<String,Object> event(long id){
        List<Map<String,Object>> list=db.queryForList(
          "SELECT id,title,coins,end_date AS \"endDate\",status FROM events WHERE id=?",id);
        if(list.isEmpty())throw bad("Veranstaltung nicht vorhanden");
        return list.get(0);
    }
    private void ensureClass(String c){
        if(c==null || c.isBlank() || c.length()>50)throw bad("Ungültige Klasse");
    }
    private ResponseStatusException bad(String s){return new ResponseStatusException(HttpStatus.BAD_REQUEST,s);}
}

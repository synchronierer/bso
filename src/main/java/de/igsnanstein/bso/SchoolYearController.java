package de.igsnanstein.bso;

import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/school-years")
public class SchoolYearController {
    private final JdbcTemplate db;
    SchoolYearController(JdbcTemplate db) { this.db=db; }
    public record Decision(long studentPk, String decision, String className, Integer grade, String track, String notes) {}
    public record Change(long toYearId, List<Decision> decisions) {}

    @GetMapping public List<Map<String,Object>> years() { return db.queryForList("SELECT id,name,starts_on AS \"startsOn\",ends_on AS \"endsOn\" FROM school_years ORDER BY starts_on DESC"); }

    @GetMapping("/history/{studentPk}")
    public Map<String,Object> history(@PathVariable long studentPk) {
        return Map.of("episodes",db.queryForList("SELECT e.id,e.starts_on AS \"startsOn\",e.ends_on AS \"endsOn\",e.entry_reason AS \"entryReason\",e.exit_reason AS \"exitReason\",a.class_name AS \"className\",a.grade,a.track,a.effective_from AS \"effectiveFrom\",a.effective_until AS \"effectiveUntil\" FROM enrollment_episodes e LEFT JOIN student_assignments a ON a.episode_id=e.id WHERE e.student_pk=? ORDER BY e.starts_on,a.effective_from",studentPk));
    }

    @PostMapping("/change/preview")
    public Map<String,Object> preview(@RequestBody Change change) {
        if (change.decisions()==null || change.decisions().isEmpty()) throw bad("Mindestens eine Entscheidung erforderlich");
        List<Map<String,Object>> rows=new ArrayList<>();
        for(Decision d:change.decisions()) {
            Map<String,Object> s=singleStudent(d.studentPk());
            rows.add(Map.of("studentPk",d.studentPk(),"studentId",s.get("student_id"),"currentClass",s.get("class_name"),"currentGrade",s.get("grade"),"decision",d.decision(),"targetClass",Objects.requireNonNullElse(d.className(),s.get("class_name")),"targetGrade",Objects.requireNonNullElse(d.grade(),s.get("grade"))));
        }
        return Map.of("toYearId",change.toYearId(),"rows",rows,"requiresConfirmation",true);
    }

    @PostMapping("/change") @Transactional
    public Map<String,Object> apply(@RequestBody Change change, Authentication auth) {
        preview(change);
        Long current=db.queryForObject("SELECT id FROM school_years WHERE starts_on=(SELECT MAX(starts_on) FROM school_years WHERE starts_on<=CURRENT_DATE)",Long.class);
        long id=db.queryForObject("INSERT INTO school_year_changes(from_year_id,to_year_id,status,created_by) VALUES (?,?, 'ANGEWENDET',?) RETURNING id",Long.class,current,change.toYearId(),auth.getName());
        LocalDate today=LocalDate.now();
        for(Decision d:change.decisions()) {
            Map<String,Object> s=singleStudent(d.studentPk()); String decision=normal(d.decision());
            if (decision.equals("AUSTRITT")) {
                db.update("UPDATE enrollment_episodes SET ends_on=?,exit_reason=? WHERE student_pk=? AND ends_on IS NULL",today,"AUSTRITT",d.studentPk());
                db.update("UPDATE students SET active=false,version=version+1 WHERE id=?",d.studentPk());
            } else {
                int grade=Objects.requireNonNullElse(d.grade(),((Number)s.get("grade")).intValue()); String clazz=Objects.requireNonNullElse(d.className(),(String)s.get("class_name")); String track=Objects.requireNonNullElse(d.track(),(String)s.get("track"));
                if(decision.equals("WIEDEREINTRITT")) { db.update("UPDATE enrollment_episodes SET ends_on=COALESCE(ends_on,?) WHERE student_pk=? AND ends_on IS NULL",today.minusDays(1),d.studentPk()); db.update("INSERT INTO enrollment_episodes(student_pk,starts_on,entry_reason,created_by) VALUES (?,?,?,?)",d.studentPk(),today,"WIEDEREINTRITT",auth.getName()); }
                Long episode=db.queryForObject("SELECT id FROM enrollment_episodes WHERE student_pk=? AND ends_on IS NULL ORDER BY id DESC LIMIT 1",Long.class,d.studentPk());
                db.update("UPDATE student_assignments SET effective_until=? WHERE episode_id=? AND effective_until IS NULL",today.minusDays(1),episode);
                db.update("INSERT INTO student_assignments(episode_id,class_name,grade,track,effective_from,created_by) VALUES (?,?,?,?,?,?)",episode,clazz,grade,track,today,auth.getName());
                db.update("UPDATE students SET class_name=?,grade=?,track=?,active=true,version=version+1 WHERE id=?",clazz,grade,track,d.studentPk());
            }
            db.update("INSERT INTO school_year_change_decisions(change_id,student_pk,decision,class_name,grade,track,notes) VALUES (?,?,?,?,?,?,?)",id,d.studentPk(),decision,d.className(),d.grade(),d.track(),d.notes());
        }
        return Map.of("changeId",id,"status","ANGEWENDET");
    }
    private String normal(String value) { if(value==null) throw bad("Entscheidung fehlt"); String v=value.toUpperCase(Locale.ROOT); if(!Set.of("VERSETZT","WIEDERHOLUNG","AUSTRITT","WIEDEREINTRITT","UNVERAENDERT").contains(v)) throw bad("Unbekannte Schuljahresentscheidung"); return v; }
    private Map<String,Object> singleStudent(long id) { List<Map<String,Object>> r=db.queryForList("SELECT id,student_id,class_name,grade,track FROM students WHERE id=?",id); if(r.isEmpty()) throw bad("Schüler nicht gefunden: "+id); return r.get(0); }
    private ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,m); }
}

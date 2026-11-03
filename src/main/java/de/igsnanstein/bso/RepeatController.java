package de.igsnanstein.bso;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/repetitions")
public class RepeatController {
    private final JdbcTemplate db;
    RepeatController(JdbcTemplate db){this.db=db;}
    public record Decision(int repeatedGrade,String effectiveOn){}

    @PostMapping("/{studentPk}")
    @Transactional
    public Map<String,Object> confirm(@PathVariable long studentPk,@RequestBody Decision decision,Authentication auth){
        if(decision==null||decision.repeatedGrade()<7||decision.repeatedGrade()>13) throw bad("Wiederholter Jahrgang ungültig");
        LocalDate date=parse(decision.effectiveOn());
        int student=db.queryForObject("SELECT COUNT(*) FROM students WHERE id=? AND active=true",Integer.class,studentPk);
        if(student!=1) throw bad("Schüler nicht vorhanden oder archiviert");
        db.update("UPDATE repeat_decisions SET active=false WHERE student_pk=? AND repeated_grade=? AND active=true",studentPk,decision.repeatedGrade());
        db.update("INSERT INTO repeat_decisions(student_pk,repeated_grade,effective_on,decided_by) VALUES(?,?,?,?)",studentPk,decision.repeatedGrade(),date,auth.getName());
        int excluded=db.update("UPDATE coin_grants SET excluded=true,exclusion_reason='UNGUELTIG_WIEDERHOLUNG' WHERE student_pk=? AND student_grade=? AND excluded=false",studentPk,decision.repeatedGrade());
        db.update("UPDATE certificates SET status='UNGUELTIG_WIEDERHOLUNG' WHERE student_pk=? AND status='AKTUELL'",studentPk);
        audit(auth,"REPETITION_CONFIRM",studentPk,"{\"grade\":"+decision.repeatedGrade()+",\"excluded\":"+excluded+"}");
        return Map.of("message","Klassenwiederholung bestätigt","excludedGrants",excluded);
    }

    @DeleteMapping("/{studentPk}/{grade}")
    @Transactional
    public Map<String,Object> revoke(@PathVariable long studentPk,@PathVariable int grade,Authentication auth){
        int changed=db.update("UPDATE repeat_decisions SET active=false WHERE student_pk=? AND repeated_grade=? AND active=true",studentPk,grade);
        int restored=db.update("UPDATE coin_grants SET excluded=false,exclusion_reason=null WHERE student_pk=? AND student_grade=? AND excluded=true AND exclusion_reason='UNGUELTIG_WIEDERHOLUNG'",studentPk,grade);
        db.update("UPDATE certificates SET status='AKTUELL' WHERE student_pk=? AND status='UNGUELTIG_WIEDERHOLUNG'",studentPk);
        audit(auth,"REPETITION_REVOKE",studentPk,"{\"grade\":"+grade+",\"restored\":"+restored+"}");
        return Map.of("message","Klassenwiederholung zurückgenommen","decisions",changed,"restoredGrants",restored);
    }
    private LocalDate parse(String value){try{return LocalDate.parse(value);}catch(Exception ex){throw bad("Datum muss JJJJ-MM-TT sein");}}
    private void audit(Authentication auth,String action,long student,String json){db.update("INSERT INTO audit_events(actor,action,object_type,object_id,before_json,after_json) VALUES(?,?,?, ?,CAST('null' AS JSONB),CAST(? AS JSONB))",auth.getName(),action,"student",Long.toString(student),json);}
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}

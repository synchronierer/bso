package de.igsnanstein.bso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/events")
public class EventMaintenanceController {
    private final JdbcTemplate db;
    EventMaintenanceController(JdbcTemplate db) { this.db=db; }
    public record Correction(String title, String endDate, BigDecimal coins, String status) {}

    @PatchMapping("/{eventId}") @Transactional
    public Map<String,Object> correct(@PathVariable long eventId,@RequestBody Correction c,Authentication auth) {
        Map<String,Object> old=event(eventId);
        if(c.title()==null || c.title().isBlank() || c.endDate()==null || c.coins()==null || !CoinRules.isHalfStep(c.coins()) || c.coins().signum()<=0) throw bad("Ungültige Veranstaltungskorrektur");
        db.update("UPDATE events SET title=?,end_date=CAST(? AS DATE),coins=?,status=COALESCE(?,status),version=version+1 WHERE id=?",c.title().trim(),c.endDate(),c.coins(),c.status(),eventId);
        invalidate(eventId,auth.getName(),"EVENT_CORRECTION",old.toString());
        return Map.of("eventId",eventId,"status","KORRIGIERT","certificatesMarked",true);
    }

    @PatchMapping("/{eventId}/status") @Transactional
    public Map<String,Object> status(@PathVariable long eventId,@RequestParam String value,Authentication auth) {
        Map<String,Object> old=event(eventId);
        if(!java.util.Set.of("AKTIV","ABGESCHLOSSEN","ABGEBROCHEN").contains(value)) throw bad("Unbekannter Veranstaltungsstatus");
        db.update("UPDATE events SET status=?,version=version+1 WHERE id=?",value,eventId);
        invalidate(eventId,auth.getName(),"EVENT_STATUS",old.toString());
        return Map.of("eventId",eventId,"status",value,"certificatesMarked",true);
    }

    private void invalidate(long eventId,String actor,String action,String before) {
        int count=db.update("UPDATE certificates c SET status='PRUEFUNG_ERFORDERLICH' WHERE EXISTS (SELECT 1 FROM certificate_versions v JOIN coin_grants g ON g.student_pk=c.student_pk WHERE v.certificate_id=c.id AND g.event_id=? )",eventId);
        db.update("INSERT INTO audit_events(actor,action,object_type,object_id,before_json,after_json) VALUES (?,?,?, ?,CAST(? AS JSONB),CAST(? AS JSONB))",actor,action,"event",Long.toString(eventId),quote(before),quote("certificatesMarked="+count));
    }
    private Map<String,Object> event(long id) { var r=db.queryForList("SELECT id,title,end_date,coins,status FROM events WHERE id=?",id); if(r.isEmpty()) throw bad("Veranstaltung nicht gefunden"); return r.get(0); }
    private ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,m); }
    private String quote(String v) { return "\""+v.replace("\\","\\\\").replace("\"","\\\"")+"\""; }
}

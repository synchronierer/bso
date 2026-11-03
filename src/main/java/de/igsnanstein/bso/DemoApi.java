package de.igsnanstein.bso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api")
public class DemoApi {
    private final JdbcTemplate db;
    DemoApi(JdbcTemplate db) { this.db = db; }

    public record Student(String studentId, String firstName, String lastName, String className, int grade, String track) {}
    public record Event(String title, String endDate, BigDecimal coins, Boolean confirmHighValue) {}
    public record Grant(long studentPk, long eventId, BigDecimal coins) {}

    @GetMapping("/health")
    public Map<String,String> health() { return Map.of("status","ok", "mode","DEMO – NICHT PRODUKTIV"); }

    @GetMapping("/students")
    public List<Map<String,Object>> students() {
        return db.queryForList("""
          SELECT s.id, s.student_id AS "studentId", s.first_name AS "firstName",
            s.last_name AS "lastName", s.class_name AS "className", s.grade, s.track,
            COALESCE(SUM(CASE WHEN (s.grade <= 10 AND EXTRACT(YEAR FROM e.end_date) < 9999)
               OR s.grade >= 11 THEN g.coins ELSE 0 END),0) AS coins
          FROM students s
          LEFT JOIN coin_grants g ON g.student_pk=s.id
          LEFT JOIN events e ON e.id=g.event_id
          GROUP BY s.id ORDER BY s.class_name,s.last_name,s.first_name
          """);
    }

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> addStudent(@RequestBody Student s) {
        if (blank(s.studentId()) || blank(s.firstName()) || blank(s.lastName()) || blank(s.className())
            || blank(s.track()) || s.grade()<7 || s.grade()>13) throw bad("Schülerdaten unvollständig oder ungültig");
        try {
            db.update("INSERT INTO students(student_id,first_name,last_name,class_name,grade,track) VALUES (?,?,?,?,?,?)",
              s.studentId().trim(),s.firstName().trim(),s.lastName().trim(),s.className().trim(),s.grade(),s.track().trim());
        } catch (DataIntegrityViolationException ex) { throw bad("Schüler-ID bereits vorhanden oder ungültig"); }
        return Map.of("message","Schüler angelegt");
    }

    @GetMapping("/events")
    public List<Map<String,Object>> events() {
        return db.queryForList("SELECT id,title,end_date AS \"endDate\",coins,kind FROM events ORDER BY end_date DESC,id DESC");
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> addEvent(@RequestBody Event e, Authentication auth) {
        CoinRules.requireCoinValue(e.coins(), false);
        if(blank(e.title()) || blank(e.endDate()))
            throw bad("Name, Datum und positiver Münzwert in 0,5er-Schritten erforderlich");
        if(e.coins().compareTo(new BigDecimal("20"))>0 && !Boolean.TRUE.equals(e.confirmHighValue()))
            throw bad("Münzwert über 20 benötigt eine ausdrückliche Bestätigung");
        try {
            db.update("INSERT INTO events(title,end_date,coins,kind) VALUES (?,CAST(? AS DATE),?,'WAHLPFLICHT')",
               e.title().trim(),e.endDate(),e.coins());
            audit(auth,"EVENT_CREATE", "event", e.title(), null, e.title()+"/"+e.coins());
        } catch (Exception ex) { throw bad("Ungültiges Datum oder Veranstaltung bereits vorhanden"); }
        return Map.of("message","Veranstaltung angelegt");
    }

    @PostMapping("/grants")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> grant(@RequestBody Grant g, Authentication auth) {
        if(!CoinRules.isHalfStep(g.coins())) throw bad("Ungültiger Münzwert");
        List<Map<String,Object>> events=db.queryForList("SELECT coins,end_date,status FROM events WHERE id=?",g.eventId());
        if(events.isEmpty()) throw bad("Veranstaltung nicht gefunden");
        BigDecimal standard=(BigDecimal)events.get(0).get("coins");
        LocalDate end=((java.sql.Date)events.get(0).get("end_date")).toLocalDate();
        if(end.isAfter(LocalDate.now())) throw bad("Buchung erst nach dem Veranstaltungsende möglich");
        if(!"AKTIV".equals(events.get(0).get("status"))) throw bad("Veranstaltung ist beendet oder abgebrochen");
        List<BigDecimal> values=List.of(standard);
        if(g.coins().compareTo(values.get(0))>0) throw bad("Individueller Wert darf Standard nicht übersteigen");
        Integer equivalent = db.queryForObject("""
            SELECT COUNT(*) FROM coin_grants old_grant
            JOIN events old_event ON old_event.id=old_grant.event_id
            JOIN event_equivalences old_eq ON old_eq.event_id=old_event.id
            JOIN event_equivalences new_eq ON new_eq.group_id=old_eq.group_id
            WHERE old_grant.student_pk=? AND new_eq.event_id=?
              AND EXTRACT(YEAR FROM old_event.end_date)=EXTRACT(YEAR FROM CAST(? AS DATE))
            """, Integer.class, g.studentPk(), g.eventId(), end);
        if (equivalent != null && equivalent > 0)
            throw bad("Eine gleichartige Wahlpflichtgruppe wurde in diesem Schuljahr bereits gewertet");
        try {
            int changed=db.update("INSERT INTO coin_grants(student_pk,event_id,coins,recorded_by) VALUES (?,?,?,?)",g.studentPk(),g.eventId(),g.coins(),auth.getName());
            if(changed!=1) throw bad("Buchung fehlgeschlagen");
            audit(auth,"COIN_GRANT", "student", Long.toString(g.studentPk()), null, g.coins().toPlainString());
        } catch(DataIntegrityViolationException ex) { throw bad("Schüler nicht vorhanden oder Veranstaltung schon verbucht"); }
        return Map.of("message","Münzen verbucht");
    }

    private boolean validCoins(BigDecimal c) {
        return c!=null && c.remainder(new BigDecimal("0.5")).compareTo(BigDecimal.ZERO)==0;
    }
    private boolean blank(String s) { return s==null || s.isBlank(); }
    private ResponseStatusException bad(String s) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,s); }

    private void audit(Authentication auth, String action, String type, String id, String before, String after) {
        db.update("INSERT INTO audit_events(actor,action,object_type,object_id,before_json,after_json) VALUES (?,?,?,?,CAST(? AS JSONB),CAST(? AS JSONB))",
            auth.getName(), action, type, id, before == null ? "null" : quote(before), quote(after));
    }
    private String quote(String value) { return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }
}

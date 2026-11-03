package de.igsnanstein.bso;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api")
public class DemoApi {
    private final JdbcTemplate db;
    DemoApi(JdbcTemplate db) { this.db = db; }

    public record Student(String studentId, String firstName, String lastName, String className, int grade, String track) {}
    public record Event(String title, String endDate, BigDecimal coins) {}
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
    public Map<String,String> addEvent(@RequestBody Event e) {
        if(blank(e.title()) || blank(e.endDate()) || !validCoins(e.coins()) || e.coins().compareTo(BigDecimal.ZERO)<=0)
            throw bad("Name, Datum und positiver Münzwert in 0,5er-Schritten erforderlich");
        if(e.coins().compareTo(new BigDecimal("20"))>0) throw bad("Werte über 20 Münzen sind in dieser Demo gesperrt");
        try {
            db.update("INSERT INTO events(title,end_date,coins,kind) VALUES (?,CAST(? AS DATE),?,'WAHLPFLICHT')",
               e.title().trim(),e.endDate(),e.coins());
        } catch (Exception ex) { throw bad("Ungültiges Datum"); }
        return Map.of("message","Veranstaltung angelegt");
    }

    @PostMapping("/grants")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> grant(@RequestBody Grant g) {
        if(!validCoins(g.coins()) || g.coins().compareTo(BigDecimal.ZERO)<0) throw bad("Ungültiger Münzwert");
        List<BigDecimal> values=db.query("SELECT coins FROM events WHERE id=?", (rs,n)->rs.getBigDecimal(1),g.eventId());
        if(values.isEmpty()) throw bad("Veranstaltung nicht gefunden");
        if(g.coins().compareTo(values.get(0))>0) throw bad("Individueller Wert darf Standard nicht übersteigen");
        try {
            int changed=db.update("INSERT INTO coin_grants(student_pk,event_id,coins) VALUES (?,?,?)",g.studentPk(),g.eventId(),g.coins());
            if(changed!=1) throw bad("Buchung fehlgeschlagen");
        } catch(DataIntegrityViolationException ex) { throw bad("Schüler nicht vorhanden oder Veranstaltung schon verbucht"); }
        return Map.of("message","Münzen verbucht");
    }

    private boolean validCoins(BigDecimal c) {
        return c!=null && c.remainder(new BigDecimal("0.5")).compareTo(BigDecimal.ZERO)==0;
    }
    private boolean blank(String s) { return s==null || s.isBlank(); }
    private ResponseStatusException bad(String s) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,s); }
}

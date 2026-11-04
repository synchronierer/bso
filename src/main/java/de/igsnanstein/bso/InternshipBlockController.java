package de.igsnanstein.bso;

import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Pre-booked internship placements do not earn coins until explicitly finalised. */
@RestController
@RequestMapping("/api/internship-blocks")
public class InternshipBlockController {
    private final JdbcTemplate db;
    private final InternshipController internships;
    InternshipBlockController(JdbcTemplate db, InternshipController internships) {
        this.db=db; this.internships=internships;
    }

    record NewBlock(String title,String kind,String startsOn,String endsOn){}
    record Placement(long studentPk,String company){}
    record SavePlacements(List<Placement> entries){}
    record FinaliseRow(long placementId,int recognizedDays){}
    record Finalise(List<FinaliseRow> entries){}

    @GetMapping
    public List<Map<String,Object>> blocks() {
        return db.queryForList("""
            SELECT b.id,b.title,b.kind,
              b.starts_on AS "startsOn",b.ends_on AS "endsOn",
              COUNT(p.id) AS "placements",
              COUNT(p.internship_id) AS "finalised"
            FROM internship_blocks b LEFT JOIN internship_placements p ON p.block_id=b.id
            GROUP BY b.id ORDER BY b.starts_on DESC,b.id DESC
            """);
    }

    @PostMapping
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> create(@RequestBody NewBlock r,Authentication auth) {
        if(r==null||blank(r.title())||r.title().length()>200)
            throw bad("Bezeichnung ist erforderlich");
        String kind=r.kind()==null?"":r.kind().trim().toUpperCase(Locale.ROOT);
        if(!Set.of("PFLICHT","ZUSATZ").contains(kind))throw bad("Praktikumsart ungültig");
        LocalDate start=parse(r.startsOn()),end=parse(r.endsOn());
        if(end.isBefore(start))throw bad("Das Enddatum darf nicht vor dem Anfang liegen");
        Long id=db.queryForObject("""
            INSERT INTO internship_blocks(title,kind,starts_on,ends_on,created_by)
            VALUES(?,?,?,?,?) RETURNING id
            """,Long.class,r.title().trim(),kind,start,end,auth.getName());
        audit(auth,"INTERNSHIP_BLOCK_CREATE",String.valueOf(id));
        return Map.of("id",id,"message","Praktikumsblock gespeichert – noch keine Münzen vergeben");
    }

    @GetMapping("/{id}/placements")
    public List<Map<String,Object>> placements(@PathVariable long id,@RequestParam String className) {
        block(id);
        if(blank(className)||className.length()>50)throw bad("Bitte eine Klasse als Filter wählen");
        return db.queryForList("""
            SELECT s.id AS "studentPk",s.first_name AS "firstName",s.last_name AS "lastName",
              s.student_id AS "studentId",p.id AS "placementId",p.company,
              p.recognized_days AS "recognizedDays",p.internship_id AS "internshipId"
            FROM students s
            LEFT JOIN internship_placements p ON p.student_pk=s.id AND p.block_id=?
            WHERE s.class_name=? AND s.active=true
            ORDER BY s.last_name,s.first_name,s.id
            """,id,className.trim());
    }

    @PostMapping("/{id}/placements")
    @Transactional
    public Map<String,Object> save(@PathVariable long id,@RequestBody SavePlacements request,Authentication auth) {
        block(id);
        db.queryForObject("SELECT id FROM internship_blocks WHERE id=? FOR UPDATE",Long.class,id);
        if(request==null||request.entries()==null||request.entries().isEmpty()||request.entries().size()>300)
            throw bad("Bitte mindestens einen Schüler mit Betrieb eintragen");
        Set<Long> seen=new HashSet<>();
        int count=0;
        for(Placement item:request.entries()){
            if(item==null||item.studentPk()<=0||!seen.add(item.studentPk())||blank(item.company())||item.company().length()>200)
                throw bad("Schüler oder Betrieb ungültig oder doppelt");
            Map<String,Object> s=db.query("SELECT s.class_name,ep.id AS episode_id FROM students s JOIN enrollment_episodes ep ON ep.student_pk=s.id AND ep.ends_on IS NULL WHERE s.id=? AND s.active=true",
                rs->rs.next()?Map.of("class_name",rs.getString(1),"episode_id",rs.getLong(2)):null,item.studentPk());
            if(s==null)
                throw bad("Schüler ist nicht aktiv oder hat keine offene Schulbesuchsepisode");
            Integer existing=db.query("SELECT COUNT(*) FROM internship_placements WHERE block_id=? AND student_pk=? AND internship_id IS NOT NULL",
                rs->rs.next()?rs.getInt(1):0,id,item.studentPk());
            if(existing!=null&&existing>0)throw bad("Bereits endgültig angerechnete Einträge können nicht verändert werden");
            db.update("""
                INSERT INTO internship_placements(block_id,student_pk,episode_id,company)
                VALUES(?,?,?,?)
                ON CONFLICT(block_id,student_pk) DO UPDATE SET company=EXCLUDED.company,
                  updated_at=now()
                WHERE internship_placements.internship_id IS NULL
                """,id,item.studentPk(),s.get("episode_id"),item.company().trim());
            count++;
        }
        audit(auth,"INTERNSHIP_PLACEMENTS_SAVE",id+"/"+count);
        return Map.of("message","Betriebe gespeichert – noch keine Münzen vergeben","changed",count);
    }

    @PostMapping("/{id}/finalise")
    @Transactional
    public Map<String,Object> finalise(@PathVariable long id,@RequestBody Finalise request,Authentication auth) {
        Map<String,Object> block=block(id);
        LocalDate end=((java.sql.Date)block.get("ends_on")).toLocalDate();
        if(end.isAfter(LocalDate.now()))throw bad("Münzvergabe erst nach dem Abschlussdatum des Praktikumsblocks");
        if(request==null||request.entries()==null||request.entries().isEmpty()||request.entries().size()>300)
            throw bad("Bitte Schülerinnen und Schüler für die endgültige Münzvergabe auswählen");
        // Block lock serialises confirmation requests for the same block.
        db.queryForObject("SELECT id FROM internship_blocks WHERE id=? FOR UPDATE",Long.class,id);
        Set<Long> seen=new HashSet<>();
        int count=0,coins=0;
        for(FinaliseRow item:request.entries()){
            if(item==null||item.placementId()<=0||!seen.add(item.placementId()))
                throw bad("Praktikumseintrag ungültig oder doppelt");
            Map<String,Object> placement=db.query("""
                SELECT p.student_pk,p.company,p.episode_id,p.internship_id,s.active,s.class_name
                FROM internship_placements p JOIN students s ON s.id=p.student_pk
                WHERE p.id=? AND p.block_id=?
                """,rs->rs.next()?Map.of("studentPk",rs.getLong(1),"company",rs.getString(2),
                    "episodeId",rs.getLong(3),"internshipId",rs.getObject(4)==null?0L:rs.getLong(4),
                    "active",rs.getBoolean(5),"className",rs.getString(6)):null,item.placementId(),id);
            if(placement==null||((Long)placement.get("internshipId"))!=0L)
                throw bad("Praktikum fehlt oder wurde bereits endgültig abgerechnet");
            if(!(Boolean)placement.get("active"))
                throw bad("Schüler ist nicht mehr aktiv");
            Long currentEpisode=db.query("SELECT id FROM enrollment_episodes WHERE student_pk=? AND ends_on IS NULL",
                rs->rs.next()?rs.getLong(1):null,placement.get("studentPk"));
            if(!Objects.equals(currentEpisode,placement.get("episodeId")))
                throw bad("Schulbesuchsepisode hat sich geändert – Eintrag bitte neu prüfen");
            int max="PFLICHT".equals(block.get("kind"))?10:5;
            if(item.recognizedDays()<1||item.recognizedDays()>max)
                throw bad("Anerkannte Tage müssen zwischen 1 und "+max+" liegen");
            internships.create(new InternshipController.Request(
                (Long)placement.get("studentPk"),(String)block.get("kind"),(String)placement.get("company"),
                ((java.sql.Date)block.get("starts_on")).toLocalDate().toString(),end.toString(),
                item.recognizedDays(),true),auth);
            Long internshipId=db.queryForObject("""
                SELECT id FROM internships WHERE student_pk=? AND episode_id=? AND company=? AND starts_on=? AND ends_on=?
                AND kind=? AND finalised=true ORDER BY id DESC LIMIT 1
                """,Long.class,placement.get("studentPk"),currentEpisode,placement.get("company"),block.get("starts_on"),end,block.get("kind"));
            db.update("UPDATE internship_placements SET recognized_days=?,internship_id=?,updated_at=now() WHERE id=? AND internship_id IS NULL",
                item.recognizedDays(),internshipId,item.placementId());
            count++;coins+=item.recognizedDays()*10;
        }
        audit(auth,"INTERNSHIP_BLOCK_FINALISE",id+"/"+count+"/"+coins);
        return Map.of("message","Praktika endgültig angerechnet","count",count,"coins",coins);
    }

    private Map<String,Object> block(long id){
        return db.query("SELECT id,title,kind,starts_on,ends_on FROM internship_blocks WHERE id=?",
            rs->{if(!rs.next())throw bad("Praktikumsblock nicht gefunden");
                Map<String,Object> m=new HashMap<>();
                m.put("id",rs.getLong(1));m.put("title",rs.getString(2));
                m.put("kind",rs.getString(3));m.put("starts_on",rs.getDate(4));m.put("ends_on",rs.getDate(5));
                return m;},id);
    }
    private boolean blank(String s){return s==null||s.isBlank();}
    private LocalDate parse(String value){try{return LocalDate.parse(value);}catch(Exception ex){throw bad("Datum ungültig (JJJJ-MM-TT)");}}
    private void audit(Authentication auth,String action,String value){
        db.update("INSERT INTO audit_events(actor,action,object_type,object_id,before_json,after_json) VALUES (? ,? ,'internship_block',?,CAST('null' AS JSONB),to_jsonb(CAST(? AS TEXT)))",
            auth.getName(),action,value,value);
    }
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}

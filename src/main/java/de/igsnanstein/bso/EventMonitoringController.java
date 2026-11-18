package de.igsnanstein.bso;

import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/** Auditable fulfillment of the published obligations for grade and track. */
@RestController
@RequestMapping("/api/monitoring")
public class EventMonitoringController {
    private final JdbcTemplate db;
    EventMonitoringController(JdbcTemplate db){this.db=db;}

    @GetMapping("/{studentPk}")
    public Map<String,Object> student(@PathVariable long studentPk){
        Map<String,Object> pupil=db.queryForMap(
            "SELECT grade,track FROM students WHERE id=? AND active=true",studentPk);
        int grade=((Number)pupil.get("grade")).intValue();
        String track=String.valueOf(pupil.get("track"));
        Map<String,Object> year=db.queryForMap("""
            SELECT id,starts_on,ends_on FROM school_years
            WHERE CURRENT_DATE BETWEEN starts_on AND ends_on ORDER BY starts_on DESC LIMIT 1
            """);
        List<Map<String,Object>> requirements=db.queryForList("""
            SELECT id,title,kind,minimum_count,target_coins AS "targetCoins",
                   equivalence_group_id AS "equivalenceGroupId"
            FROM event_requirements
            WHERE school_year_id=? AND grade=? AND (track IS NULL OR UPPER(track)=UPPER(?))
            ORDER BY id
            """,year.get("id"),grade,track);
        LocalDate from=((java.sql.Date)year.get("starts_on")).toLocalDate();
        LocalDate to=((java.sql.Date)year.get("ends_on")).toLocalDate();
        long missing=0;
        for(Map<String,Object> requirement:requirements){
            int achieved;
            String kind=String.valueOf(requirement.get("kind"));
            if("PRAKTIKUM".equals(kind)){
                achieved=db.queryForObject("""
                    SELECT COUNT(*) FROM internships i
                    JOIN enrollment_episodes ep ON ep.id=i.episode_id
                    WHERE i.student_pk=? AND i.kind='PFLICHT' AND i.finalised=true
                      AND ep.student_pk=? AND i.ends_on BETWEEN ? AND ?
                    """,Integer.class,studentPk,studentPk,from,to);
            }else if("WORKSHOP".equals(kind)){
                achieved=db.queryForObject("""
                    SELECT COUNT(DISTINCT e.title) FROM coin_grants g
                    JOIN events e ON e.id=g.event_id
                    JOIN enrollment_episodes ep ON ep.id=g.episode_id
                    WHERE ep.student_pk=? AND g.student_pk=? AND g.excluded=false
                      AND e.status<>'ABGEBROCHEN' AND e.kind='WORKSHOP'
                      AND e.end_date BETWEEN ? AND ?
                    """,Integer.class,studentPk,studentPk,from,to);
            }else{
                Object group=requirement.get("equivalenceGroupId");
                if(group!=null){
                    achieved=db.queryForObject("""
                        SELECT COUNT(DISTINCT e.id) FROM coin_grants g
                        JOIN events e ON e.id=g.event_id
                        JOIN event_equivalences eq ON eq.event_id=e.id
                        JOIN enrollment_episodes ep ON ep.id=g.episode_id
                        WHERE ep.student_pk=? AND g.student_pk=? AND eq.group_id=?
                          AND g.excluded=false AND e.status<>'ABGEBROCHEN'
                          AND e.end_date BETWEEN ? AND ?
                        """,Integer.class,studentPk,studentPk,group,from,to);
                }else{
                    achieved=db.queryForObject("""
                        SELECT COUNT(DISTINCT e.id) FROM coin_grants g
                        JOIN events e ON e.id=g.event_id
                        JOIN enrollment_episodes ep ON ep.id=g.episode_id
                        WHERE ep.student_pk=? AND g.student_pk=? AND e.title=?
                          AND g.excluded=false AND e.status<>'ABGEBROCHEN'
                          AND e.end_date BETWEEN ? AND ?
                        """,Integer.class,studentPk,studentPk,requirement.get("title"),from,to);
                }
            }
            int required=((Number)requirement.get("minimum_count")).intValue();
            requirement.put("achieved",achieved);
            if(achieved<required)missing++;
        }
        return Map.of("studentPk",studentPk,"requirements",requirements,
            "missing",missing,"complete",missing==0);
    }
}

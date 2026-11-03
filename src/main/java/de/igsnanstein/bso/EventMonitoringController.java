package de.igsnanstein.bso;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/monitoring")
public class EventMonitoringController {
    private final JdbcTemplate db;
    EventMonitoringController(JdbcTemplate db) { this.db=db; }
    @GetMapping("/{studentPk}")
    public Map<String,Object> student(@PathVariable long studentPk) {
        Map<String,Object> s=db.queryForMap("SELECT grade,track FROM students WHERE id=? AND active=true",studentPk);
        List<Map<String,Object>> requirements=db.queryForList("SELECT r.id,r.title,r.kind,r.minimum_count,r.equivalence_group_id AS \"equivalenceGroupId\",COALESCE(x.count,0) AS achieved FROM event_requirements r LEFT JOIN (SELECT r2.id,COUNT(DISTINCT g.event_id) count FROM event_requirements r2 JOIN events e ON ((r2.equivalence_group_id IS NOT NULL AND EXISTS (SELECT 1 FROM event_equivalences ee WHERE ee.event_id=e.id AND ee.group_id=r2.equivalence_group_id)) OR (r2.equivalence_group_id IS NULL AND e.title=r2.title)) JOIN coin_grants g ON g.event_id=e.id AND g.student_pk=? JOIN enrollment_episodes ep ON ep.id=g.episode_id AND ep.ends_on IS NULL WHERE r2.grade=? AND (r2.track IS NULL OR r2.track=?) GROUP BY r2.id) x ON x.id=r.id WHERE r.school_year_id=(SELECT id FROM school_years ORDER BY starts_on DESC LIMIT 1) AND r.grade=? AND (r.track IS NULL OR r.track=?) ORDER BY r.id",studentPk,s.get("grade"),s.get("track"),s.get("grade"),s.get("track"));
        long missing=requirements.stream().filter(r->((Number)r.get("achieved")).intValue()<((Number)r.get("minimum_count")).intValue()).count();
        return Map.of("studentPk",studentPk,"requirements",requirements,"missing",missing,"complete",missing==0);
    }
}

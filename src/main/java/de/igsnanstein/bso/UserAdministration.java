package de.igsnanstein.bso;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

@RestController
@RequestMapping("/api")
public class UserAdministration {
    private final JdbcTemplate db;
    private final PasswordEncoder passwords;
    private static final Pattern NAME=Pattern.compile("[a-zA-Z0-9._-]{3,64}");

    UserAdministration(JdbcTemplate db, PasswordEncoder passwords) {
        this.db=db;this.passwords=passwords;
    }

    @Bean ApplicationRunner initializeAdmin(JdbcTemplate db, PasswordEncoder passwords,
            @Value("${BSO_ADMIN_PASSWORD:}") String adminPassword) {
        return args -> {
            Integer users=db.queryForObject("SELECT COUNT(*) FROM app_users",Integer.class);
            if(users!=null && users==0) {
                if(adminPassword.length()<12)
                    throw new IllegalStateException("Erstinstallation: ADMIN_INITIAL_PASSWORD muss mindestens 12 Zeichen lang sein.");
                db.update("INSERT INTO app_users(username,password_hash,role,enabled) VALUES ('admin',?,'ADMIN',true)",
                          passwords.encode(adminPassword));
            }
        };
    }

    public record NewUser(String username,String password,String role) {}
    public record NewPassword(String currentPassword,String newPassword) {}
    public record ResetPassword(String newPassword) {}
    public record Enabled(boolean enabled) {}

    @GetMapping("/me")
    public Map<String,Object> me(Authentication auth) {
        return Map.of("username",auth.getName(),
                "admin",auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN")));
    }

    @GetMapping("/csrf")
    public Map<String,String> csrf(org.springframework.security.web.csrf.CsrfToken token) {
        return Map.of("token",token.getToken(),"headerName",token.getHeaderName());
    }

    @PostMapping("/password")
    public Map<String,String> password(@RequestBody NewPassword request, Authentication auth) {
        if(request.currentPassword()==null || request.newPassword()==null
                || request.newPassword().length()<12 || request.newPassword().length()>200)
            throw bad("Neues Passwort muss 12 bis 200 Zeichen lang sein");
        String hash=db.queryForObject("SELECT password_hash FROM app_users WHERE username=?",
                String.class,auth.getName());
        if(!passwords.matches(request.currentPassword(),hash)) throw bad("Aktuelles Passwort falsch");
        db.update("UPDATE app_users SET password_hash=? WHERE username=?",
                passwords.encode(request.newPassword()),auth.getName());
        db.update("INSERT INTO user_audit(actor,action,target) VALUES (?, 'PASSWORD_CHANGE', ?)",
                auth.getName(),auth.getName());
        return Map.of("message","Passwort geändert");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public List<Map<String,Object>> users() {
        return db.queryForList("SELECT id,username,role,enabled,created_at AS \"createdAt\" FROM app_users ORDER BY username");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/users")
    public Map<String,String> create(@RequestBody NewUser request,Authentication auth) {
        if(request.username()==null || !NAME.matcher(request.username()).matches()
                || request.password()==null || request.password().length()<12 || request.password().length()>200
                || !("ADMIN".equals(request.role()) || "TEACHER".equals(request.role())))
            throw bad("Benutzername 3–64 Zeichen, Passwort mindestens 12 Zeichen, Rolle ADMIN oder TEACHER");
        try {
            db.update("INSERT INTO app_users(username,password_hash,role,enabled) VALUES (?,?,?,true)",
                    request.username(),passwords.encode(request.password()),request.role());
        } catch(DataIntegrityViolationException ex){throw bad("Benutzername bereits vorhanden");}
        db.update("INSERT INTO user_audit(actor,action,target) VALUES (?,'USER_CREATE',?)",
                auth.getName(),request.username());
        return Map.of("message","Benutzer erstellt");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/users/{id}/password")
    public Map<String,String> reset(@PathVariable long id,@RequestBody ResetPassword request,
                                    Authentication auth) {
        if(request.newPassword()==null || request.newPassword().length()<12 || request.newPassword().length()>200)
            throw bad("Passwort muss mindestens 12 Zeichen haben");
        int n=db.update("UPDATE app_users SET password_hash=? WHERE id=?",
                passwords.encode(request.newPassword()),id);
        if(n==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        db.update("INSERT INTO user_audit(actor,action,target) VALUES (?,'PASSWORD_RESET',?)",
                auth.getName(),Long.toString(id));
        return Map.of("message","Passwort zurückgesetzt");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/users/{id}/enabled")
    public Map<String,String> enabled(@PathVariable long id,@RequestBody Enabled request,
                                      Authentication auth) {
        long ownId=db.queryForObject("SELECT id FROM app_users WHERE username=?",Long.class,auth.getName());
        if(ownId==id && !request.enabled()) throw bad("Eigenes Administrationskonto kann nicht deaktiviert werden");
        int n=db.update("UPDATE app_users SET enabled=? WHERE id=?",request.enabled(),id);
        if(n==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        db.update("INSERT INTO user_audit(actor,action,target) VALUES (?,?,?)",
                auth.getName(),request.enabled()?"USER_ENABLED":"USER_DISABLED",Long.toString(id));
        return Map.of("message","Kontostatus geändert");
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);
    }
}
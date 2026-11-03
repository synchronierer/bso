package de.igsnanstein.bso;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean UserDetailsService userDetailsService(JdbcTemplate db) {
        return username -> db.query("""
                SELECT username,password_hash,role,enabled FROM app_users WHERE username=?
                """, rs -> {
                    if(!rs.next()) throw new UsernameNotFoundException("Unbekannter Benutzer");
                    return User.withUsername(rs.getString("username"))
                            .password(rs.getString("password_hash"))
                            .roles(rs.getString("role"))
                            .disabled(!rs.getBoolean("enabled"))
                            .build();
                }, username);
    }

    @Bean SecurityFilterChain security(HttpSecurity http, JdbcTemplate db, UserDetailsService uds,
                                       PasswordEncoder encoder) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(uds);
        provider.setPasswordEncoder(encoder);
        http.authenticationProvider(provider);
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/login.html","/api/csrf","/api/health","/favicon.ico","/error").permitAll()
                .requestMatchers("/admin.html","/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated());
        http.formLogin(form -> form.loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/",true)
                .failureHandler((req,res,ex)-> {
                    db.update("INSERT INTO login_attempts(username,client_ip,successful) VALUES (?,?,false)",
                            req.getParameter("username"),req.getRemoteAddr());
                    res.sendRedirect("/login.html?error");
                })
                .successHandler((req,res,auth)-> {
                    req.getSession().setMaxInactiveInterval(1800);
                    db.update("INSERT INTO login_attempts(username,client_ip,successful) VALUES (?,?,true)",
                            auth.getName(),req.getRemoteAddr());
                    res.sendRedirect("/");
                }));
        http.logout(logout -> logout.logoutUrl("/logout")
                .invalidateHttpSession(true).clearAuthentication(true).logoutSuccessUrl("/login.html?logout"));
        http.sessionManagement(session -> session.invalidSessionUrl("/login.html?expired")
                .sessionFixation(fixation -> fixation.migrateSession())
                .maximumSessions(1).expiredUrl("/login.html?expired"));
        http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
        http.addFilterBefore(new LoginRateLimitFilter(), UsernamePasswordAuthenticationFilter.class);
        http.headers(headers -> headers.contentSecurityPolicy(csp ->
                csp.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; object-src 'none'; frame-ancestors 'none'; base-uri 'self'")));
        return http.build();
    }

    /** IP-basiert: max. 20 Loginversuche/Minute, ohne automatische Kontosperre. */
    static final class LoginRateLimitFilter extends OncePerRequestFilter {
        private final ConcurrentHashMap<String,Window> attempts = new ConcurrentHashMap<>();
        record Window(long minute, AtomicInteger count) {}
        @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                FilterChain chain) throws ServletException, IOException {
            if("POST".equals(req.getMethod()) && "/login".equals(req.getServletPath())) {
                long minute=System.currentTimeMillis()/60_000;
                String key=req.getRemoteAddr();
                Window w=attempts.compute(key,(k,old)-> old==null || old.minute()!=minute
                        ? new Window(minute,new AtomicInteger(1))
                        : new Window(minute,new AtomicInteger(old.count().incrementAndGet())));
                if(w.count().get()>20) {res.sendError(429,"Zu viele Anmeldeversuche");return;}
                if(attempts.size()>5000) attempts.entrySet().removeIf(e->e.getValue().minute()<minute-2);
            }
            chain.doFilter(req,res);
        }
    }
}
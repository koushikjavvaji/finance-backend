package com.financeapp.config;

import com.financeapp.security.JwtAuthFilter;
import com.financeapp.security.RateLimitingFilter;
import com.financeapp.security.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Optional;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter          jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * RateLimitingFilter is only registered in non-test profiles (@Profile("!test")).
     * Using Optional injection so SecurityConfig compiles and runs fine in the test profile
     * where the bean does not exist.
     */
    @Autowired(required = false)
    private RateLimitingFilter rateLimitingFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        HttpSecurity security = http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // Swagger UI (public)
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // Read transactions: VIEWER, ANALYST, ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/transactions/**")
                            .hasAnyRole("VIEWER", "ANALYST", "ADMIN")
                        // Dashboard: ANALYST, ADMIN
                        .requestMatchers("/api/dashboard/**")
                            .hasAnyRole("ANALYST", "ADMIN")
                        // Write transactions + user management: ADMIN only
                        .requestMatchers("/api/transactions/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // Return 401 (not 403) for unauthenticated requests
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                )
                // Allow H2 console iframes
                .headers(h -> h.frameOptions(f -> f.disable()));

        // Only add rate limiter when the bean is present (i.e. not in test profile)
        if (rateLimitingFilter != null) {
            security.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        }

        security.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return security.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

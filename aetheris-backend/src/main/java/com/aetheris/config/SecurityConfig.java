package com.aetheris.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Value("${aetheris.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Por defecto Spring Security fuerza "Cache-Control: no-cache, no-store,
            // max-age=0, must-revalidate" en TODAS las respuestas, sobreescribiendo
            // cualquier Cache-Control que pongamos manualmente en un controller.
            // Esto rompe el streaming SSE de /aria/chat: Railway (y otros proxies/CDN)
            // solo evitan bufferizar/comprimir una respuesta en streaming si ven
            // "Cache-Control: no-transform" — que el header por defecto de Spring
            // Security estaba borrando. Se desactiva el writer automático y cada
            // controller pone el Cache-Control que necesite (AriaController ya
            // pone "no-cache, no-transform" explícitamente).
            .headers(headers -> headers.cacheControl(cache -> cache.disable()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // setAllowedOriginPatterns soporta wildcards (e.g. https://*.replit.dev)
        // Se incluyen los orígenes configurados + Replit (dev y producción)
        List<String> patterns = new java.util.ArrayList<>(
            Arrays.asList(allowedOrigins.split(","))
        );
        patterns.add("https://*.replit.dev");
        patterns.add("https://*.repl.co");
        cfg.setAllowedOriginPatterns(patterns);

        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}

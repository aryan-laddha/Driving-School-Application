    package com.example.drivingschool.security;

    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.http.HttpMethod;
    import org.springframework.security.authentication.AuthenticationManager;
    import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
    import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
    import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
    @EnableMethodSecurity
    public class SecurityConfig {

        private final CustomUserDetailsService userDetailsService;
        private final JwtAuthFilter jwtAuthFilter;
        private final JwtAuthenticationEntryPoint authenticationEntryPoint;
        private final CustomAccessDeniedHandler accessDeniedHandler;

        public SecurityConfig(CustomUserDetailsService userDetailsService,
                              JwtAuthFilter jwtAuthFilter,
                              JwtAuthenticationEntryPoint authenticationEntryPoint,
                              CustomAccessDeniedHandler accessDeniedHandler) {
            this.userDetailsService = userDetailsService;
            this.jwtAuthFilter = jwtAuthFilter;
            this.authenticationEntryPoint = authenticationEntryPoint;
            this.accessDeniedHandler = accessDeniedHandler;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(HttpMethod.POST, "/api/queries/submit").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/courses").permitAll()
                            .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                            .anyRequest().authenticated()
                    )
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint(authenticationEntryPoint)
                            .accessDeniedHandler(accessDeniedHandler)
                    )
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

            return http.build();
        }
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration configuration = new CorsConfiguration();

            // Allowed origins (your React dev server)
            configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174", "https://welcomedrivingschool.netlify.app"));

            // Allowed methods (must include OPTIONS for preflight)
            configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT","PATCH", "DELETE", "OPTIONS"));

            // Allowed headers (crucial for Authorization header used by JWT)
            configuration.setAllowedHeaders(List.of("*"));

            // Allow credentials (necessary if you are using cookies/sessions, but generally safe to include)
            configuration.setAllowCredentials(true);

            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            // Apply this configuration to all endpoints
            source.registerCorsConfiguration("/**", configuration);
            return source;
        }
        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
            AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
            authBuilder.userDetailsService(userDetailsService)
                    .passwordEncoder(passwordEncoder());

            return authBuilder.build(); // no .and() needed
        }

    }

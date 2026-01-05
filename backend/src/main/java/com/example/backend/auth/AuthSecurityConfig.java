package com.example.backend.auth;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AuthSecurityConfig {
    private final CustomUserDetailsService uds;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwt;
    ///
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> req
                        // 1. 프리플라이트 요청 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 2. 공개용 엔드포인트
                        .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/auth/**","/api/files/**").permitAll()
                        .requestMatchers("/api/users/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/videos/**").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/videos/*/view").permitAll()

                        // 3. 영상 업로드는 로그인 필요
                        .requestMatchers("/api/videos/upload").authenticated()

                        // 4. 나머지는 전부 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthFilter(jwt, uds), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }





    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration cfg = new CorsConfiguration();

        //프론트 개발 서버 오리진 등록
        cfg.setAllowedOrigins(List.of("http://localhost:5173","https://vidsparkkr.com"));
        //인증정보 (쿠키 Authorization 헤더) 쓰면 true;
        cfg.setAllowCredentials(true);

        //허용 메서드/헤더
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Content-Type","Authorization","X-Requested-With","X-Refresh-Token"));
        cfg.setExposedHeaders(List.of("Authorization","Location"));

        // prefLight 캐시 (초)
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**",cfg);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(){
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);

    }


    static class JwtAuthFilter extends OncePerRequestFilter{
        private final JwtUtil jwt;
        private final CustomUserDetailsService uds;
        JwtAuthFilter(JwtUtil jwt,  CustomUserDetailsService uds){this.jwt=jwt; this.uds=uds;}

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {

            String auth = req.getHeader("Authorization");
            if(auth != null && auth.startsWith("Bearer")){
                String token = auth.substring(7);
                if(jwt.isValid(token)){
                    String email = jwt.getEmail(token);
                    UserDetails ud = uds.loadUserByUsername(email);
                    var authToken = new UsernamePasswordAuthenticationToken(
                            ud,null,ud.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
            chain.doFilter(req,res);
        }
    }

}

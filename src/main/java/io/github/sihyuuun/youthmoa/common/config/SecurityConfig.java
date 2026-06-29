package io.github.sihyuuun.youthmoa.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 인증 페이지는 우선 permit (Spring Security 7 매처 동작 이슈 회피용 단독 명시)
                        .requestMatchers("/login", "/signup").permitAll()
                        // 인증 필요 (먼저 매칭되어 permitAll 보다 우선)
                        .requestMatchers("/programs/*/apply", "/bookmarks/**").authenticated()
                        // 그 외 비인증 허용
                        .requestMatchers("/", "/api/ping",
                                "/programs", "/programs/**",
                                "/css/**", "/js/**", "/images/**", "/webjars/**",
                                "/favicon.ico")
                        .permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

package io.github.sihyuuun.youthmoa.common.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
public class SecurityConfig {

  /** 로그인 실패 시 username 을 세션에 보존해 로그인 폼 재표시 시 채워둠. */
  private static SimpleUrlAuthenticationFailureHandler loginFailureHandler() {
    return new SimpleUrlAuthenticationFailureHandler("/login?error") {
      @Override
      public void onAuthenticationFailure(
          jakarta.servlet.http.HttpServletRequest request,
          jakarta.servlet.http.HttpServletResponse response,
          org.springframework.security.core.AuthenticationException exception)
          throws java.io.IOException, jakarta.servlet.ServletException {
        String username = request.getParameter("username");
        if (username != null) {
          request.getSession().setAttribute("savedUsername", username);
        }
        super.onAuthenticationFailure(request, response, exception);
      }
    };
  }

  /**
   * remember-me 의 PersistentToken 저장소. - persistent_logins 테이블은 PersistentLogin Entity 로 ddl-auto
   * 자동 관리 → createTableOnStartup(false) - 같은 series 의 옛 token 사용 시 Spring 이 자동으로 도난 의심 → 전체 무효화
   */
  @Bean
  public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
    JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
    repo.setDataSource(dataSource);
    // ddl-auto 가 Entity 기반으로 테이블 생성하므로 false. (true 시 SQL CREATE 충돌)
    repo.setCreateTableOnStartup(false);
    return repo;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      PersistentTokenRepository persistentTokenRepository,
      @Value("${security.remember-me.key}") String rememberMeKey)
      throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth
                    // 인증 페이지는 우선 permit (Spring Security 7 매처 동작 이슈 회피용 단독 명시)
                    .requestMatchers(
                        "/login",
                        "/signup",
                        "/api/users/check-email",
                        // F-signup-01: 휴대폰 인증 API (비인증 signup 화면에서 호출).
                        // CSRF 는 유지 — signup.html 이 meta 태그로 토큰 제공.
                        "/api/phone/send-code",
                        "/api/phone/verify-code",
                        "/find-id",
                        "/find-password",
                        "/find-password/**",
                        // P0-2 A1 이월: /admin/login 페이지·성공 리다이렉트는 후속 티켓.
                        // 지금은 매처만 등록해 후속에서 formLogin 재설정 시 곧바로 permit 되도록 준비.
                        "/admin/login")
                    .permitAll()
                    // P0-2: 관리자 영역 전체 hasAnyRole 매처. anyRequest() 앞에 삽입하여
                    // "/admin/**" 하위 URL 은 CENTER_ADMIN / SYSTEM_ADMIN 만 접근 가능.
                    .requestMatchers("/admin/**")
                    .hasAnyRole("CENTER_ADMIN", "SYSTEM_ADMIN")
                    // 인증 필요 (먼저 매칭되어 permitAll 보다 우선)
                    .requestMatchers(
                        "/programs/*/apply",
                        "/bookmarks/**",
                        "/notifications/**",
                        "/mypage",
                        "/mypage/**",
                        // F-signup-03: 온보딩 화면 — signup 자동 로그인 후 진입.
                        "/welcome",
                        "/welcome/**")
                    .authenticated()
                    // 그 외 비인증 허용
                    .requestMatchers(
                        "/",
                        "/api/ping",
                        "/programs",
                        "/programs/**",
                        "/notices",
                        "/notices/**",
                        "/centers",
                        "/centers/**",
                        // Spring 이 ResponseStatusException 등을 내부 forward → /error 로 dispatch.
                        // 비인증 URL 에서 404 등을 던질 때 /error 가 다시 로그인 리다이렉트 되지 않도록 허용.
                        "/error",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/favicon.ico")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(
            form ->
                form.loginPage("/login")
                    .loginProcessingUrl("/login")
                    .defaultSuccessUrl("/", true)
                    .failureHandler(loginFailureHandler())
                    .permitAll())
        // Spring Security remember-me — PersistentToken (DB 기반).
        // 사용자가 "로그인 상태 유지" 체크 시 series + token 발급.
        // 매 사용 시 token rotation. 같은 series 옛 token 사용 시 도난 의심 → 전체 무효화.
        .rememberMe(
            remember ->
                remember
                    .key(rememberMeKey)
                    .rememberMeParameter("remember-me") // form 의 input name
                    .tokenRepository(persistentTokenRepository)
                    .tokenValiditySeconds(60 * 60 * 24 * 14) // 14일 (Spring 기본값)
            )
        .logout(
            logout ->
                logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
                    // 로그아웃 시 세션 + remember-me cookie 모두 제거
                    // DB 의 persistent_logins row 는 Spring 의 RememberMeServices.logout() 이 자동 제거
                    .deleteCookies("JSESSIONID", "remember-me")
                    .permitAll());
    // P0-2: CSRF 활성. Spring Security 7 기본 = 세션 저장 CsrfTokenRepository
    // (HttpSessionCsrfTokenRepository).
    //   - Thymeleaf 는 ${_csrf.token} / ${_csrf.headerName} 로 접근
    //   - HTMX 는 static/js/htmx-csrf.js 가 meta 태그 값을 configRequest 에서 헤더로 부착
    //   - 기존 .csrf(csrf -> csrf.disable()) 삭제 (직접 disable 하지 않음, 기본 활성 유지)
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * F-signup-03: signup 자동 로그인 시 세션에 SecurityContext 를 저장하기 위한 저장소. Spring Security 7 에서는 filter
   * 체인이 자동으로 사용하는 저장소와 동일한 인스턴스를 직접 saveContext() 로 호출해야 signup 이후 다음 요청에서 인증 상태가 유지됨.
   */
  @Bean
  public SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }
}

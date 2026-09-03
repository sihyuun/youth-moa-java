package io.github.sihyuuun.youthmoa.common.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * A1-admin-shell (Qn-1 A안): 관리자 트랙과 사용자 트랙의 SecurityFilterChain 을 완전히 분리.
 *
 * <ul>
 *   <li>Order 1 — {@link #adminSecurityFilterChain} : {@code securityMatcher("/admin/**")} 로 관리자
 *       요청만 처리. 자체 formLogin(/admin/login), logout, hasAnyRole("CENTER_ADMIN","SYSTEM_ADMIN"),
 *       remember-me 미적용 (Qn-3 B).
 *   <li>Order 2 — {@link #userSecurityFilterChain} : 나머지 요청. 기존 사용자 formLogin·remember-me 그대로.
 * </ul>
 *
 * Spring Security 7 은 {@code securityMatcher()} 로 매칭되지 않은 요청은 자동으로 다음 chain 으로 넘어가므로 두 chain 이
 * 충돌 없이 공존한다. ({@code UnreachableFilterChainException} 방지 위해 admin chain 은 명시적 matcher 지정 필수.)
 */
@Configuration
public class SecurityConfig {

  /** 사용자 로그인 실패 시 username 을 세션에 보존해 로그인 폼 재표시 시 채워둠. */
  private static SimpleUrlAuthenticationFailureHandler loginFailureHandler() {
    return failureHandler("/login?error");
  }

  /** 관리자 로그인 실패 시 username 을 세션에 보존해 /admin/login?error 로 리다이렉트. */
  private static SimpleUrlAuthenticationFailureHandler adminLoginFailureHandler() {
    return failureHandler("/admin/login?error");
  }

  private static SimpleUrlAuthenticationFailureHandler failureHandler(String defaultFailureUrl) {
    return new SimpleUrlAuthenticationFailureHandler(defaultFailureUrl) {
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

  /**
   * A1 (Qn-1 A · Qn-2 A · Qn-3 B): 관리자 전용 SecurityFilterChain.
   *
   * <ul>
   *   <li>매처: {@code /admin/**} — 이 chain 이 처리하지 않는 URL 은 자동으로 order 2 로 넘어감
   *   <li>인가: {@code /admin/login} permit, 그 외 CENTER_ADMIN 또는 SYSTEM_ADMIN 필요
   *   <li>formLogin: {@code /admin/login} 페이지·프로세싱 URL, 성공 시 {@code /admin}, 실패 시 {@code
   *       /admin/login?error} + savedUsername 세션 보존
   *   <li>logout: {@code POST /admin/logout} → {@code /admin/login?logout} (Qn-2 A)
   *   <li>remember-me: 미적용 (Qn-3 B) — 관리자 권한은 세션 유출 위험이 크므로 편의보다 안전 우선
   *   <li>CSRF: 활성 (기본값)
   * </ul>
   */
  @Bean
  @Order(1)
  public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/admin/**")
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/admin/login")
                    .permitAll()
                    .anyRequest()
                    .hasAnyRole("CENTER_ADMIN", "SYSTEM_ADMIN"))
        .formLogin(
            form ->
                form.loginPage("/admin/login")
                    .loginProcessingUrl("/admin/login")
                    .defaultSuccessUrl("/admin", true)
                    .failureUrl("/admin/login?error")
                    .failureHandler(adminLoginFailureHandler())
                    .permitAll())
        .logout(
            logout ->
                logout
                    .logoutUrl("/admin/logout")
                    .logoutSuccessUrl("/admin/login?logout")
                    .deleteCookies("JSESSIONID"));
    // remember-me 미적용 (Qn-3 B). CSRF 는 Spring Security 기본 활성 유지.
    return http.build();
  }

  /**
   * A1 (Qn-1 A): 사용자 트랙 SecurityFilterChain. Order 2 — admin chain 이 처리하지 않은 요청만 도달.
   *
   * <p>기존 P0-2 매처의 {@code /admin/**} hasAnyRole 및 {@code /admin/login} permit 는 A1 에서 admin chain 으로 이동
   * → 여기서 제거. 나머지 로직은 이전과 동일.
   */
  @Bean
  @Order(Ordered.LOWEST_PRECEDENCE)
  public SecurityFilterChain userSecurityFilterChain(
      HttpSecurity http,
      PersistentTokenRepository persistentTokenRepository,
      Environment environment,
      @Value("${security.remember-me.key}") String rememberMeKey)
      throws Exception {
    // fix-e2e-seed-pollution: e2e 프로파일에서만 test-only fixture endpoint 공개.
    // TestFixtureController 자체가 @Profile("e2e") 이라 다른 프로파일에서는 Bean 미등록.
    // Security 매처만 항상 등록하되, 실제 endpoint 존재는 프로파일이 통제 → 이중 안전장치.
    boolean e2eProfile = environment.matchesProfiles("e2e");
    http.authorizeHttpRequests(
            auth -> {
              if (e2eProfile) {
                auth.requestMatchers("/__test__/**").permitAll();
              }
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
                      "/find-password/**")
                  .permitAll()
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
                      // 이용약관·개인정보처리방침·이메일 무단 수집거부 정적 페이지 (푸터/회원가입에서 링크)
                      "/terms",
                      "/privacy",
                      "/email-policy",
                      // Spring 이 ResponseStatusException 등을 내부 forward → /error 로 dispatch.
                      // 비인증 URL 에서 404 등을 던질 때 /error 가 다시 로그인 리다이렉트 되지 않도록 허용.
                      "/error",
                      // chore-observability (2026-07-23): Actuator 는 별도 포트 9091 로 노출됨.
                      "/actuator/**",
                      "/css/**",
                      "/js/**",
                      "/images/**",
                      "/webjars/**",
                      "/favicon.ico")
                  .permitAll()
                  .anyRequest()
                  .authenticated();
            })
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
    if (e2eProfile) {
      // fix-e2e-seed-pollution: Playwright request.post 는 세션 CSRF 토큰을 자동 부착하지 않으므로
      // e2e 프로파일에서만 /__test__/** 경로를 CSRF 대상에서 제외.
      http.csrf(csrf -> csrf.ignoringRequestMatchers("/__test__/**"));
    }
    // P0-2: CSRF 활성. Spring Security 7 기본 = 세션 저장 CsrfTokenRepository
    // (HttpSessionCsrfTokenRepository).
    //   - Thymeleaf 는 ${_csrf.token} / ${_csrf.headerName} 로 접근
    //   - HTMX 는 static/js/htmx-csrf.js 가 meta 태그 값을 configRequest 에서 헤더로 부착
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

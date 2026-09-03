package io.github.sihyuuun.youthmoa.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

/**
 * fix-e2e-seed-pollution 회귀 방지 — 실 HTTP 라우팅 검증판.
 *
 * <p>{@link TestFixtureProfileGuardTest} 가 컨텍스트 로드 후 Bean 미등록 assert 만 수행하는 것과 달리, 이 테스트는 실제 서블릿을
 * {@code RANDOM_PORT} 로 기동해 {@code POST /__test__/reset-applications} 호출 시 <b>404</b> 응답이 나오는지
 * 실측한다. 라우팅 자체가 미등록임을 HTTP status 로 직접 증명한다.
 *
 * <p>인증·CSRF 우회: 프로덕션 {@link io.github.sihyuuun.youthmoa.common.config.SecurityConfig} 는 e2e 가 아닌
 * 프로파일에서 {@code /__test__/**} 를 authenticated() 로 판정해 로그인 페이지로 302 redirect 시킨다. 이 상태로는 "라우팅 미등록"
 * 신호가 Security 필터에 가려진다. 그래서 {@link TestSecurityAllPermit} 를 통해 test-guard 프로파일에서만 anyRequest 를
 * permitAll 로 만든 SecurityFilterChain 을 {@code @Order(1)} 로 앞에 삽입한다. 프로덕션 chain 은 그대로 두고, 테스트 컨텍스트에만
 * 우선순위가 더 높은 chain 을 얹어 순수 MVC 라우팅 결과(=404)만 남긴다.
 *
 * <p>이 테스트가 실패한다는 것은 곧 {@link TestFixtureController} 가 프로덕션(non-e2e) 프로파일에서도 라우팅되고 있다는 뜻이며, 인증 없이
 * 신청 데이터를 삭제할 수 있는 endpoint 가 실서비스에 노출되는 심각한 회귀다.
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:test-guard-http;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false",
      "app.seed.enabled=false"
    })
@ActiveProfiles("test-guard")
@Import(TestFixtureHttpGuardTest.TestSecurityAllPermit.class)
class TestFixtureHttpGuardTest {

  @LocalServerPort private int port;

  @Test
  void resetApplications_endpoint_e2e_외_프로파일에서_404() throws Exception {
    HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/__test__/reset-applications"))
            .header("Content-Type", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "{\"userEmail\":\"seed30@youth-moa.test\",\"programId\":7}"))
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode())
        .as("test-guard 프로파일에서 /__test__/reset-applications 는 라우팅 자체가 등록되지 않아야 함")
        .isEqualTo(404);
  }

  /**
   * test-guard 프로파일 전용 open SecurityFilterChain.
   *
   * <p>@Order(1) 로 프로덕션 SecurityConfig chain 보다 앞에 매칭되어, 모든 요청을 permit + csrf disable 로 통과시킨다.
   * Security 필터가 가로채지 않으므로 DispatcherServlet 까지 요청이 도달하고, 미등록 endpoint 는 순수 MVC 결과인 404 를 반환한다.
   */
  @TestConfiguration
  static class TestSecurityAllPermit {

    @Bean
    @Order(1)
    SecurityFilterChain testAllPermitFilterChain(HttpSecurity http) throws Exception {
      // securityMatcher 로 /__test__/** 만 이 chain 이 처리하도록 제한.
      // anyRequest 로 두면 프로덕션 SecurityConfig chain 이 UnreachableFilterChainException 으로
      // 감지되어 컨텍스트 로드가 실패한다. 우리는 /__test__/** 경로의 라우팅 결과만 확인하면 되므로
      // 이 chain 의 유효 범위를 좁혀 프로덕션 chain 을 그대로 살려둔다.
      http.securityMatcher("/__test__/**")
          .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
          .csrf(csrf -> csrf.disable())
          .formLogin(form -> form.disable())
          .httpBasic(basic -> basic.disable())
          .logout(logout -> logout.disable());
      return http.build();
    }
  }
}

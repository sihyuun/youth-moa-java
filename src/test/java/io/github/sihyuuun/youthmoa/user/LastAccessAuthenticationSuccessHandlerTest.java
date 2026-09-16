package io.github.sihyuuun.youthmoa.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.sihyuuun.youthmoa.user.LastAccessAuthenticationSuccessHandler.Updater;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * A5 admin-users (2026-09-15): {@link LastAccessAuthenticationSuccessHandler} 회귀 방어.
 *
 * <p>spec §5 최우선 회귀 방어 항목:
 *
 * <ol>
 *   <li>정상: lastAccessAt Updater 호출됨
 *   <li>Updater 예외 발생 시 로그인 flow 는 무영향 (예외 삼킴)
 *   <li>Updater 는 별도 컴포넌트 (Spring 프록시 self-invocation 회피) — {@code @Transactional(REQUIRES_NEW)} 가
 *       부착돼 있어야 spec §3 회귀 방어를 만족
 * </ol>
 *
 * <p>실행 컨텍스트: 단위 테스트. handler.onAuthenticationSuccess 를 직접 호출하고 super 의 redirect 는 mock 응답으로 흡수.
 */
class LastAccessAuthenticationSuccessHandlerTest {

  private Updater updater;
  private LastAccessAuthenticationSuccessHandler handler;

  @BeforeEach
  void setup() {
    updater = mock(Updater.class);
    handler = new LastAccessAuthenticationSuccessHandler(updater);
    // super redirect 흡수: 기본 URL 만 지정하면 SimpleUrlAuthenticationSuccessHandler 가 sendRedirect 시도.
    handler.setDefaultTargetUrl("/");
  }

  @Test
  void success_invokes_updater_with_principal_id() throws Exception {
    UserPrincipal principal = buildPrincipal(42L);
    Authentication auth =
        new UsernamePasswordAuthenticationToken(principal, "N/A", principal.getAuthorities());
    HttpServletRequest req = new MockHttpServletRequest();
    HttpServletResponse res = new MockHttpServletResponse();

    handler.onAuthenticationSuccess(req, res, auth);

    verify(updater).updateLastAccess(42L);
  }

  @Test
  void updater_exception_swallowed_login_still_succeeds() throws Exception {
    // 회귀 방어의 핵심: DB 예외 발생해도 로그인 flow 는 성공해야 한다.
    // 예외가 밖으로 새면 로그인 폼이 error 화면으로 이동하는 회귀 발생.
    UserPrincipal principal = buildPrincipal(42L);
    Authentication auth =
        new UsernamePasswordAuthenticationToken(principal, "N/A", principal.getAuthorities());
    willThrow(new RuntimeException("DB 다운 시뮬레이션")).given(updater).updateLastAccess(any());

    HttpServletRequest req = new MockHttpServletRequest();
    MockHttpServletResponse res = new MockHttpServletResponse();

    // 예외 없이 완료돼야 함 (super redirect 만 정상 진행)
    handler.onAuthenticationSuccess(req, res, auth);

    // super 가 redirect 를 시도했음을 확인 → 로그인 flow 자체는 계속 진행됨
    assertThat(res.getStatus()).isEqualTo(302);
  }

  @Test
  void non_UserPrincipal_principal_skipped_gracefully() throws Exception {
    // principal 이 UserPrincipal 이 아닌 경우 (예외 케이스) → 갱신 스킵 + 로그인 정상 성공
    Authentication auth =
        new UsernamePasswordAuthenticationToken(
            "someString", "N/A", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    HttpServletRequest req = new MockHttpServletRequest();
    MockHttpServletResponse res = new MockHttpServletResponse();

    handler.onAuthenticationSuccess(req, res, auth);

    assertThat(res.getStatus()).isEqualTo(302);
  }

  @Test
  void updater_class_annotated_with_REQUIRES_NEW() throws Exception {
    // spec §3 · §5 명시: Updater#updateLastAccess 는 @Transactional(propagation = REQUIRES_NEW)
    // 로 별도 트랜잭션에서 실행돼야 한다 (로그인 트랜잭션과 분리 · 실패 시 rollback 격리).
    var method = Updater.class.getMethod("updateLastAccess", Long.class);
    org.springframework.transaction.annotation.Transactional tx =
        method.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
    assertThat(tx).as("Updater#updateLastAccess 에 @Transactional 부착 필수").isNotNull();
    assertThat(tx.propagation())
        .as("REQUIRES_NEW — 로그인 tx 와 분리")
        .isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW);
  }

  // ============ helper ============

  private static UserPrincipal buildPrincipal(Long id) throws Exception {
    User u = User.builder().email("u@t.com").password("x").name("u").role(UserRole.USER).build();
    Field f = User.class.getDeclaredField("id");
    f.setAccessible(true);
    f.set(u, id);
    return new UserPrincipal(u);
  }
}

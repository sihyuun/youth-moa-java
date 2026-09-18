package io.github.sihyuuun.youthmoa.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.sihyuuun.youthmoa.center.Center;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A5-1 admin-staff-management (2026-09-18): {@link PasswordChangeRequiredInterceptor} 단위 검증.
 *
 * <p>회귀 방어 3점:
 *
 * <ul>
 *   <li>비로그인 요청 통과 (Spring Security 가 처리)
 *   <li>flag=FALSE 유저 통과 (O(1) — 일반 사용자·seedAdmins 무회귀)
 *   <li>flag=TRUE 유저는 /password/change · 정적 리소스 · /login · /logout 제외한 모든 요청에서 리다이렉트
 * </ul>
 */
class PasswordChangeRequiredInterceptorTest {

  private final PasswordChangeRequiredInterceptor interceptor =
      new PasswordChangeRequiredInterceptor();

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void unauthenticated_request_passes() throws Exception {
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse res = mock(HttpServletResponse.class);
    when(req.getRequestURI()).thenReturn("/admin/users");

    assertThat(interceptor.preHandle(req, res, new Object())).isTrue();
    verify(res, never()).sendRedirect(eq("/password/change"));
  }

  @Test
  void must_change_false_passes_immediately() throws Exception {
    setAuth(false);
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse res = mock(HttpServletResponse.class);
    when(req.getRequestURI()).thenReturn("/admin/users");

    assertThat(interceptor.preHandle(req, res, new Object())).isTrue();
    verify(res, never()).sendRedirect(eq("/password/change"));
  }

  @Test
  void must_change_true_redirects_to_password_change() throws Exception {
    setAuth(true);
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse res = mock(HttpServletResponse.class);
    when(req.getRequestURI()).thenReturn("/admin/users");
    when(req.getContextPath()).thenReturn("");

    assertThat(interceptor.preHandle(req, res, new Object())).isFalse();
    verify(res).sendRedirect("/password/change");
  }

  @Test
  void must_change_true_password_change_path_passes() throws Exception {
    setAuth(true);
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse res = mock(HttpServletResponse.class);
    when(req.getRequestURI()).thenReturn("/password/change");

    assertThat(interceptor.preHandle(req, res, new Object())).isTrue();
    verify(res, never()).sendRedirect(eq("/password/change"));
  }

  @Test
  void must_change_true_static_resources_pass() throws Exception {
    setAuth(true);
    for (String path :
        List.of(
            "/css/main.css",
            "/js/app.js",
            "/images/logo.png",
            "/webjars/htmx.min.js",
            "/favicon.ico",
            "/error",
            "/actuator/health")) {
      HttpServletRequest req = mock(HttpServletRequest.class);
      HttpServletResponse res = mock(HttpServletResponse.class);
      when(req.getRequestURI()).thenReturn(path);
      assertThat(interceptor.preHandle(req, res, new Object())).as("path=" + path).isTrue();
      verify(res, never()).sendRedirect(eq("/password/change"));
    }
  }

  @Test
  void must_change_true_login_logout_pass() throws Exception {
    setAuth(true);
    for (String path : List.of("/login", "/logout", "/admin/login", "/admin/logout")) {
      HttpServletRequest req = mock(HttpServletRequest.class);
      HttpServletResponse res = mock(HttpServletResponse.class);
      when(req.getRequestURI()).thenReturn(path);
      assertThat(interceptor.preHandle(req, res, new Object())).as("path=" + path).isTrue();
      verify(res, never()).sendRedirect(eq("/password/change"));
    }
  }

  private void setAuth(boolean mustChangePassword) {
    User user =
        User.builder()
            .email("t@t.local")
            .name("t")
            .role(UserRole.USER)
            .password("noop")
            .center((Center) null)
            .build();
    // 리플렉션 없이 도메인 메서드로 상태 세팅
    if (mustChangePassword) {
      user.assignInitialPassword("noop", null);
    }
    // UserPrincipal 은 id 를 User.id 에서 읽는데 여기 id 는 null. 인터셉터 로직에 id 는 사용 안 됨.
    UserPrincipal principal = new UserPrincipal(user);
    UsernamePasswordAuthenticationToken token =
        new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(token);
  }
}

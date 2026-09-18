package io.github.sihyuuun.youthmoa.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * A5-1 admin-staff-management (2026-09-18): {@link UserPrincipal#isMustChangePassword()} 가 TRUE 인
 * 사용자를 <code>/password/change</code> 로 강제 이동시킨다.
 *
 * <p>회귀 방어 3점 (spec §6):
 *
 * <ul>
 *   <li>일반 사용자 (flag=FALSE) 는 O(1) 즉시 통과 — 인증 정보 조회만 1회
 *   <li>seed 관리자 계정은 V19 default FALSE 로 발동하지 않음 — 로그인 flow 무회귀
 *   <li>비로그인 요청은 통과 (Spring Security 가 별도 처리)
 * </ul>
 *
 * <p>허용 경로 (강제 redirect 예외):
 *
 * <ul>
 *   <li>{@code /password/change} — 변경 폼 자체
 *   <li>{@code /login}, {@code /logout}, {@code /admin/login}, {@code /admin/logout} — 인증 flow
 *   <li>정적 리소스 {@code /css/**} {@code /js/**} {@code /images/**} {@code /webjars/**} · {@code
 *       /favicon.ico} · {@code /error} · {@code /actuator/**}
 * </ul>
 */
@Component
public class PasswordChangeRequiredInterceptor implements HandlerInterceptor {

  static final String REDIRECT_TARGET = "/password/change";

  private static final Set<String> EXACT_ALLOWED =
      Set.of(
          "/password/change",
          "/login",
          "/logout",
          "/admin/login",
          "/admin/logout",
          "/favicon.ico",
          "/error");

  private static final List<String> PREFIX_ALLOWED =
      List.of("/css/", "/js/", "/images/", "/webjars/", "/actuator/", "/__test__/");

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws IOException {
    // 인증 미완이면 통과 (Spring Security 가 처리)
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) return true;
    Object principal = auth.getPrincipal();
    if (!(principal instanceof UserPrincipal up)) return true;

    // flag=FALSE (대부분의 사용자) 즉시 통과 — O(1)
    if (!up.isMustChangePassword()) return true;

    // 허용 경로는 통과
    String uri = request.getRequestURI();
    if (EXACT_ALLOWED.contains(uri)) return true;
    for (String prefix : PREFIX_ALLOWED) {
      if (uri.startsWith(prefix)) return true;
    }

    // 그 외 요청은 /password/change 로 강제 리다이렉트
    response.sendRedirect(request.getContextPath() + REDIRECT_TARGET);
    return false;
  }
}

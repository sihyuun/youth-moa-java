package io.github.sihyuuun.youthmoa.stats;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * A6 (2026-09-16 신설) — postHandle 에서 in-memory 방문자 누적.
 *
 * <p>WebMvcConfig 에서 등록되며, {@code /admin/**} · 정적 리소스 · actuator 는 제외 패턴으로 스킵된다.
 *
 * <p>성능 목표: p50 +2ms 이내. DB write 는 하지 않고 {@link DailyVisitCollector} 만 호출.
 */
@Component
public class VisitTrackingInterceptor implements HandlerInterceptor {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final ObjectProvider<DailyVisitCollector> collectorProvider;

  public VisitTrackingInterceptor(ObjectProvider<DailyVisitCollector> collectorProvider) {
    this.collectorProvider = collectorProvider;
  }

  @Override
  public void postHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      ModelAndView modelAndView) {
    int status = response.getStatus();
    // 200~399 만 카운트. 4xx/5xx 는 실제 콘텐츠 조회로 볼 수 없음.
    if (status < 200 || status >= 400) return;

    LocalDate today = LocalDate.now(KST);
    HttpSession session = request.getSession(false);
    String sessionId = session != null ? session.getId() : null;
    // 세션이 아직 없는 익명 첫 요청은 강제로 세션을 만들어 유니크 카운트에 반영.
    // (Interceptor 진입 시점에는 세션이 없을 수 있음)
    if (sessionId == null) {
      HttpSession created = request.getSession(true);
      sessionId = created.getId();
    }

    DailyVisitCollector collector = collectorProvider.getIfAvailable();
    if (collector == null) return;
    boolean authenticated = isAuthenticated();
    collector.record(today, sessionId, authenticated);
  }

  private boolean isAuthenticated() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) return false;
    // AnonymousAuthenticationToken 도 isAuthenticated()=true 이므로 principal 로 판별.
    Object principal = auth.getPrincipal();
    return principal != null && !"anonymousUser".equals(principal);
  }
}

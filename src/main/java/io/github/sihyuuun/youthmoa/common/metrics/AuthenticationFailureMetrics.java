package io.github.sihyuuun.youthmoa.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.stereotype.Component;

/**
 * chore-observability PR-2: 로그인 실패 지표.
 *
 * <p>Micrometer 이름 {@code youthmoa.login.failure} → Prometheus 노출 이름 {@code
 * youthmoa_login_failure_total}.
 *
 * <p><b>계측 방식</b>: SecurityConfig 의 failureHandler 를 수정하지 않고 Spring Security 표준 이벤트 ({@link
 * AbstractAuthenticationFailureEvent}) 를 구독. Boot 이 {@code DefaultAuthenticationEventPublisher} 를
 * 자동 구성하므로 실패 시 항상 이벤트가 발화된다.
 *
 * <p><b>태그 없음</b>: username 태그 절대 금지 (카디널리티 폭발 + PII). exception type 태그도 attack surface (예:
 * BadCredentialsException 만 vs LockedException) 를 카테고리화하는 후속 티켓에서 상한 있는 값으로 추가 검토.
 */
@Component
@RequiredArgsConstructor
public class AuthenticationFailureMetrics {

  private final MeterRegistry meterRegistry;
  private Counter loginFailureCounter;

  @PostConstruct
  void initMetrics() {
    this.loginFailureCounter =
        Counter.builder("youthmoa.login.failure")
            .description("Spring Security 인증 실패 이벤트 누적 건수 (BadCredentials·Locked·Disabled 등 전체)")
            .register(meterRegistry);
  }

  @EventListener
  public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
    loginFailureCounter.increment();
  }
}

package io.github.sihyuuun.youthmoa.common.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;

/**
 * chore-observability PR-2: 로그인 실패 카운터 단위 테스트.
 *
 * <p>{@link AuthenticationFailureMetrics} 를 SimpleMeterRegistry 로 격리 검증.
 */
class AuthenticationFailureMetricsTest {

  private SimpleMeterRegistry registry;
  private AuthenticationFailureMetrics metrics;

  @BeforeEach
  void setUp() {
    this.registry = new SimpleMeterRegistry();
    this.metrics = new AuthenticationFailureMetrics(registry);
    metrics.initMetrics();
  }

  @Test
  @DisplayName("BadCredentials 실패 이벤트 발화 시 counter 1 증가")
  void badCredentials_increments_counter() {
    metrics.onAuthenticationFailure(
        new AuthenticationFailureBadCredentialsEvent(
            new UsernamePasswordAuthenticationToken("u", "p"), new BadCredentialsException("bad")));

    assertThat(registry.counter("youthmoa.login.failure").count()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("Locked 등 다른 실패 이벤트도 동일 counter 로 집계 (전체 실패 누적)")
  void other_failures_also_increment() {
    metrics.onAuthenticationFailure(
        new AuthenticationFailureLockedEvent(
            new UsernamePasswordAuthenticationToken("u", "p"), new LockedException("locked")));
    metrics.onAuthenticationFailure(
        new AuthenticationFailureBadCredentialsEvent(
            new UsernamePasswordAuthenticationToken("u", "p"), new BadCredentialsException("bad")));

    assertThat(registry.counter("youthmoa.login.failure").count()).isEqualTo(2.0);
  }

  @Test
  @DisplayName("counter 이름은 youthmoa.login.failure (Prometheus 변환 시 _total 접미사)")
  void counter_name_matches_spec() {
    assertThat(registry.get("youthmoa.login.failure").counter()).isNotNull();
  }
}

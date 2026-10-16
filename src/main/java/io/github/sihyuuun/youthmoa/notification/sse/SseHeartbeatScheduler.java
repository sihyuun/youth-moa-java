package io.github.sihyuuun.youthmoa.notification.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * A7-realtime (2026-10-15): 모든 활성 SSE emitter 에 25초 주기로 heartbeat 이벤트 발신.
 *
 * <p>Q6 결정 — 25s. nginx 기본 proxy_read_timeout(60s) · Fly.io idle timeout 안전 범위. 핑이 없으면 중간 프록시가 연결을
 * idle 로 간주하고 종료.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseHeartbeatScheduler {

  private final SseConnectionRegistry registry;

  @Scheduled(fixedRate = 25_000L)
  public void sendHeartbeat() {
    int active = registry.countAll();
    if (active == 0) return;
    registry.broadcastAll("heartbeat", "ping");
    log.trace("[A7-sse] heartbeat sent active={}", active);
  }
}

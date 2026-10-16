package io.github.sihyuuun.youthmoa.notification.sse;

import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * A7-realtime (2026-10-15): admin 알림 SSE endpoint.
 *
 * <p>관리자 로그인 상태에서 HTMX {@code sse-connect="/admin/notifications/stream"} 로 접속. {@link SseEmitter} 를
 * 반환하면 Servlet async 커넥션으로 chunked response 가 열리고, 서버는 {@link SseConnectionRegistry} 를 통해 push.
 *
 * <p>security: 관리자 chain({@code /admin/**}) 상속 + {@code @PreAuthorize} 이중 방어.
 */
@Slf4j
@RestController
@RequestMapping("/admin/notifications")
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CENTER_ADMIN')")
@RequiredArgsConstructor
public class NotificationSseController {

  /** 30분 = SseEmitter timeout. HTMX sse-ext 가 자동 재연결하므로 짧게 가져도 안전. */
  private static final long EMITTER_TIMEOUT_MS = 30L * 60L * 1000L;

  private final SseConnectionRegistry registry;

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(
      @AuthenticationPrincipal UserPrincipal principal, HttpServletResponse response) {
    // 프록시 buffering 방지 — nginx 등 upstream 이 응답을 모아뒀다 flush 하는 것을 차단.
    response.setHeader("X-Accel-Buffering", "no");
    response.setHeader("Cache-Control", "no-cache");

    SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
    Long userId = principal.getId();
    registry.register(userId, emitter);

    try {
      // 연결 수립 직후 ready 이벤트 — 테스트/디버깅용.
      emitter.send(SseEmitter.event().name("ready").data("connected"));
    } catch (IOException e) {
      log.debug("[A7-sse] initial send failed userId={}", userId, e);
      emitter.completeWithError(e);
      return emitter;
    }
    log.debug("[A7-sse] stream opened userId={} activeTotal={}", userId, registry.countAll());
    return emitter;
  }
}

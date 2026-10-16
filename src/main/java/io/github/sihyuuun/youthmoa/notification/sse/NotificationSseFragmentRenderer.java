package io.github.sihyuuun.youthmoa.notification.sse;

import io.github.sihyuuun.youthmoa.notification.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * A7-realtime (2026-10-15): SSE event 로 push 할 HTML fragment 를 Thymeleaf 로 렌더.
 *
 * <p>Q7 결정 — HTML fragment 포맷. HTMX sse-ext 의 {@code sse-swap} 속성이 그대로 삽입하므로 JSON 대신 완성된 HTML 을
 * 보낸다. 기존 {@code _notification-badge.html} fragment 를 재사용해 템플릿 중복을 피한다.
 *
 * <p>SSE wire format 에서 {@code data:} 라인 안의 개행은 두 번째 데이터 라인으로 해석되므로 렌더 결과의 LF/CRLF 를 공백으로 치환. HTMX
 * 는 data 라인을 모두 수신 후 join 하지만 공백 치환이 가장 안전.
 */
@Component
@RequiredArgsConstructor
public class NotificationSseFragmentRenderer {

  private final ApplicationContext applicationContext;

  /**
   * 배지 fragment 렌더 — {@code #admin-notif-badge} span 하나.
   *
   * @param unreadCount 미읽음 알림 수
   */
  public String renderBadge(long unreadCount) {
    SpringTemplateEngine engine = resolveEngine();
    Context ctx = new Context();
    ctx.setVariable("count", unreadCount);
    String html = engine.process("admin/fragments/_notification-badge :: badge(${count})", ctx);
    return collapseWhitespace(html);
  }

  /**
   * 신규 알림 item fragment 렌더 — 드롭다운 리스트 상단 prepend 용 {@code <li>}.
   *
   * <p>신규 전용 fragment ({@code _notification-sse-item.html :: item}) 를 사용해 기존 drop-down 템플릿과 결합도를
   * 줄인다.
   */
  public String renderItem(Notification notification) {
    SpringTemplateEngine engine = resolveEngine();
    Context ctx = new Context();
    ctx.setVariable("n", notification);
    String html = engine.process("admin/fragments/_notification-sse-item :: item", ctx);
    return collapseWhitespace(html);
  }

  private SpringTemplateEngine resolveEngine() {
    return applicationContext.getBean(SpringTemplateEngine.class);
  }

  /** SSE data 라인 안의 개행을 공백으로 치환. */
  private String collapseWhitespace(String html) {
    if (html == null) return "";
    return html.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ').trim();
  }
}

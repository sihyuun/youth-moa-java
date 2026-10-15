package io.github.sihyuuun.youthmoa.notification.sse;

import io.github.sihyuuun.youthmoa.notification.NotificationRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A7-realtime (2026-10-15): {@link NotificationBroadcastEvent} 를 받아 수신자 관리자의 활성 SSE 연결에 push.
 *
 * <p>Q2 결정 반영 — merged=true 면 badge 이벤트만, 신규(merged=false)는 badge + notification-item 모두 broadcast.
 * item fragment 는 드롭다운이 열려 있을 때 상단 prepend 되며, 닫혀 있으면 HTMX sse-swap 이 대상 미발견으로 무시.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSseEventListener {

  private final SseConnectionRegistry registry;
  private final NotificationSseFragmentRenderer renderer;
  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;

  @EventListener
  @Transactional(readOnly = true)
  public void onBroadcast(NotificationBroadcastEvent event) {
    Long userId = event.recipientUserId();
    if (registry.countFor(userId) == 0) {
      // 수신자가 접속 중이 아니면 조기 종료 — 쿼리 1회라도 절약.
      return;
    }
    try {
      User user = userRepository.getReferenceById(userId);
      long unread = notificationRepository.countByUserAndIsReadFalse(user);
      String badgeHtml = renderer.renderBadge(unread);
      registry.broadcast(userId, "notification-badge", badgeHtml);

      if (!event.merged()) {
        // Q2: 신규일 때만 item fragment 발신. merge 는 badge 재알림만.
        String itemHtml = renderer.renderItem(event.notification());
        registry.broadcast(userId, "notification-item", itemHtml);
      }
    } catch (RuntimeException e) {
      log.warn(
          "[A7-sse] broadcast failed userId={} notificationId={} merged={} reason={}",
          userId,
          event.notification() != null ? event.notification().getId() : null,
          event.merged(),
          e.getMessage());
    }
  }
}

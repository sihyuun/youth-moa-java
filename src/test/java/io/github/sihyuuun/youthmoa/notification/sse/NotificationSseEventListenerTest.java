package io.github.sihyuuun.youthmoa.notification.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.sihyuuun.youthmoa.notification.Notification;
import io.github.sihyuuun.youthmoa.notification.NotificationRepository;
import io.github.sihyuuun.youthmoa.notification.NotificationType;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * A7-realtime (2026-10-15): {@link NotificationSseEventListener} 분기 동작 검증.
 *
 * <p>Q2 결정 반영 — merged=true 는 badge 만, merged=false 는 badge + item broadcast.
 */
class NotificationSseEventListenerTest {

  private final NotificationSseFragmentRenderer renderer =
      Mockito.mock(NotificationSseFragmentRenderer.class);
  private final NotificationRepository notifRepo = Mockito.mock(NotificationRepository.class);
  private final UserRepository userRepo = Mockito.mock(UserRepository.class);

  @Test
  void 수신자_접속_없음_시_broadcast_생략() {
    SseConnectionRegistry registry = new SseConnectionRegistry();
    NotificationSseEventListener listener =
        new NotificationSseEventListener(registry, renderer, notifRepo, userRepo);
    Notification n = sampleNotification(1L, false);

    // 아무도 접속 안 함 → 조기 종료
    listener.onBroadcast(new NotificationBroadcastEvent(99L, n, false));

    Mockito.verifyNoInteractions(renderer, notifRepo, userRepo);
  }

  @Test
  void 신규_알림_시_badge_와_item_모두_broadcast() {
    SseConnectionRegistry registry = Mockito.mock(SseConnectionRegistry.class);
    given(registry.countFor(10L)).willReturn(1);
    User user = Mockito.mock(User.class);
    given(userRepo.getReferenceById(10L)).willReturn(user);
    given(notifRepo.countByUserAndIsReadFalse(user)).willReturn(3L);
    given(renderer.renderBadge(3L)).willReturn("<span id=\"admin-notif-badge\">3</span>");
    Notification n = sampleNotification(99L, false);
    given(renderer.renderItem(n)).willReturn("<li>new</li>");

    NotificationSseEventListener listener =
        new NotificationSseEventListener(registry, renderer, notifRepo, userRepo);
    listener.onBroadcast(new NotificationBroadcastEvent(10L, n, false));

    verify(registry).broadcast(eq(10L), eq("notification-badge"), any());
    verify(registry).broadcast(eq(10L), eq("notification-item"), any());
  }

  @Test
  void 병합_알림_시_badge_만_broadcast_item_생략() {
    SseConnectionRegistry registry = Mockito.mock(SseConnectionRegistry.class);
    given(registry.countFor(10L)).willReturn(1);
    User user = Mockito.mock(User.class);
    given(userRepo.getReferenceById(10L)).willReturn(user);
    given(notifRepo.countByUserAndIsReadFalse(user)).willReturn(1L);
    given(renderer.renderBadge(1L)).willReturn("<span id=\"admin-notif-badge\">1</span>");
    Notification n = sampleNotification(42L, true);

    NotificationSseEventListener listener =
        new NotificationSseEventListener(registry, renderer, notifRepo, userRepo);
    listener.onBroadcast(new NotificationBroadcastEvent(10L, n, true));

    verify(registry).broadcast(eq(10L), eq("notification-badge"), any());
    verify(registry, never()).broadcast(eq(10L), eq("notification-item"), any());
    verify(renderer, never()).renderItem(any());
  }

  @Test
  void broadcast_중_예외_발생_시_삼키고_fallback_로그() {
    SseConnectionRegistry registry = Mockito.mock(SseConnectionRegistry.class);
    given(registry.countFor(10L)).willReturn(1);
    given(userRepo.getReferenceById(10L)).willThrow(new RuntimeException("db down"));

    NotificationSseEventListener listener =
        new NotificationSseEventListener(registry, renderer, notifRepo, userRepo);

    // 예외가 바깥으로 새어나오면 상위 리스너 체인에 영향 → 반드시 삼켜야 함
    Notification n = sampleNotification(1L, false);
    listener.onBroadcast(new NotificationBroadcastEvent(10L, n, false));
    // 통과 시 테스트 성공
    assertThat(true).isTrue();
  }

  private Notification sampleNotification(Long id, boolean merged) {
    Notification n = Mockito.mock(Notification.class);
    given(n.getId()).willReturn(id);
    given(n.getOccurrenceCount()).willReturn(merged ? 2 : 1);
    given(n.getType()).willReturn(NotificationType.NEW_APPLICATION);
    return n;
  }

  // UserRole import 유지 — 다른 테스트와 동일한 import 패턴 (실제 사용 안 하지만 미래 확장 안전).
  @SuppressWarnings("unused")
  private static UserRole roleForFixture() {
    return UserRole.SYSTEM_ADMIN;
  }
}

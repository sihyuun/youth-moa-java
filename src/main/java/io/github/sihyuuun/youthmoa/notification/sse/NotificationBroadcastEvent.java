package io.github.sihyuuun.youthmoa.notification.sse;

import io.github.sihyuuun.youthmoa.notification.Notification;

/**
 * A7-realtime (2026-10-15): admin 알림 fan-out → SSE push 를 트리거하는 Spring application event.
 *
 * <p>{@link io.github.sihyuuun.youthmoa.notification.admin.AdminNotificationEventListener} 가 수신자별
 * {@code createOrMerge} 를 완료한 뒤 발행하면 {@link NotificationSseEventListener} 가 받아 {@link
 * SseConnectionRegistry} 로 broadcast 한다. 두 리스너를 분리해 테스트 가능성·단일 책임을 확보.
 *
 * <p>Q2 결정: 리스너 분기 — {@code merged=true} 면 badge 만 broadcast, {@code merged=false} (신규) 면 badge +
 * item 모두 broadcast.
 *
 * @param recipientUserId 알림을 수신한 관리자 user id
 * @param notification 저장된 (혹은 병합된) Notification 엔티티
 * @param merged true 면 기존 알림에 병합된 경우, false 면 신규 row 생성
 */
public record NotificationBroadcastEvent(
    Long recipientUserId, Notification notification, boolean merged) {}

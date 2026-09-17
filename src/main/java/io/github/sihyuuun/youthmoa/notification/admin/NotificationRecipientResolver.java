package io.github.sihyuuun.youthmoa.notification.admin;

import io.github.sihyuuun.youthmoa.user.User;
import java.util.List;

/**
 * A7 admin 헤더 알림 벨 (2026-09-17): 도메인 이벤트별 알림 수신자 결정 전략.
 *
 * <p>이벤트 리스너가 수신자 조회 로직을 직접 갖는 대신 Resolver 로 위임한다. SRP 준수 + 후속 확장(B-1 → B-3 로드맵)에서 구현체 추가만으로 정책을
 * 확장할 수 있도록 설계한다.
 *
 * <ul>
 *   <li>이번 A7 (B-1) — {@link ApplicationCreatedRecipientResolver} · {@link
 *       UserCreatedRecipientResolver}
 *   <li>후속 A7-createdBy-recipient (B-3) — {@code CreatedByRecipientResolver} chain
 *   <li>후속 (B-3+) — 선택한 사용자 watcher (`ProgramWatcher`) 를 위한 {@code WatcherRecipientResolver}
 * </ul>
 *
 * <p>Resolver 는 이벤트 스냅샷만 받아 {@link User} 목록을 반환한다. 각 리스너는 반환된 사용자에게 개별 Notification row 를 INSERT
 * (fan-out).
 *
 * @param <E> 이벤트 타입 (record snapshot)
 */
public interface NotificationRecipientResolver<E> {

  /**
   * 이벤트에 대응하는 알림 수신 대상 사용자 목록.
   *
   * @param event 도메인 이벤트 (record snapshot — 엔티티 참조 없음)
   * @return 알림을 받아야 하는 활성 사용자. 비어 있으면 fan-out 하지 않음. 결코 {@code null} 을 반환하지 않는다.
   */
  List<User> resolve(E event);
}

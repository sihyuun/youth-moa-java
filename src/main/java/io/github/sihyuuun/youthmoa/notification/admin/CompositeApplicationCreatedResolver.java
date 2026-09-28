package io.github.sihyuuun.youthmoa.notification.admin;

import io.github.sihyuuun.youthmoa.application.event.ApplicationCreatedEvent;
import io.github.sihyuuun.youthmoa.user.User;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * A7-createdBy-recipient (2026-09-28): NEW_APPLICATION 수신자 결정 Composite.
 *
 * <p>A7-watcher-ui (2026-09-28 · 확장): 3축 (B-2 + B-3-A + B-3-B) distinct union.
 *
 * <ul>
 *   <li>B-2 = {@link ApplicationCreatedRecipientResolver} — 프로그램 소속 센터 CENTER_ADMIN
 *   <li>B-3-A = {@link CreatedByRecipientResolver} — 프로그램 작성자
 *   <li>B-3-B = {@link WatcherRecipientResolver} — 프로그램을 지켜보기 등록한 admin
 * </ul>
 *
 * <p>{@link AdminNotificationEventListener} 는 본 Composite 만 참조. 하위 Resolver 는 독립 테스트 + 향후 축 재조합 여지를
 * 위해 유지.
 *
 * <h2>정책</h2>
 *
 * <ul>
 *   <li>Q-B3-3 A (distinct union) — {@link User#getId()} 기준 중복 제거. 여러 축이 같은 admin 을 반환해도 알림은 1건만 발행
 *   <li>Q-B3-6 A (축 균등) — 모두 알림. 우선순위/억제 없음
 *   <li>Q-A7W-5 A (삽입 순서) — B-2 → B-3-A → B-3-B. {@link LinkedHashMap} 가 순서 유지, 알림 발행 순서도 동일
 *   <li>{@link Primary} 지정 이유 — 기존 코드에서 {@code ApplicationCreatedRecipientResolver} 를 직접 주입하는
 *       리스너/테스트가 남아있어도 컨텍스트 유일성이 필요한 경우 Composite 를 우선하도록 함
 * </ul>
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class CompositeApplicationCreatedResolver
    implements NotificationRecipientResolver<ApplicationCreatedEvent> {

  private final ApplicationCreatedRecipientResolver centerAdminResolver;
  private final CreatedByRecipientResolver createdByResolver;
  private final WatcherRecipientResolver watcherResolver;

  @Override
  public List<User> resolve(ApplicationCreatedEvent event) {
    List<User> centerAdmins = safeResolve("B-2", () -> centerAdminResolver.resolve(event));
    List<User> creators = safeResolve("B-3-A", () -> createdByResolver.resolve(event));
    List<User> watchers = safeResolve("B-3-B", () -> watcherResolver.resolve(event));

    // Q-B3-3 A: distinct union by user.id. LinkedHashMap 으로 삽입 순서 유지.
    // Q-A7W-5 A: B-2 → B-3-A → B-3-B 순서 보장.
    Map<Long, User> unique = new LinkedHashMap<>();
    for (User u : centerAdmins) unique.putIfAbsent(u.getId(), u);
    for (User u : creators) unique.putIfAbsent(u.getId(), u);
    for (User u : watchers) unique.putIfAbsent(u.getId(), u);
    return new ArrayList<>(unique.values());
  }

  /**
   * 개별 Resolver 실패가 다른 축까지 잃게 하지 않도록 격리. Resolver 는 정상 상태에서 예외를 던지지 않지만 (DB 이슈 등) 방어적으로 감싼다. 리스너
   * 최상위 fallback 과 이중 방어.
   */
  private List<User> safeResolve(String axis, java.util.function.Supplier<List<User>> supplier) {
    try {
      List<User> result = supplier.get();
      return result == null ? List.of() : result;
    } catch (RuntimeException e) {
      log.error("[A7] NEW_APPLICATION Resolver 축 실패 axis={}", axis, e);
      return List.of();
    }
  }
}

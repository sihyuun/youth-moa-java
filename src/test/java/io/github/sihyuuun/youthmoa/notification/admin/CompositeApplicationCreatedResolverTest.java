package io.github.sihyuuun.youthmoa.notification.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.sihyuuun.youthmoa.application.event.ApplicationCreatedEvent;
import io.github.sihyuuun.youthmoa.user.User;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A7-createdBy-recipient (2026-09-28) + A7-watcher-ui (2026-09-28 · B-3-B 확장) — {@link
 * CompositeApplicationCreatedResolver} 단위 테스트.
 *
 * <p>3축 (B-2 + B-3-A + B-3-B) distinct union 을 스텁 Resolver 로 검증한다.
 *
 * <ul>
 *   <li>단일 축만 매칭 (각 3케이스)
 *   <li>겹침 (같은 user.id) → distinct 로 중복 제거 (Q-B3-3 A)
 *   <li>순서 보장 — B-2 → B-3-A → B-3-B (Q-A7W-5 A)
 *   <li>한 축이 예외를 던져도 다른 축 결과는 유지 (격리)
 * </ul>
 */
class CompositeApplicationCreatedResolverTest {

  private static final ApplicationCreatedEvent EVENT =
      new ApplicationCreatedEvent(1L, 2L, 100L, "제목", 10L);

  private static User user(long id) {
    User u = mock(User.class);
    when(u.getId()).thenReturn(id);
    return u;
  }

  private CompositeApplicationCreatedResolver newComposite(
      List<User> centerAdmins, List<User> creators, List<User> watchers) {
    ApplicationCreatedRecipientResolver centerAdminResolver =
        mock(ApplicationCreatedRecipientResolver.class);
    CreatedByRecipientResolver createdByResolver = mock(CreatedByRecipientResolver.class);
    WatcherRecipientResolver watcherResolver = mock(WatcherRecipientResolver.class);
    when(centerAdminResolver.resolve(EVENT)).thenReturn(centerAdmins);
    when(createdByResolver.resolve(EVENT)).thenReturn(creators);
    when(watcherResolver.resolve(EVENT)).thenReturn(watchers);
    return new CompositeApplicationCreatedResolver(
        centerAdminResolver, createdByResolver, watcherResolver);
  }

  @Test
  @DisplayName("B-2 만 매칭 시 B-2 결과 그대로 반환")
  void b2_only() {
    List<User> result =
        newComposite(List.of(user(1L), user(2L)), List.of(), List.of()).resolve(EVENT);
    assertThat(result).extracting(User::getId).containsExactly(1L, 2L);
  }

  @Test
  @DisplayName("B-3-A 만 매칭 시 (다른 센터 프로그램) B-3-A 결과 그대로 반환")
  void b3a_only() {
    List<User> result = newComposite(List.of(), List.of(user(42L)), List.of()).resolve(EVENT);
    assertThat(result).extracting(User::getId).containsExactly(42L);
  }

  @Test
  @DisplayName("B-3-B 만 매칭 시 (다른 센터 프로그램을 지켜보는 admin) B-3-B 결과 그대로 반환")
  void b3b_only() {
    User u77 = user(77L);
    User u78 = user(78L);
    List<User> result = newComposite(List.of(), List.of(), List.of(u77, u78)).resolve(EVENT);
    assertThat(result).extracting(User::getId).containsExactly(77L, 78L);
  }

  @Test
  @DisplayName("겹침 (center-admin ∩ createdBy) 은 distinct union 으로 1건만 발행 (Q-B3-3 A)")
  void distinct_union_when_overlap() {
    // user id 5 가 CENTER_ADMIN 이면서 동시에 프로그램 작성자.
    List<User> result =
        newComposite(List.of(user(5L), user(6L)), List.of(user(5L)), List.of()).resolve(EVENT);
    assertThat(result).extracting(User::getId).containsExactly(5L, 6L);
  }

  @Test
  @DisplayName("B-2 · B-3-A · B-3-B 3축 모두 겹칠 때 distinct union — 순서 = B-2, B-3-A(신규), B-3-B(신규)")
  void distinct_union_across_three_axes() {
    // 시나리오: user 1 = center-admin, user 2 = center-admin + watcher, user 3 = creator + watcher,
    //         user 4 = watcher only.
    User u1 = user(1L);
    User u2 = user(2L);
    User u3 = user(3L);
    User u4 = user(4L);
    User u2b = user(2L);
    User u3b = user(3L);
    List<User> result =
        newComposite(List.of(u1, u2), List.of(u3), List.of(u2b, u3b, u4)).resolve(EVENT);
    assertThat(result).extracting(User::getId).containsExactly(1L, 2L, 3L, 4L);
  }

  @Test
  @DisplayName("두 축이 모두 매칭 (겹침 없음) 시 B-2 순 → B-3-A 순 으로 배치")
  void order_center_admins_first() {
    List<User> result =
        newComposite(List.of(user(1L), user(2L)), List.of(user(99L)), List.of()).resolve(EVENT);
    assertThat(result).extracting(User::getId).containsExactly(1L, 2L, 99L);
  }

  @Test
  @DisplayName("3축 모두 매칭 (겹침 없음) 시 B-2 → B-3-A → B-3-B 순서 보장 (Q-A7W-5 A)")
  void order_three_axes_disjoint() {
    List<User> result =
        newComposite(List.of(user(1L)), List.of(user(2L)), List.of(user(3L))).resolve(EVENT);
    assertThat(result).extracting(User::getId).containsExactly(1L, 2L, 3L);
  }

  @Test
  @DisplayName("모두 빈 결과면 빈 리스트")
  void empty_when_all_empty() {
    assertThat(newComposite(List.of(), List.of(), List.of()).resolve(EVENT)).isEmpty();
  }

  @Test
  @DisplayName("한 축 (B-3-A) 이 예외를 던져도 다른 축 (B-2 · B-3-B) 결과는 유지 (격리)")
  void axis_failure_isolated() {
    // Mockito 함정 회피: user(1L) 을 when(...).thenReturn(...) 인자로 인라인 호출하면 stubbing 사이클이
    // 겹쳐 UnfinishedStubbingException 발생. helper 로 먼저 인스턴스화한 뒤 stubbing.
    User u1 = user(1L);
    User u9 = user(9L);
    List<User> centerAdmins = List.of(u1);
    List<User> watchers = List.of(u9);
    ApplicationCreatedRecipientResolver centerAdminResolver =
        mock(ApplicationCreatedRecipientResolver.class);
    CreatedByRecipientResolver createdByResolver = mock(CreatedByRecipientResolver.class);
    WatcherRecipientResolver watcherResolver = mock(WatcherRecipientResolver.class);
    when(centerAdminResolver.resolve(EVENT)).thenReturn(centerAdmins);
    doThrow(new RuntimeException("DB down")).when(createdByResolver).resolve(EVENT);
    when(watcherResolver.resolve(EVENT)).thenReturn(watchers);

    List<User> result =
        new CompositeApplicationCreatedResolver(
                centerAdminResolver, createdByResolver, watcherResolver)
            .resolve(EVENT);
    assertThat(result).extracting(User::getId).containsExactly(1L, 9L);
  }
}

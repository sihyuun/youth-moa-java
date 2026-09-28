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
 * A7-createdBy-recipient (2026-09-28) — {@link CompositeApplicationCreatedResolver} 단위 테스트.
 *
 * <p>B-2 + B-3-A 두 축의 distinct union 을 스텁 Resolver 로 검증한다.
 *
 * <ul>
 *   <li>B-2 만 매칭 → B-2 결과 그대로
 *   <li>B-3-A 만 매칭 → B-3-A 결과 그대로
 *   <li>겹침 (같은 user.id) → distinct 로 중복 제거 (Q-B3-3 A)
 *   <li>B-2 결과가 B-3-A 결과보다 먼저 배치 (순서 보장)
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
      List<User> centerAdmins, List<User> creators) {
    ApplicationCreatedRecipientResolver centerAdminResolver =
        mock(ApplicationCreatedRecipientResolver.class);
    CreatedByRecipientResolver createdByResolver = mock(CreatedByRecipientResolver.class);
    when(centerAdminResolver.resolve(EVENT)).thenReturn(centerAdmins);
    when(createdByResolver.resolve(EVENT)).thenReturn(creators);
    return new CompositeApplicationCreatedResolver(centerAdminResolver, createdByResolver);
  }

  @Test
  @DisplayName("B-2 만 매칭 시 B-2 결과 그대로 반환")
  void b2_only() {
    List<User> result = newComposite(List.of(user(1L), user(2L)), List.of()).resolve(EVENT);
    assertThat(result).extracting(User::getId).containsExactly(1L, 2L);
  }

  @Test
  @DisplayName("B-3-A 만 매칭 시 (다른 센터 프로그램) B-3-A 결과 그대로 반환")
  void b3a_only() {
    List<User> result = newComposite(List.of(), List.of(user(42L))).resolve(EVENT);
    assertThat(result).extracting(User::getId).containsExactly(42L);
  }

  @Test
  @DisplayName("겹침 (center-admin ∩ createdBy) 은 distinct union 으로 1건만 발행 (Q-B3-3 A)")
  void distinct_union_when_overlap() {
    // user id 5 가 CENTER_ADMIN 이면서 동시에 프로그램 작성자.
    List<User> result = newComposite(List.of(user(5L), user(6L)), List.of(user(5L))).resolve(EVENT);
    assertThat(result).extracting(User::getId).containsExactly(5L, 6L);
  }

  @Test
  @DisplayName("두 축이 모두 매칭 (겹침 없음) 시 B-2 순 → B-3-A 순 으로 배치")
  void order_center_admins_first() {
    List<User> result =
        newComposite(List.of(user(1L), user(2L)), List.of(user(99L))).resolve(EVENT);
    assertThat(result).extracting(User::getId).containsExactly(1L, 2L, 99L);
  }

  @Test
  @DisplayName("모두 빈 결과면 빈 리스트")
  void empty_when_both_empty() {
    assertThat(newComposite(List.of(), List.of()).resolve(EVENT)).isEmpty();
  }

  @Test
  @DisplayName("한 축 (B-3-A) 이 예외를 던져도 다른 축 (B-2) 결과는 유지 (격리)")
  void axis_failure_isolated() {
    // Mockito 함정 회피: user(1L) 을 when(...).thenReturn(...) 인자로 인라인 호출하면 stubbing 사이클이
    // 겹쳐 UnfinishedStubbingException 발생. helper 로 먼저 인스턴스화한 뒤 stubbing.
    User u1 = user(1L);
    List<User> centerAdmins = List.of(u1);
    ApplicationCreatedRecipientResolver centerAdminResolver =
        mock(ApplicationCreatedRecipientResolver.class);
    CreatedByRecipientResolver createdByResolver = mock(CreatedByRecipientResolver.class);
    when(centerAdminResolver.resolve(EVENT)).thenReturn(centerAdmins);
    doThrow(new RuntimeException("DB down")).when(createdByResolver).resolve(EVENT);

    List<User> result =
        new CompositeApplicationCreatedResolver(centerAdminResolver, createdByResolver)
            .resolve(EVENT);
    assertThat(result).extracting(User::getId).containsExactly(1L);
  }
}

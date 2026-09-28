package io.github.sihyuuun.youthmoa.notification.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.sihyuuun.youthmoa.application.event.ApplicationCreatedEvent;
import io.github.sihyuuun.youthmoa.notification.watch.ProgramWatchRepository;
import io.github.sihyuuun.youthmoa.user.User;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A7-watcher-ui (2026-09-28 · B-3-B) — {@link WatcherRecipientResolver} 단위 테스트.
 *
 * <p>Repository 를 스텁하여 4가지 시나리오를 검증한다:
 *
 * <ol>
 *   <li>활성 watcher N명 → 그대로 반환
 *   <li>비활성 admin 은 skip (Q-A7W-6 A) — Repository 쿼리에서 필터되므로 여기서는 결과만 검증
 *   <li>watcher 0명 → 빈 리스트
 *   <li>programId null → 빈 리스트 · repository 호출 안 함 (방어)
 * </ol>
 */
class WatcherRecipientResolverTest {

  private static ApplicationCreatedEvent eventOf(Long programId) {
    return new ApplicationCreatedEvent(1L, 2L, programId, "제목", 10L);
  }

  private static User user(long id) {
    User u = mock(User.class);
    when(u.getId()).thenReturn(id);
    return u;
  }

  @Test
  @DisplayName("활성 watcher N명 → 그대로 반환")
  void watchers_returned() {
    // Mockito 함정 회피: user() 인라인 호출은 UnfinishedStubbingException 유발. 헬퍼로 먼저 인스턴스화.
    User u10 = user(10L);
    User u20 = user(20L);
    ProgramWatchRepository repo = mock(ProgramWatchRepository.class);
    when(repo.findAdminsByProgramId(eq(100L))).thenReturn(List.of(u10, u20));

    WatcherRecipientResolver resolver = new WatcherRecipientResolver(repo);
    List<User> result = resolver.resolve(eventOf(100L));

    assertThat(result).extracting(User::getId).containsExactly(10L, 20L);
  }

  @Test
  @DisplayName("watcher 0명 → 빈 리스트 (Q-A7W-6 A · Repository 쿼리가 비활성 admin 을 이미 필터)")
  void no_watcher_returns_empty() {
    ProgramWatchRepository repo = mock(ProgramWatchRepository.class);
    when(repo.findAdminsByProgramId(eq(100L))).thenReturn(List.of());

    WatcherRecipientResolver resolver = new WatcherRecipientResolver(repo);
    assertThat(resolver.resolve(eventOf(100L))).isEmpty();
  }

  @Test
  @DisplayName("event.programId 가 null 이면 빈 리스트 + Repository 호출 없음 (방어)")
  void null_programId_short_circuits() {
    ProgramWatchRepository repo = mock(ProgramWatchRepository.class);

    WatcherRecipientResolver resolver = new WatcherRecipientResolver(repo);
    List<User> result = resolver.resolve(eventOf(null));

    assertThat(result).isEmpty();
    verifyNoInteractions(repo);
  }

  @Test
  @DisplayName("Q-A7W-7 A 정합: SUSPENDED 프로그램도 watcher 축은 그대로 발송 (Resolver 는 상태 필터 없음)")
  void suspended_program_still_notifies_watchers() {
    // Resolver 는 program 상태를 확인하지 않는다. Repository 도 program.isActive 필터 없이 admin 만 활성 체크.
    User u30 = user(30L);
    ProgramWatchRepository repo = mock(ProgramWatchRepository.class);
    when(repo.findAdminsByProgramId(eq(555L))).thenReturn(List.of(u30));

    WatcherRecipientResolver resolver = new WatcherRecipientResolver(repo);
    assertThat(resolver.resolve(eventOf(555L))).extracting(User::getId).containsExactly(30L);
  }
}

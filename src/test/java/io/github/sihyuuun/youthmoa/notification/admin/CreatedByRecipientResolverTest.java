package io.github.sihyuuun.youthmoa.notification.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.sihyuuun.youthmoa.application.event.ApplicationCreatedEvent;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A7-createdBy-recipient (2026-09-28 · B-3-A) — {@link CreatedByRecipientResolver} 단위 테스트.
 *
 * <p>Mockito 로 {@link ProgramRepository} + {@link Program} 을 스텁하여 4가지 시나리오를 검증한다:
 *
 * <ol>
 *   <li>활성 createdBy → 단건 반환
 *   <li>비활성 createdBy → 빈 리스트 (Q-B3-4 A)
 *   <li>프로그램 미존재 → 빈 리스트 (예외 전파 안 함)
 *   <li>programId null → 빈 리스트 (방어)
 * </ol>
 *
 * <p>실 통합 시나리오 (fan-out 결과 확인) 는 {@link AdminNotificationEventListenerTest} 담당.
 */
class CreatedByRecipientResolverTest {

  private static ApplicationCreatedEvent eventOf(Long programId) {
    return new ApplicationCreatedEvent(1L, 2L, programId, "제목", 10L);
  }

  private static User user(long id, UserRole role, boolean active) {
    User u = mock(User.class);
    when(u.getId()).thenReturn(id);
    when(u.getRole()).thenReturn(role);
    when(u.isActive()).thenReturn(active);
    return u;
  }

  @Test
  @DisplayName("활성 createdBy 는 단건 반환")
  void active_creator_returned() {
    ProgramRepository repo = mock(ProgramRepository.class);
    Program program = mock(Program.class);
    User creator = user(42L, UserRole.SYSTEM_ADMIN, true);
    when(program.getCreatedBy()).thenReturn(creator);
    when(repo.findById(eq(100L))).thenReturn(Optional.of(program));

    CreatedByRecipientResolver resolver = new CreatedByRecipientResolver(repo);
    List<User> result = resolver.resolve(eventOf(100L));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(42L);
  }

  @Test
  @DisplayName("비활성 createdBy 는 skip (Q-B3-4 A)")
  void inactive_creator_skipped() {
    ProgramRepository repo = mock(ProgramRepository.class);
    Program program = mock(Program.class);
    User creator = user(42L, UserRole.SYSTEM_ADMIN, false);
    when(program.getCreatedBy()).thenReturn(creator);
    when(repo.findById(eq(100L))).thenReturn(Optional.of(program));

    CreatedByRecipientResolver resolver = new CreatedByRecipientResolver(repo);
    List<User> result = resolver.resolve(eventOf(100L));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("프로그램 미존재 시 빈 리스트 반환 (예외 전파 X)")
  void missing_program_returns_empty() {
    ProgramRepository repo = mock(ProgramRepository.class);
    when(repo.findById(eq(999L))).thenReturn(Optional.empty());

    CreatedByRecipientResolver resolver = new CreatedByRecipientResolver(repo);
    List<User> result = resolver.resolve(eventOf(999L));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("event.programId 가 null 이면 빈 리스트 (방어)")
  void null_programId_returns_empty() {
    ProgramRepository repo = mock(ProgramRepository.class);

    CreatedByRecipientResolver resolver = new CreatedByRecipientResolver(repo);
    List<User> result = resolver.resolve(eventOf(null));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("createdBy 필드가 null 인 이상 상태에서도 빈 리스트 반환 (경고 로깅 후 skip)")
  void null_createdBy_returns_empty() {
    ProgramRepository repo = mock(ProgramRepository.class);
    Program program = mock(Program.class);
    when(program.getCreatedBy()).thenReturn(null);
    when(repo.findById(eq(100L))).thenReturn(Optional.of(program));

    CreatedByRecipientResolver resolver = new CreatedByRecipientResolver(repo);
    List<User> result = resolver.resolve(eventOf(100L));

    assertThat(result).isEmpty();
  }
}

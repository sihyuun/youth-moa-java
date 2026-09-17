package io.github.sihyuuun.youthmoa.notification.admin;

import io.github.sihyuuun.youthmoa.application.event.ApplicationCreatedEvent;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * A7 admin 헤더 알림 벨 QC (2026-09-17): NEW_APPLICATION 수신자 결정 전략 — B-1 구현.
 *
 * <p>{@code program.organization} 문자열이 {@code CENTER_ADMIN.center.name} 과 일치하는 활성 관리자에게만 fan-out.
 * SYSTEM_ADMIN 은 QC B-1 결정에 따라 알림에서 제외 (성수기 발송량 감소 + A6 대시보드로 커버).
 *
 * <h2>B-1 → B-3 로드맵</h2>
 *
 * <p>후속 티켓 {@code A7-createdBy-recipient} 에서 다음이 확장될 예정:
 *
 * <ul>
 *   <li>{@code Program.createdBy} FK 신설 + backfill
 *   <li>{@code CreatedByRecipientResolver} — createdBy 있으면 그 사용자 우선, 없으면 이 클래스로 fallback
 *   <li>{@code WatcherRecipientResolver} — {@code ProgramWatcher} M:N 엔티티 기반 선택 알림
 *   <li>{@code CompositeRecipientResolver} — 여러 resolver 결과를 합치기 (중복 제거)
 * </ul>
 *
 * <p>이번 티켓에선 인터페이스만 정의해 후속 확장 시 이 클래스 수정 없이 새 구현체를 추가할 수 있도록 준비.
 *
 * <h2>Program.organization 이 비어 있는 경우</h2>
 *
 * <p>{@code organization} 이 {@code null} 또는 blank 인 프로그램은 매칭 대상이 없어 빈 리스트 반환. 시드 데이터 중 organization
 * 이 채워지지 않은 프로그램이 있을 수 있으므로 방어적으로 처리한다.
 */
@Component
@RequiredArgsConstructor
public class ApplicationCreatedRecipientResolver
    implements NotificationRecipientResolver<ApplicationCreatedEvent> {

  private final UserRepository userRepository;

  @Override
  public List<User> resolve(ApplicationCreatedEvent event) {
    String organization = event.programOrganization();
    if (organization == null || organization.isBlank()) {
      return List.of();
    }
    return userRepository.findByRoleAndIsActiveTrueAndCenter_Name(
        UserRole.CENTER_ADMIN, organization);
  }
}

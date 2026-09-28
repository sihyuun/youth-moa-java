package io.github.sihyuuun.youthmoa.notification.admin;

import io.github.sihyuuun.youthmoa.application.event.ApplicationCreatedEvent;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * A7-createdBy-recipient (2026-09-28 · B-3-A): NEW_APPLICATION 수신자에 프로그램 작성자 축 추가.
 *
 * <p>{@link ApplicationCreatedRecipientResolver} (B-2 · center 소속 CENTER_ADMIN) 와 병행하여 {@link
 * CompositeApplicationCreatedResolver} 가 두 결과를 distinct union 한다.
 *
 * <h2>정책 (사용자 결정 Q-B3)</h2>
 *
 * <ul>
 *   <li>Q-B3-3 (distinct union) — 결과 조합은 Composite 가 담당, 본 Resolver 는 createdBy 축만 반환
 *   <li>Q-B3-4 (createdBy 비활성 → skip) — {@code user.isActive()==false} 면 빈 리스트
 *   <li>Q-B3-Δ (event 무변경) — programId 로 lazy fetch 하여 event record 확장 회피
 * </ul>
 *
 * <p>{@code programId} 로 조회 실패 시 (경합/삭제) 빈 리스트 반환하여 fan-out 을 건너뛴다. 예외 전파 시 리스너 최상위 fallback 이 삼키긴
 * 하지만 B-2 축까지 함께 잃게 되므로 여기서 격리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreatedByRecipientResolver
    implements NotificationRecipientResolver<ApplicationCreatedEvent> {

  private final ProgramRepository programRepository;

  @Override
  public List<User> resolve(ApplicationCreatedEvent event) {
    Long programId = event.programId();
    if (programId == null) {
      return List.of();
    }
    Program program = programRepository.findById(programId).orElse(null);
    if (program == null) {
      log.debug("[A7-B3A] createdBy resolve: 프로그램 조회 실패 programId={}", programId);
      return List.of();
    }
    User creator = program.getCreatedBy();
    if (creator == null) {
      // NOT NULL FK 이므로 정상 상태에서는 도달 불가. 마이그레이션 직후 등 이상 상태 방어.
      log.warn("[A7-B3A] program.createdBy null programId={}", programId);
      return List.of();
    }
    if (!creator.isActive()) {
      // Q-B3-4 A: 비활성 계정은 알림 skip.
      log.debug(
          "[A7-B3A] createdBy 비활성 skip programId={} creatorId={}", programId, creator.getId());
      return List.of();
    }
    return List.of(creator);
  }
}

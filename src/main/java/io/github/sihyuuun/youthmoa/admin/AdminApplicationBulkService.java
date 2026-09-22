package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.application.event.ApplicationApprovedEvent;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A8 admin-bulk (2026-09-17 · Qn-App1 A · deviation): 신청 bulk approve 서비스.
 *
 * <p>Reject bulk 는 이번 스코프 제외 (deferred: A8-reject-bulk) — 반려 사유 개별성·감사 정합 위해 A4 개별 flow 유지.
 *
 * <p>Qn-9 유지: 각 성공 건마다 {@link ApplicationApprovedEvent} 발송 → 사용자 알림 자동. A4 Application.approve 도메인
 * 메서드 무변경.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminApplicationBulkService {

  private final ApplicationRepository applicationRepository;
  private final ProgramRepository programRepository;
  private final UserRepository userRepository;
  private final AdminScope adminScope;
  private final ApplicationEventPublisher eventPublisher;
  private final PlatformTransactionManager transactionManager;

  private TransactionTemplate rowTx() {
    TransactionTemplate tx = new TransactionTemplate(transactionManager);
    tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return tx;
  }

  /** Bulk approve. 각 row 는 개별 트랜잭션. 성공 후 이벤트 발송. */
  public BulkResult bulkApprove(Long programId, List<Long> applicationIds, String adminEmail) {
    BulkResult result = new BulkResult();
    Program program =
        programRepository
            .findById(programId)
            .orElseThrow(() -> new IllegalArgumentException("프로그램을 찾을 수 없어요: " + programId));
    // A9-a: Center FK 기반 스코프 검증
    Long scopeId = adminScope.effectiveCenterId();
    if (scopeId != null) {
      Long pCenterId = program.getCenter() != null ? program.getCenter().getId() : null;
      if (!scopeId.equals(pCenterId)) {
        throw new IllegalStateException("자신의 센터 프로그램만 조작할 수 있어요.");
      }
    }
    User admin =
        userRepository
            .findByEmail(adminEmail)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없어요: " + adminEmail));
    if (applicationIds == null) return result;

    TransactionTemplate tx = rowTx();
    for (Long aid : applicationIds) {
      try {
        EventPayload payload =
            tx.execute(
                status -> {
                  Application app =
                      applicationRepository
                          .findById(aid)
                          .orElseThrow(() -> new IllegalArgumentException("신청을 찾을 수 없어요: " + aid));
                  if (!app.getProgram().getId().equals(programId)) {
                    throw new IllegalArgumentException("해당 프로그램의 신청이 아니에요.");
                  }
                  if (app.getStatus() == ApplicationStatus.APPROVED) {
                    return null; // idempotent - 이벤트 미발행
                  }
                  app.approve(admin);
                  return new EventPayload(
                      app.getId(),
                      app.getUser().getId(),
                      app.getProgram().getId(),
                      app.getProgram().getTitle());
                });
        result.addSuccess();
        if (payload != null) {
          eventPublisher.publishEvent(
              new ApplicationApprovedEvent(
                  payload.applicationId(),
                  payload.userId(),
                  payload.programId(),
                  payload.programTitle()));
        }
      } catch (IllegalArgumentException | IllegalStateException e) {
        log.info("[bulk-approve] skip id={} reason={}", aid, e.getMessage());
        result.addFailure(aid, e.getMessage());
      }
    }
    return result;
  }

  private record EventPayload(
      Long applicationId, Long userId, Long programId, String programTitle) {}
}

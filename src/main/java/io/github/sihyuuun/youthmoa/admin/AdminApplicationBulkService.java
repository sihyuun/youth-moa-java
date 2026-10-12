package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.application.WaitlistPromotionService;
import io.github.sihyuuun.youthmoa.application.event.ApplicationApprovedEvent;
import io.github.sihyuuun.youthmoa.application.event.ApplicationRejectedEvent;
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
 * A8 admin-bulk (2026-09-17 · Qn-App1 A · deviation) · A5-2 admin-approval-bulk (2026-10-12): 신청
 * bulk 처리 서비스.
 *
 * <ul>
 *   <li>Qn-9 — 각 성공 건마다 도메인 이벤트 발송 → 알림/메일 자동
 *   <li>A5-2 Q3 — bulk 최대 500건 (초과 시 IllegalArgumentException)
 *   <li>A5-2 Q4 — CENTER_ADMIN 지원 (effectiveCenterId 매칭)
 *   <li>A5-2 Q1 — reject 사유는 공통 1건 (모든 대상에 동일 reason)
 * </ul>
 *
 * <p>row 단위 트랜잭션(REQUIRES_NEW) — per-row 실패를 허용하고 BulkResult 로 집계.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminApplicationBulkService {

  /** A5-2 Q3: bulk 최대 처리 건수 하드 cap. 초과 시 처리 중단. */
  public static final int MAX_BULK_SIZE = 500;

  private final ApplicationRepository applicationRepository;
  private final ProgramRepository programRepository;
  private final UserRepository userRepository;
  private final AdminScope adminScope;
  private final ApplicationEventPublisher eventPublisher;
  private final PlatformTransactionManager transactionManager;
  private final WaitlistPromotionService waitlistPromotionService;

  private TransactionTemplate rowTx() {
    TransactionTemplate tx = new TransactionTemplate(transactionManager);
    tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return tx;
  }

  /** Bulk approve. 각 row 는 개별 트랜잭션. 성공 후 이벤트 발송. */
  public BulkResult bulkApprove(Long programId, List<Long> applicationIds, String adminEmail) {
    BulkResult result = new BulkResult();
    Program program = loadProgramInScope(programId);
    User admin = loadUser(adminEmail);
    if (applicationIds == null || applicationIds.isEmpty()) return result;
    assertSizeCap(applicationIds);

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
                      app.getProgram().getTitle(),
                      null);
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
    // program 참조 unused 경고 방지 — scope 검증은 loadProgramInScope 에서 끝났음
    log.debug("[bulk-approve] programId={} title={}", programId, program.getTitle());
    return result;
  }

  /**
   * A5-2 B: 대기 전체 승인. PENDING 신청 전부 ids 조회 후 {@link #bulkApprove} 로 위임. MAX_BULK_SIZE 초과 시 승인 실패.
   */
  public BulkResult bulkApproveAllPending(Long programId, String adminEmail) {
    loadProgramInScope(programId);
    List<Long> pendingIds =
        applicationRepository.findIdsByProgramIdAndStatus(programId, ApplicationStatus.PENDING);
    if (pendingIds.isEmpty()) {
      return new BulkResult();
    }
    return bulkApprove(programId, pendingIds, adminEmail);
  }

  /**
   * A5-2 A: Bulk reject. 공통 사유 1건을 모든 선택 신청에 적용. row 단위 tx + 이벤트 발송.
   *
   * <ul>
   *   <li>이미 REJECTED/CANCELLED 는 idempotent skip (성공으로 카운트, 이벤트 미발행)
   *   <li>APPROVED → REJECTED 전이 시 공석 승격 (A4 개별 reject 와 동일 방어)
   *   <li>reason 은 Controller 단에서 blank/길이 검증 완료 전제
   * </ul>
   */
  public BulkResult bulkReject(
      Long programId, List<Long> applicationIds, String adminEmail, String reason) {
    BulkResult result = new BulkResult();
    loadProgramInScope(programId);
    User admin = loadUser(adminEmail);
    if (applicationIds == null || applicationIds.isEmpty()) return result;
    assertSizeCap(applicationIds);
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("반려 사유를 입력해주세요.");
    }

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
                  if (app.getStatus() == ApplicationStatus.REJECTED
                      || app.getStatus() == ApplicationStatus.CANCELLED) {
                    return null; // idempotent - 이벤트 미발행
                  }
                  ApplicationStatus prev = app.getStatus();
                  app.reject(admin, reason);
                  return new EventPayload(
                      app.getId(),
                      app.getUser().getId(),
                      app.getProgram().getId(),
                      app.getProgram().getTitle(),
                      prev);
                });
        result.addSuccess();
        if (payload != null) {
          eventPublisher.publishEvent(
              new ApplicationRejectedEvent(
                  payload.applicationId(),
                  payload.userId(),
                  payload.programId(),
                  payload.programTitle(),
                  reason));
          // APPROVED → REJECTED 만 공석 발생 → 대기자 승격 (A4 개별 reject 와 동일)
          if (payload.prevStatus() == ApplicationStatus.APPROVED) {
            waitlistPromotionService.promoteIfEligible(programId);
          }
        }
      } catch (IllegalArgumentException | IllegalStateException e) {
        log.info("[bulk-reject] skip id={} reason={}", aid, e.getMessage());
        result.addFailure(aid, e.getMessage());
      }
    }
    return result;
  }

  // =============== 내부 헬퍼 ===============

  private Program loadProgramInScope(Long programId) {
    Program program =
        programRepository
            .findById(programId)
            .orElseThrow(() -> new IllegalArgumentException("프로그램을 찾을 수 없어요: " + programId));
    Long scopeId = adminScope.effectiveCenterId();
    if (scopeId != null) {
      Long pCenterId = program.getCenter() != null ? program.getCenter().getId() : null;
      if (!scopeId.equals(pCenterId)) {
        throw new IllegalStateException("자신의 센터 프로그램만 조작할 수 있어요.");
      }
    }
    return program;
  }

  private User loadUser(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없어요: " + email));
  }

  private static void assertSizeCap(List<Long> ids) {
    if (ids.size() > MAX_BULK_SIZE) {
      throw new IllegalArgumentException(
          "한 번에 " + MAX_BULK_SIZE + "건까지만 처리할 수 있어요. (요청: " + ids.size() + "건)");
    }
  }

  private record EventPayload(
      Long applicationId,
      Long userId,
      Long programId,
      String programTitle,
      ApplicationStatus prevStatus) {}
}

package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A8 admin-bulk (2026-09-17 · Qn-1 A): 프로그램 bulk action 서비스.
 *
 * <p>Qn-P1 결정: 프로그램 status 는 신청기간·정원 파생이라 UPDATE 불가 (prototype.tsx L507 명시). bulk publish/unpublish
 * 는 미도입. Bulk deactivate/reactivate 로 대체 (soft delete 정책).
 *
 * <p>CENTER_ADMIN 격리 (R6): AdminScope.effectiveCenterName() 이 null 이 아니면 organization 문자열 매칭으로 필터.
 * 하나라도 다른 센터 row 가 섞이면 개별 skip (per-row 정책 정합).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProgramBulkService {

  private final ProgramRepository programRepository;
  private final AdminScope adminScope;
  private final PlatformTransactionManager transactionManager;

  private TransactionTemplate rowTx() {
    TransactionTemplate tx = new TransactionTemplate(transactionManager);
    tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return tx;
  }

  /** Bulk soft delete (isActive=false → SUSPENDED). */
  public BulkResult bulkDeactivate(List<Long> ids) {
    BulkResult result = new BulkResult();
    if (ids == null) return result;
    Long scopeId = adminScope.effectiveCenterId();
    TransactionTemplate tx = rowTx();
    for (Long id : ids) {
      try {
        tx.executeWithoutResult(
            status -> {
              Program p = loadTarget(id);
              assertInScope(p, scopeId);
              if (!p.isActive()) return; // idempotent
              p.deactivate();
            });
        result.addSuccess();
      } catch (IllegalArgumentException | IllegalStateException e) {
        log.info("[bulk-program-deactivate] skip id={} reason={}", id, e.getMessage());
        result.addFailure(id, e.getMessage());
      }
    }
    return result;
  }

  /** Bulk 재활성화. */
  public BulkResult bulkReactivate(List<Long> ids) {
    BulkResult result = new BulkResult();
    if (ids == null) return result;
    Long scopeId = adminScope.effectiveCenterId();
    TransactionTemplate tx = rowTx();
    for (Long id : ids) {
      try {
        tx.executeWithoutResult(
            status -> {
              Program p = loadTarget(id);
              assertInScope(p, scopeId);
              if (p.isActive()) return; // idempotent
              p.activate();
            });
        result.addSuccess();
      } catch (IllegalArgumentException | IllegalStateException e) {
        log.info("[bulk-program-reactivate] skip id={} reason={}", id, e.getMessage());
        result.addFailure(id, e.getMessage());
      }
    }
    return result;
  }

  private Program loadTarget(Long id) {
    Optional<Program> p = programRepository.findById(id);
    return p.orElseThrow(() -> new IllegalArgumentException("프로그램을 찾을 수 없어요: " + id));
  }

  /** A9-a: Center FK 기반 스코프 검증. */
  private void assertInScope(Program p, Long scopeId) {
    if (scopeId == null) return;
    Long pCenterId = p.getCenter() != null ? p.getCenter().getId() : null;
    if (!scopeId.equals(pCenterId)) {
      throw new IllegalStateException("자신의 센터 프로그램만 조작할 수 있어요.");
    }
  }
}

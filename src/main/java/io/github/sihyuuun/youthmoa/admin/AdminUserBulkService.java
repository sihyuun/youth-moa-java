package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A8 admin-bulk (2026-09-17 · Qn-1 A · Qn-8 A): 사용자 bulk action 서비스.
 *
 * <p>정책:
 *
 * <ul>
 *   <li>Qn-1 A — per-row 트랜잭션 (부분 실패 허용). 각 row 는 {@link TransactionTemplate} 로 REQUIRES_NEW 트랜잭션에서
 *       실행되어 한 건의 예외가 다른 건에 전파되지 않음
 *   <li>Qn-8 A — {@link AdminUserSafeguard} 를 개별·bulk 재사용. 개별 endpoint 회귀 위험 방지
 *   <li>R1 회귀 방어 — {@link AdminUserService} 개별 메서드 (deactivate/reactivate/changeRole) 그대로 유지. bulk
 *       는 low-level 도메인 메서드를 직접 호출해 사이드이펙트를 통제
 * </ul>
 *
 * <p>구현 노트 — Spring self-invocation 우회: 같은 클래스 내 {@code @Transactional} 메서드 직접 호출은 프록시가 관여하지 않아
 * REQUIRES_NEW 가 작동하지 않는다. {@code TransactionTemplate} 은 코드 레벨에서 트랜잭션을 시작하므로 이 문제를 회피한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserBulkService {

  private final UserRepository userRepository;
  private final AdminUserSafeguard safeguard;
  private final PlatformTransactionManager transactionManager;

  private TransactionTemplate rowTx() {
    TransactionTemplate tx = new TransactionTemplate(transactionManager);
    tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return tx;
  }

  /**
   * Bulk deactivate — Qn-3 A(spec §4-1): reason 공통. 각 row 는 개별 트랜잭션.
   *
   * @return per-row 결과 리포트
   */
  public BulkResult bulkDeactivate(List<Long> ids, User admin, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("차단 사유를 입력해주세요.");
    }
    BulkResult result = new BulkResult();
    if (ids == null) return result;
    TransactionTemplate tx = rowTx();
    for (Long id : ids) {
      try {
        tx.executeWithoutResult(
            status -> {
              User target = loadTarget(id);
              safeguard.assertCanDeactivate(admin, target);
              if (!target.isActive()) return; // idempotent
              target.deactivate(admin, reason);
            });
        result.addSuccess();
      } catch (IllegalArgumentException | IllegalStateException e) {
        log.info("[bulk-deactivate] skip id={} reason={}", id, e.getMessage());
        result.addFailure(id, e.getMessage());
      }
    }
    return result;
  }

  /** Bulk reactivate. */
  public BulkResult bulkReactivate(List<Long> ids, User admin) {
    BulkResult result = new BulkResult();
    if (ids == null) return result;
    TransactionTemplate tx = rowTx();
    for (Long id : ids) {
      try {
        tx.executeWithoutResult(
            status -> {
              User target = loadTarget(id);
              safeguard.assertCanReactivate(admin, target);
              if (target.isActive()) return; // idempotent
              target.reactivate();
            });
        result.addSuccess();
      } catch (IllegalArgumentException | IllegalStateException e) {
        log.info("[bulk-reactivate] skip id={} reason={}", id, e.getMessage());
        result.addFailure(id, e.getMessage());
      }
    }
    return result;
  }

  /** Bulk role change — Qn-8 A: safeguard 3종 재적용. */
  public BulkResult bulkChangeRole(List<Long> ids, User admin, UserRole newRole) {
    if (newRole == null) {
      throw new IllegalArgumentException("변경할 권한을 선택해주세요.");
    }
    BulkResult result = new BulkResult();
    if (ids == null) return result;
    TransactionTemplate tx = rowTx();
    for (Long id : ids) {
      try {
        tx.executeWithoutResult(
            status -> {
              User target = loadTarget(id);
              if (target.getRole() == newRole) return; // idempotent
              safeguard.assertCanChangeRole(admin, target, newRole);
              target.promoteTo(newRole);
            });
        result.addSuccess();
      } catch (IllegalArgumentException | IllegalStateException e) {
        log.info("[bulk-role] skip id={} newRole={} reason={}", id, newRole, e.getMessage());
        result.addFailure(id, e.getMessage());
      }
    }
    return result;
  }

  private User loadTarget(Long id) {
    Optional<User> u = userRepository.findById(id);
    return u.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없어요: " + id));
  }
}

package io.github.sihyuuun.youthmoa.admin;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * A8 admin-bulk (2026-09-17): bulk action 실행 결과 리포트.
 *
 * <p>per-row 트랜잭션 (Qn-1 · POLICY.md P-BULK-1) 정책상 개별 실패를 허용하고 카운트·사유를 함께 flash 에 담는다.
 *
 * <ul>
 *   <li>{@code successCount} 성공 건 수
 *   <li>{@code failCount} 실패 건 수 (사유 없음 · 존재하지 않음 · idempotent skip 등 모두 실패로 세지 않음 — 예외로 실패한 것만)
 *   <li>{@code errors} 실패 사유 리스트 (최대 5건 표시 예정)
 * </ul>
 */
@Getter
public class BulkResult {

  private int successCount = 0;
  private int failCount = 0;
  private final List<FailureRow> errors = new ArrayList<>();

  public void addSuccess() {
    this.successCount++;
  }

  public void addFailure(Long id, String reason) {
    this.failCount++;
    this.errors.add(new FailureRow(id, reason));
  }

  public int getTotal() {
    return successCount + failCount;
  }

  public boolean hasFailure() {
    return failCount > 0;
  }

  /** 개별 실패 row 스냅샷. */
  public record FailureRow(Long id, String reason) {}
}

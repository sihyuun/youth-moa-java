package io.github.sihyuuun.youthmoa.program;

/**
 * A3-1 (2026-09-10 · Qn-Δ2 A): 프로그램 신청 승인 방식.
 *
 * <ul>
 *   <li>{@link #AUTO} — 신청 즉시 자동 승인 (실행 로직은 A4 스코프)
 *   <li>{@link #MANUAL} — 관리자가 신청 상세에서 수동 승인/반려 (기본값)
 * </ul>
 */
public enum ApprovalMode {
  AUTO("자동 승인"),
  MANUAL("수동 승인");

  private final String label;

  ApprovalMode(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}

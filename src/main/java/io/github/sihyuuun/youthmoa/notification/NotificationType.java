package io.github.sihyuuun.youthmoa.notification;

public enum NotificationType {
  APPLICATION_APPROVED,
  APPLICATION_REJECTED,
  APPLICATION_CANCELLED,
  WAITLIST_PROMOTED,
  PROGRAM_DEADLINE_NEAR,
  WELCOME;

  /**
   * 알림 종류별 톤 컬러 키.
   *
   * <p>템플릿에서 CSS 클래스 suffix 로 사용 — {@code notif-icon--success} 등. 값 종류: success / warning / error /
   * primary.
   *
   * <p>prototype.tsx line 292~295 tone 매핑:
   *
   * <ul>
   *   <li>APPROVED / PROMOTED → success
   *   <li>DEADLINE_NEAR → warning
   *   <li>REJECTED / CANCELLED → error
   *   <li>WELCOME / 기타 → primary
   * </ul>
   */
  public String getToneColor() {
    return switch (this) {
      case APPLICATION_APPROVED, WAITLIST_PROMOTED -> "success";
      case PROGRAM_DEADLINE_NEAR -> "warning";
      case APPLICATION_REJECTED, APPLICATION_CANCELLED -> "error";
      case WELCOME -> "primary";
    };
  }
}

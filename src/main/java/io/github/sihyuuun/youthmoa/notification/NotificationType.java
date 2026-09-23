package io.github.sihyuuun.youthmoa.notification;

public enum NotificationType {
  APPLICATION_APPROVED,
  APPLICATION_REJECTED,
  APPLICATION_CANCELLED,
  WAITLIST_PROMOTED,
  PROGRAM_DEADLINE_NEAR,
  WELCOME,

  // ── A7 admin 헤더 알림 벨 (2026-09-17) ─────────────────────────────────
  // admin 트랙 전용 이벤트 타입. Notification 엔티티는 user FK 기반 fan-out INSERT.
  // 사용자 알림 flow (PR #143) 완전 무영향 — 기존 값 6종은 그대로 유지.
  /** 사용자가 프로그램 신청 시 발행. 수신자: 해당 프로그램의 center.id 매칭 CENTER_ADMIN (A9-b · Qn-C B-3). */
  NEW_APPLICATION,
  /** 신규 회원가입 시 발행. 수신자: SYSTEM_ADMIN 전체 (Qn-E). */
  NEW_USER;

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
      case APPLICATION_APPROVED, WAITLIST_PROMOTED, NEW_USER -> "success";
      case PROGRAM_DEADLINE_NEAR -> "warning";
      case APPLICATION_REJECTED, APPLICATION_CANCELLED -> "error";
      case WELCOME, NEW_APPLICATION -> "primary";
    };
  }

  /**
   * 알림 원형 아이콘 안에 렌더할 lucide SVG fragment 이름.
   *
   * <p>prototype.tsx L314~319 NOTIF_ITEMS 매핑:
   *
   * <ul>
   *   <li>APPROVED → check
   *   <li>DEADLINE_NEAR → calendar
   *   <li>WELCOME (공지사항) / WAITLIST_PROMOTED (빈자리) → bell
   *   <li>REJECTED / CANCELLED → close
   * </ul>
   *
   * <p>icons.html fragment 이름과 정확히 일치해야 한다.
   */
  public String getIconName() {
    return switch (this) {
      case APPLICATION_APPROVED, NEW_USER -> "check";
      case PROGRAM_DEADLINE_NEAR -> "calendar";
      case WELCOME, WAITLIST_PROMOTED, NEW_APPLICATION -> "bell";
      case APPLICATION_REJECTED, APPLICATION_CANCELLED -> "close";
    };
  }
}

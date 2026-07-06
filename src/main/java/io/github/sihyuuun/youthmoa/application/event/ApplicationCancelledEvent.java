package io.github.sihyuuun.youthmoa.application.event;

/**
 * 신청 취소 도메인 이벤트 (신청자 본인이 취소).
 *
 * <p>D5: {@code cancelReason} 필드 추가. null 이면 이전 호출부 호환 (기존 취소 로직).
 */
public record ApplicationCancelledEvent(
    Long applicationId, Long userId, Long programId, String programTitle, String cancelReason) {

  /** D5 이전 호출부 호환용 오버로드. */
  public ApplicationCancelledEvent(
      Long applicationId, Long userId, Long programId, String programTitle) {
    this(applicationId, userId, programId, programTitle, null);
  }
}

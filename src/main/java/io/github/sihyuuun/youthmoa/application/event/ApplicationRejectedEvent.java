package io.github.sihyuuun.youthmoa.application.event;

/** 신청 반려 도메인 이벤트. 반려 사유 포함. */
public record ApplicationRejectedEvent(
    Long applicationId, Long userId, Long programId, String programTitle, String rejectReason) {}

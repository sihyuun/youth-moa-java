package io.github.sihyuuun.youthmoa.application.event;

/** 신청 취소 도메인 이벤트 (신청자 본인이 취소). */
public record ApplicationCancelledEvent(
    Long applicationId, Long userId, Long programId, String programTitle) {}

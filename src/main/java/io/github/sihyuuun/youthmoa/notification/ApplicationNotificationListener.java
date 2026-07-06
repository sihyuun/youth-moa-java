package io.github.sihyuuun.youthmoa.notification;

import io.github.sihyuuun.youthmoa.application.event.ApplicationApprovedEvent;
import io.github.sihyuuun.youthmoa.application.event.ApplicationCancelledEvent;
import io.github.sihyuuun.youthmoa.application.event.ApplicationRejectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 신청 상태 변경 도메인 이벤트를 구독해 알림을 생성한다.
 *
 * <p>{@code @TransactionalEventListener(phase = AFTER_COMMIT)} — 발행 트랜잭션이 실제 커밋된 이후에만 리스너가 실행된다. 신청
 * 상태 변경이 롤백되면 알림도 발행되지 않으므로 데이터 정합성이 보장된다.
 *
 * <p>알림 저장은 별도의 새 트랜잭션({@code REQUIRES_NEW}) 에서 수행한다. AFTER_COMMIT 시점엔 기존 트랜잭션이 이미 닫혀 있어 새 트랜잭션이
 * 필요하다. 알림 저장이 실패하더라도 원본 신청 상태 변경은 이미 커밋되어 있으므로 로그만 남긴다 (F2c 에서 outbox 로 신뢰성 강화 예정).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationNotificationListener {

  /** 반려 사유 message 최대 길이 — Notification.message 컬럼 length(500) 준수. */
  private static final int MESSAGE_MAX = 500;

  private final NotificationService notificationService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onApproved(ApplicationApprovedEvent event) {
    try {
      String message = String.format("'%s' 신청이 승인되었습니다.", event.programTitle());
      notificationService.create(
          event.userId(),
          NotificationType.APPLICATION_APPROVED,
          "신청이 승인되었습니다",
          truncate(message),
          "/apply/complete?applicationId=" + event.applicationId());
    } catch (RuntimeException e) {
      log.error("[F2b] APPROVED 알림 발행 실패 applicationId={}", event.applicationId(), e);
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onRejected(ApplicationRejectedEvent event) {
    try {
      String reason = event.rejectReason() != null ? event.rejectReason() : "";
      String message = String.format("'%s' 신청이 반려되었습니다. 사유: %s", event.programTitle(), reason);
      notificationService.create(
          event.userId(),
          NotificationType.APPLICATION_REJECTED,
          "신청이 반려되었습니다",
          truncate(message),
          "/programs/" + event.programId());
    } catch (RuntimeException e) {
      log.error("[F2b] REJECTED 알림 발행 실패 applicationId={}", event.applicationId(), e);
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onCancelled(ApplicationCancelledEvent event) {
    try {
      String message = String.format("'%s' 신청을 취소했습니다.", event.programTitle());
      notificationService.create(
          event.userId(),
          NotificationType.APPLICATION_CANCELLED,
          "신청이 취소되었습니다",
          truncate(message),
          "/programs/" + event.programId());
    } catch (RuntimeException e) {
      log.error("[F2b] CANCELLED 알림 발행 실패 applicationId={}", event.applicationId(), e);
    }
  }

  private static String truncate(String s) {
    if (s == null) return null;
    return s.length() <= MESSAGE_MAX ? s : s.substring(0, MESSAGE_MAX);
  }
}

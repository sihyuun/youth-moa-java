package io.github.sihyuuun.youthmoa.notification.admin;

import io.github.sihyuuun.youthmoa.application.event.ApplicationCreatedEvent;
import io.github.sihyuuun.youthmoa.notification.Notification;
import io.github.sihyuuun.youthmoa.notification.NotificationService;
import io.github.sihyuuun.youthmoa.notification.NotificationType;
import io.github.sihyuuun.youthmoa.notification.sse.NotificationBroadcastEvent;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.event.UserCreatedEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * A7 admin 헤더 알림 벨 (2026-09-17): admin 대상 도메인 이벤트 구독 → Notification fan-out INSERT.
 *
 * <p>기존 사용자 트랙 {@code ApplicationNotificationListener} 와 완전 분리된 admin 트랙 리스너. 두 리스너는 서로 다른 이벤트를
 * 구독하며 (사용자: Approved/Rejected/Cancelled, admin: Created/UserCreated) 서로의 flow 에 영향을 주지 않는다.
 *
 * <h2>트랜잭션 경계</h2>
 *
 * <ul>
 *   <li>{@code @TransactionalEventListener(AFTER_COMMIT)} — 발행 트랜잭션(apply/signUp)이 실제 커밋된 이후에만 실행.
 *       apply/signUp 롤백 시 알림 발행되지 않아 데이터 정합성 유지 (회귀 방어 3점 中 #2).
 *   <li>{@code @Transactional(REQUIRES_NEW)} — AFTER_COMMIT 시점엔 기존 트랜잭션이 닫혔으므로 새 트랜잭션에서 fan-out
 *       INSERT 수행.
 *   <li>fan-out 예외 격리 — Resolver 조회 또는 INSERT 실패 시 warn/error 로그만 남기고 삼킨다. apply/signUp 자체는 이미 커밋
 *       완료 상태이므로 원본 flow 는 영향 없음.
 * </ul>
 *
 * <h2>Resolver 위임</h2>
 *
 * <p>수신자 결정은 {@link NotificationRecipientResolver} 구현체에 위임. 이 클래스는 "이벤트 → 대상 조회 → 각자에게 INSERT"
 * 오케스트레이션만 담당한다. 후속 티켓에서 Resolver 만 교체·추가하면 정책 확장 가능.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminNotificationEventListener {

  /** Notification.message length(500) 준수. */
  private static final int MESSAGE_MAX = 500;

  private final NotificationService notificationService;

  /**
   * A7-createdBy-recipient (2026-09-28): B-2 (센터 CENTER_ADMIN) + B-3-A (프로그램 작성자) distinct union.
   * 기존 {@link ApplicationCreatedRecipientResolver} 는 Composite 의 축 하나로 유지되며 삭제하지 않는다 (독립 테스트 · 향후 축
   * 재조합 여지). 오케스트레이션 코드는 무변경.
   */
  private final CompositeApplicationCreatedResolver applicationCreatedResolver;

  private final UserCreatedRecipientResolver userCreatedResolver;

  /**
   * A7-realtime (2026-10-15): fan-out INSERT 완료 후 수신자별 SSE push 트리거. {@link
   * NotificationBroadcastEvent} 를 발행하면 {@link
   * io.github.sihyuuun.youthmoa.notification.sse.NotificationSseEventListener} 가 받아 활성 emitter 에게
   * badge(+item) 를 전송한다.
   */
  private final ApplicationEventPublisher publisher;

  /**
   * 신청 생성 시 (신규 or 재신청) admin 알림 발행. 수신자는 B-3 (A9-b): program.center.id 매칭 CENTER_ADMIN 만.
   *
   * <p>Qn-8 링크: {@code /admin/programs/{programId}/applications} (A4 신청 현황 화면).
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onApplicationCreated(ApplicationCreatedEvent event) {
    try {
      List<User> recipients = applicationCreatedResolver.resolve(event);
      if (recipients.isEmpty()) {
        log.debug(
            "[A7] NEW_APPLICATION 수신자 없음 applicationId={} centerId={}",
            event.applicationId(),
            event.centerId());
        return;
      }
      String title = "새 신청이 접수됐어요";
      String message = truncate(String.format("'%s' 프로그램에 새 신청이 들어왔어요.", event.programTitle()));
      String link = "/admin/programs/" + event.programId() + "/applications";
      // A7-rate-limit (2026-09-29): Q3 그룹기준 = type + sourceId. sourceId = programId.
      String dedupKey = "NEW_APPLICATION:" + event.programId();
      for (User admin : recipients) {
        // 개별 실패가 다른 수신자에게 전파되지 않도록 try 안에서 각각 저장.
        try {
          Notification saved =
              notificationService.createOrMerge(
                  admin.getId(), NotificationType.NEW_APPLICATION, dedupKey, title, message, link);
          publishBroadcast(admin.getId(), saved);
        } catch (RuntimeException e) {
          log.error(
              "[A7] NEW_APPLICATION 개별 INSERT 실패 adminId={} applicationId={}",
              admin.getId(),
              event.applicationId(),
              e);
        }
      }
    } catch (RuntimeException e) {
      // 최상위 fallback — Resolver 조회 자체가 실패한 경우 등. apply 트랜잭션은 이미 커밋 완료.
      log.error("[A7] NEW_APPLICATION fan-out 실패 applicationId={}", event.applicationId(), e);
    }
  }

  /**
   * 신규 회원가입 시 SYSTEM_ADMIN 대상 알림 발행.
   *
   * <p>Qn-9 링크: {@code /admin/users/{userId}} (A5 상세 화면).
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onUserCreated(UserCreatedEvent event) {
    try {
      List<User> recipients = userCreatedResolver.resolve(event);
      if (recipients.isEmpty()) {
        log.debug("[A7] NEW_USER 수신 SYSTEM_ADMIN 없음 userId={}", event.userId());
        return;
      }
      String title = "새 회원이 가입했어요";
      String message =
          truncate(String.format("%s(%s) 님이 회원가입 했어요.", event.userName(), event.userEmail()));
      String link = "/admin/users/" + event.userId();
      // A7-rate-limit (2026-09-29): 신규 가입은 프로그램 축이 없으므로 sourceId = "global".
      // 같은 window 안 여러 가입도 admin 인박스에서 1건으로 병합.
      String dedupKey = "NEW_USER:global";
      for (User admin : recipients) {
        try {
          Notification saved =
              notificationService.createOrMerge(
                  admin.getId(), NotificationType.NEW_USER, dedupKey, title, message, link);
          publishBroadcast(admin.getId(), saved);
        } catch (RuntimeException e) {
          log.error(
              "[A7] NEW_USER 개별 INSERT 실패 adminId={} userId={}", admin.getId(), event.userId(), e);
        }
      }
    } catch (RuntimeException e) {
      log.error("[A7] NEW_USER fan-out 실패 userId={}", event.userId(), e);
    }
  }

  /**
   * A7-realtime (2026-10-15): fan-out INSERT 완료 1건을 SSE push 로 중계.
   *
   * <p>Q2 결정: merged 여부는 {@code occurrenceCount > 1} 로 판정 (createOrMerge 가 신규 저장 시 count=1, 병합 시
   * count++ 후 반환). 발행 실패해도 fan-out 자체는 성공 처리 — publisher 는 비동기/동기 모두 가능하지만 여기선 Spring 기본 동기 모드이므로
   * Listener 예외는 try 로 삼킨다.
   */
  private void publishBroadcast(Long recipientUserId, Notification saved) {
    try {
      boolean merged = saved.getOccurrenceCount() > 1;
      publisher.publishEvent(new NotificationBroadcastEvent(recipientUserId, saved, merged));
    } catch (RuntimeException e) {
      log.warn(
          "[A7-sse] broadcast publish failed userId={} notificationId={}",
          recipientUserId,
          saved != null ? saved.getId() : null,
          e);
    }
  }

  private static String truncate(String s) {
    if (s == null) return null;
    return s.length() <= MESSAGE_MAX ? s : s.substring(0, MESSAGE_MAX);
  }
}

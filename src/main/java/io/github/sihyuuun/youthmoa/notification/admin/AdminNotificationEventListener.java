package io.github.sihyuuun.youthmoa.notification.admin;

import io.github.sihyuuun.youthmoa.application.event.ApplicationCreatedEvent;
import io.github.sihyuuun.youthmoa.notification.NotificationService;
import io.github.sihyuuun.youthmoa.notification.NotificationType;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.event.UserCreatedEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final ApplicationCreatedRecipientResolver applicationCreatedResolver;
  private final UserCreatedRecipientResolver userCreatedResolver;

  /**
   * 신청 생성 시 (신규 or 재신청) admin 알림 발행. 수신자는 B-1: program.organization 매칭 CENTER_ADMIN 만.
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
            "[A7] NEW_APPLICATION 수신자 없음 applicationId={} organization={}",
            event.applicationId(),
            event.programOrganization());
        return;
      }
      String title = "새 신청이 접수됐어요";
      String message = truncate(String.format("'%s' 프로그램에 새 신청이 들어왔어요.", event.programTitle()));
      String link = "/admin/programs/" + event.programId() + "/applications";
      for (User admin : recipients) {
        // 개별 실패가 다른 수신자에게 전파되지 않도록 try 안에서 각각 저장.
        try {
          notificationService.create(
              admin.getId(), NotificationType.NEW_APPLICATION, title, message, link);
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
      for (User admin : recipients) {
        try {
          notificationService.create(
              admin.getId(), NotificationType.NEW_USER, title, message, link);
        } catch (RuntimeException e) {
          log.error(
              "[A7] NEW_USER 개별 INSERT 실패 adminId={} userId={}", admin.getId(), event.userId(), e);
        }
      }
    } catch (RuntimeException e) {
      log.error("[A7] NEW_USER fan-out 실패 userId={}", event.userId(), e);
    }
  }

  private static String truncate(String s) {
    if (s == null) return null;
    return s.length() <= MESSAGE_MAX ? s : s.substring(0, MESSAGE_MAX);
  }
}

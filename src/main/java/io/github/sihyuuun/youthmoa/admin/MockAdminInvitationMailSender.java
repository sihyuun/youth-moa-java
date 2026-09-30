package io.github.sihyuuun.youthmoa.admin;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * A7 admin-invitation-mail (2026-09-30) — dev/e2e/unit test 용 Mock 구현체.
 *
 * <p>실 SMTP 미접속. 발송 정보만 INFO 로그로 남긴다. plain-text password 는 보안상 log 에도 마스킹 (앞 2자 + ***) — 로컬 로그가
 * 실수로 공유돼도 노출되지 않도록.
 *
 * <p>{@link #sendInvitation} · {@link #sendPasswordReset} 는 기본 성공 반환. E2E 에서 fallback (Q2 A) 경로를
 * 검증할 때는 {@link #setForceFail(boolean)} 로 강제 실패 모드를 on 하면 항상 실패 결과를 돌려준다 — 테스트 픽스처 endpoint {@code
 * POST /__test__/mail/force-fail?enabled=true|false} 로 토글한다. (M2 · 2026-09-30)
 *
 * <p>활성 조건 (M4 · 2026-09-30):
 *
 * <ul>
 *   <li>{@link Profile @Profile("!prod")} — production 프로파일에서는 이 bean 자체가 등록되지 않는다. {@code
 *       youthmoa.mail.mock=true} 를 prod 에 실수로 넣어도 부팅 시점에 "no AdminInvitationMailService bean" 으로
 *       명시적 실패 → 사고 원천 차단.
 *   <li>{@code youthmoa.mail.mock=true} (또는 미지정) — non-prod 에서 {@link
 *       SmtpAdminInvitationMailSender} 대신 이 mock 을 활성화.
 * </ul>
 */
@Slf4j
@Component
@Profile("!prod")
@ConditionalOnProperty(name = "youthmoa.mail.mock", havingValue = "true", matchIfMissing = true)
public class MockAdminInvitationMailSender implements AdminInvitationMailService {

  /**
   * M2 (2026-09-30 · A7 mail followup) — E2E fallback 경로 검증용 강제 실패 flag.
   *
   * <p>기본 false. {@code POST /__test__/mail/force-fail?enabled=true} 로 켠 뒤 초대/재발급을 트리거하면 {@link
   * MailDispatchResult#failure(String)} 을 돌려 컨트롤러가 fallback (password 카드 노출) 배너로 분기한다. 테스트 후 반드시
   * false 로 복구 (endpoint 가 idempotent 하므로 disable 호출로 정리).
   *
   * <p>AtomicBoolean 을 쓴 이유: mock sender 는 프로세스 내 단일 singleton bean 이지만 E2E 는 병렬 워커 없이 순차 실행이라
   * mutual exclusion 자체는 필요 없다. 다만 volatile 대신 AtomicBoolean 을 쓰면 API (get/set) 가 명시적이고 향후
   * fail-count 등 확장 여지가 남는다.
   */
  private final AtomicBoolean forceFail = new AtomicBoolean(false);

  /** E2E 픽스처에서 호출. true 로 켜면 이후 발송은 항상 실패 결과 반환. */
  public void setForceFail(boolean enabled) {
    forceFail.set(enabled);
    log.info("[MOCK MAIL] forceFail toggled → {}", enabled);
  }

  /** 현재 강제 실패 mode 상태 (테스트 assertion·디버깅용). */
  public boolean isForceFail() {
    return forceFail.get();
  }

  @Override
  public MailDispatchResult sendInvitation(
      String toEmail, String toName, String tempPassword, String centerName) {
    if (forceFail.get()) {
      log.info(
          "[MOCK-FORCE-FAIL] admin invitation → {} (name={}, centerName={}) — 강제 실패 반환",
          toEmail,
          toName,
          centerName == null ? "-" : centerName);
      return MailDispatchResult.failure("mock forced failure");
    }
    log.info(
        "[MOCK MAIL] admin invitation → {} (name={}, centerName={}, tempPassword={})",
        toEmail,
        toName,
        centerName == null ? "-" : centerName,
        maskPassword(tempPassword));
    return MailDispatchResult.success();
  }

  @Override
  public MailDispatchResult sendPasswordReset(
      String toEmail, String toName, String newPassword, String resetBy) {
    if (forceFail.get()) {
      log.info(
          "[MOCK-FORCE-FAIL] admin password reset → {} (name={}, resetBy={}) — 강제 실패 반환",
          toEmail,
          toName,
          resetBy == null ? "-" : resetBy);
      return MailDispatchResult.failure("mock forced failure");
    }
    log.info(
        "[MOCK MAIL] admin password reset → {} (name={}, resetBy={}, newPassword={})",
        toEmail,
        toName,
        resetBy == null ? "-" : resetBy,
        maskPassword(newPassword));
    return MailDispatchResult.success();
  }

  private static String maskPassword(String p) {
    if (p == null || p.length() < 2) return "***";
    return p.substring(0, 2) + "***";
  }
}

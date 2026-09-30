package io.github.sihyuuun.youthmoa.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * A7 admin-invitation-mail (2026-09-30) — dev/e2e/unit test 용 Mock 구현체.
 *
 * <p>실 SMTP 미접속. 발송 정보만 INFO 로그로 남긴다. plain-text password 는 보안상 log 에도 마스킹 (앞 2자 + ***) — 로컬 로그가
 * 실수로 공유돼도 노출되지 않도록.
 *
 * <p>{@link #sendInvitation} · {@link #sendPasswordReset} 는 항상 성공 반환. 실패 시나리오 (Q2 A fallback) 검증은
 * {@link SmtpAdminInvitationMailSender} 대상 통합 테스트에서 수행.
 *
 * <p>활성 조건: {@code youthmoa.mail.mock=true} 또는 미지정 시 기본값 (matchIfMissing=true) — 안전한 default.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "youthmoa.mail.mock", havingValue = "true", matchIfMissing = true)
public class MockAdminInvitationMailSender implements AdminInvitationMailService {

  @Override
  public MailDispatchResult sendInvitation(
      String toEmail, String toName, String tempPassword, String centerName) {
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

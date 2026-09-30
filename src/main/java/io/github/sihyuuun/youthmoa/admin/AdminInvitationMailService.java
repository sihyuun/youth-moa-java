package io.github.sihyuuun.youthmoa.admin;

/**
 * A7 admin-invitation-mail (2026-09-30) — 관리자 발급 · password 재발급 시 초대 메일 발송 추상화.
 *
 * <p>구현체 2종은 {@code @ConditionalOnProperty(youthmoa.mail.mock)} 로 스위칭 (CoolSMS 패턴 답습):
 *
 * <ul>
 *   <li>{@link SmtpAdminInvitationMailSender} — mock=false. Spring {@code JavaMailSender} +
 *       Thymeleaf mail template render → 실 SMTP 발송.
 *   <li>{@link MockAdminInvitationMailSender} — mock=true (dev/e2e/test). 실 발송 없이 INFO 로그만.
 * </ul>
 *
 * <p>인터페이스를 두는 이유: (1) 테스트에서 SMTP 없이 검증, (2) prod/local 인프라 차이를 서비스 코드에서 분리, (3) 후속 발송 채널 추가 시 확장
 * 지점 유지.
 */
public interface AdminInvitationMailService {

  /**
   * 신규 계정 발급 메일.
   *
   * <p>템플릿 변수 매핑 (admin-invitation.html):
   *
   * <ul>
   *   <li>{@code name} — toName
   *   <li>{@code loginId} — toEmail (템플릿 상 "아이디" row)
   *   <li>{@code centerName} — nullable. CENTER_ADMIN 이면 소속 센터명, 그 외 role 은 null → 소속 센터 row 숨김
   *   <li>{@code tempPassword} — plain-text
   *   <li>{@code loginUrl}, {@code supportEmail} — {@link AdminMailProperties} 에서 주입
   * </ul>
   *
   * @param toEmail 수신자 이메일 (== loginId)
   * @param toName 수신자 이름 (본문 인사말)
   * @param tempPassword plain-text 임시 비밀번호 (메일 본문 노출)
   * @param centerName 소속 센터명 (nullable · CENTER_ADMIN 만 노출)
   * @return {@link MailDispatchResult} — 발송 성공/실패. 예외를 throw 하지 않는 이유는 컨트롤러 fallback (Q2 A) 분기 때문.
   */
  MailDispatchResult sendInvitation(
      String toEmail, String toName, String tempPassword, String centerName);

  /**
   * 임시 비밀번호 재발급 메일.
   *
   * <p>템플릿 변수 매핑 (admin-password-reset.html):
   *
   * <ul>
   *   <li>{@code name} — toName
   *   <li>{@code loginId} — toEmail
   *   <li>{@code resetBy} — nullable. 재설정을 실행한 관리자 표시명 (없으면 "재설정한 관리자" row 숨김)
   *   <li>{@code newPassword} — plain-text (초대 flow 의 {@code tempPassword} 와 이름이 다름 · 템플릿 spec)
   *   <li>{@code loginUrl}, {@code supportEmail} — {@link AdminMailProperties} 에서 주입
   * </ul>
   *
   * @param toEmail 수신자
   * @param toName 수신자 이름
   * @param newPassword 새 임시 비밀번호
   * @param resetBy 재설정 실행 관리자 표시명 (nullable)
   */
  MailDispatchResult sendPasswordReset(
      String toEmail, String toName, String newPassword, String resetBy);
}

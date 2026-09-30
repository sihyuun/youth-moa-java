package io.github.sihyuuun.youthmoa.admin;

/**
 * A7 admin-invitation-mail (2026-09-30) — 메일 발송 결과.
 *
 * <p>Spec Q2 (A안 fallback): SMTP 발송 실패 시 계정 생성·password 재설정 자체는 성공 상태로 유지하고, plain-text password 를
 * flash 로 화면에 노출해 운영자가 직접 대상자에게 전달할 수 있게 한다. 이 결정을 위해 서비스는 "발송 성공/실패" 를 명시적으로 반환해야 하고, 컨트롤러가 flash
 * 분기를 판정한다.
 *
 * <p>{@code failureReason} 은 사용자 대면 노출 문구가 아니라 원인 진단용이다 (Controller 에서 "메일 발송 실패: <reason>" 형태로
 * 노출). SMTP 예외 stack trace 는 여기 포함하지 않고 log 로만 남긴다.
 *
 * @param sent true = SMTP 발송 성공 (수신 확인은 별개), false = 예외로 실패
 * @param failureReason sent=false 일 때만 non-null. 사용자에게 노출 가능한 짧은 사유
 */
public record MailDispatchResult(boolean sent, String failureReason) {

  public static MailDispatchResult success() {
    return new MailDispatchResult(true, null);
  }

  public static MailDispatchResult failure(String reason) {
    return new MailDispatchResult(false, reason);
  }
}

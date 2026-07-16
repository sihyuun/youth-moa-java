package io.github.sihyuuun.youthmoa.user.sms;

/**
 * F-signup-01: 휴대폰 인증 SMS 발송 추상화.
 *
 * <p>구현체는 두 가지: {@link CoolSmsSender} (실 발송, {@code youthmoa.coolsms.enabled=true}) 와 {@link
 * MockSmsSender} (로그만, 기본값). Spring 의 {@code @ConditionalOnProperty} 로 둘 중 하나만 활성.
 */
public interface SmsSender {

  /**
   * @param phone E.164 or 국내 형식 (숫자만, 10~11자리)
   * @param code 발송할 6자리 인증 코드
   */
  void send(String phone, String code);
}

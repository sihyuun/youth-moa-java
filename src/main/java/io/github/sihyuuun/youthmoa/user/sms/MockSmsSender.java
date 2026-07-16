package io.github.sihyuuun.youthmoa.user.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * F-signup-01: 개발/e2e 용 Mock SMS 발송기. 실 발송 없이 INFO 로그로만 남김.
 *
 * <p>PhoneVerificationService 는 mock 모드에서 항상 {@code mock-fixed-code} (=123456) 를 저장하므로 사용자가
 * 브라우저에서 어떤 번호로 인증 요청해도 코드 "123456" 을 입력하면 인증 통과한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(
    name = "youthmoa.coolsms.enabled",
    havingValue = "false",
    matchIfMissing = true)
public class MockSmsSender implements SmsSender {

  @Override
  public void send(String phone, String code) {
    log.info("[MOCK SMS] phone={} code={} (실 발송되지 않음)", phone, code);
  }
}

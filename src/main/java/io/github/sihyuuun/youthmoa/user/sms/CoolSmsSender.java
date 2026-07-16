package io.github.sihyuuun.youthmoa.user.sms;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * F-signup-01: CoolSMS 실 발송 구현체.
 *
 * <p>활성 조건: {@code youthmoa.coolsms.enabled=true} + api-key/api-secret/sender 지정.
 *
 * <p>발신번호 등록은 CoolSMS 콘솔에서 사용자가 직접 완료 후 sender 로 지정.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "youthmoa.coolsms.enabled", havingValue = "true")
public class CoolSmsSender implements SmsSender {

  private static final String API_URL = "https://api.coolsms.co.kr";

  @Value("${youthmoa.coolsms.api-key}")
  private String apiKey;

  @Value("${youthmoa.coolsms.api-secret}")
  private String apiSecret;

  @Value("${youthmoa.coolsms.sender}")
  private String sender;

  private DefaultMessageService messageService;

  @PostConstruct
  void init() {
    this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, API_URL);
    log.info("[CoolSMS] initialized (sender={})", sender);
  }

  @Override
  public void send(String phone, String code) {
    Message message = new Message();
    message.setFrom(sender);
    message.setTo(phone);
    message.setText("[청년모아] 인증번호 " + code + " 입니다. (유효시간 3분)");
    try {
      messageService.send(message);
      log.info("[CoolSMS] sent phone={}", phone);
    } catch (Exception e) {
      log.error("[CoolSMS] send failed phone={} : {}", phone, e.getMessage(), e);
      throw new IllegalStateException("SMS 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", e);
    }
  }
}

package io.github.sihyuuun.youthmoa.user;

import io.github.sihyuuun.youthmoa.user.sms.SmsSender;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * F-signup-01: 휴대폰 인증 코드 발송/검증.
 *
 * <p>정규화: 입력 phone 에서 숫자만 남김. 저장·조회·비교 모두 정규화된 값 기준.
 *
 * <p>Mock 모드({@code youthmoa.coolsms.enabled=false}) 에서는 발송 코드가 항상 {@code mock-fixed-code} (=123456)
 * 로 대체됨. verify 시 사용자가 어떤 임의 코드를 입력해도 mock 고정 코드가 저장돼 있으므로 "123456" 입력만 성공.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PhoneVerificationService {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final PhoneVerificationRepository repository;
  private final SmsSender smsSender;

  @Value("${youthmoa.coolsms.enabled:false}")
  private boolean coolsmsEnabled;

  @Value("${youthmoa.coolsms.mock-fixed-code:123456}")
  private String mockFixedCode;

  @Value("${youthmoa.coolsms.code-ttl-seconds:180}")
  private long codeTtlSeconds;

  @Value("${youthmoa.coolsms.max-attempts:5}")
  private int maxAttempts;

  /** 숫자만 남긴다. */
  public static String normalize(String raw) {
    if (raw == null) return "";
    return raw.replaceAll("\\D", "");
  }

  /**
   * 인증 코드 발송.
   *
   * <p>per-phone cooldown 제거 (2026-07-16, prototype UI 재디자인 정합). Spam 방어는 IP 기반 rate limiter
   * ({@link SmsRateLimiter}) 만으로 충분. VERIFIED 이후 재인증 흐름에서 즉시 재발송 가능해야 함.
   *
   * @throws IllegalArgumentException phone 형식 오류
   * @throws IllegalStateException SMS 발송 실패
   */
  public void sendCode(String phoneRaw) {
    String phone = normalize(phoneRaw);
    if (!phone.matches("^0\\d{9,10}$")) {
      throw new IllegalArgumentException("올바른 휴대폰 번호 형식이 아닙니다.");
    }
    LocalDateTime now = LocalDateTime.now();
    PhoneVerification pv = repository.findByPhone(phone).orElse(null);

    String code = coolsmsEnabled ? generateCode() : mockFixedCode;
    LocalDateTime expiresAt = now.plusSeconds(codeTtlSeconds);

    if (pv == null) {
      pv = PhoneVerification.builder().phone(phone).code(code).expiresAt(expiresAt).build();
      repository.save(pv);
    } else {
      pv.reissue(code, expiresAt);
    }
    smsSender.send(phone, code);
  }

  /**
   * 인증 코드 검증. 성공 시 verified=true 저장.
   *
   * @return true 성공. false 실패 (코드 불일치·만료·잠금).
   */
  public boolean verifyCode(String phoneRaw, String code) {
    String phone = normalize(phoneRaw);
    PhoneVerification pv = repository.findByPhone(phone).orElse(null);
    if (pv == null) return false;
    if (pv.isLocked(maxAttempts)) return false;
    if (pv.isExpired(LocalDateTime.now())) return false;

    if (!pv.getCode().equals(code)) {
      pv.incrementAttempts();
      return false;
    }
    pv.markVerified();
    return true;
  }

  private String generateCode() {
    return String.format("%06d", RANDOM.nextInt(1_000_000));
  }
}

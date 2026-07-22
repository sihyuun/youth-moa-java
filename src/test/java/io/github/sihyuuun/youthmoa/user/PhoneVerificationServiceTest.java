package io.github.sihyuuun.youthmoa.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.sihyuuun.youthmoa.user.sms.SmsSender;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F-signup-01 PhoneVerificationService 단위 테스트 (Mockito).
 *
 * <p>Repository / SmsSender 모두 Mockito mock. Service 내부 @Value 필드는 리플렉션으로 주입.
 */
class PhoneVerificationServiceTest {

  private PhoneVerificationRepository repository;
  private SmsSender smsSender;
  private PhoneVerificationService service;

  @BeforeEach
  void setUp() throws Exception {
    repository = mock(PhoneVerificationRepository.class);
    smsSender = mock(SmsSender.class);
    service = new PhoneVerificationService(repository, smsSender);
    inject("coolsmsEnabled", false);
    inject("mockFixedCode", "123456");
    inject("codeTtlSeconds", 180L);
    inject("maxAttempts", 5);

    // save 시 인자 그대로 반환하도록 (id 없어도 서비스가 참조 안 함)
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  private void inject(String fieldName, Object value) throws Exception {
    Field f = PhoneVerificationService.class.getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(service, value);
  }

  @Test
  @DisplayName("normalize: 하이픈·공백 제거하고 숫자만 남긴다")
  void normalize_숫자만_남긴다() {
    assertThat(PhoneVerificationService.normalize("010-1111-2222")).isEqualTo("01011112222");
    assertThat(PhoneVerificationService.normalize("010 1111 2222")).isEqualTo("01011112222");
    assertThat(PhoneVerificationService.normalize(null)).isEqualTo("");
  }

  @Test
  @DisplayName("sendCode: 새 번호 → save 호출 + smsSender 발송")
  void sendCode_새번호_저장_발송() {
    when(repository.findByPhone("01011112222")).thenReturn(Optional.empty());

    service.sendCode("010-1111-2222");

    verify(repository).save(any(PhoneVerification.class));
    verify(smsSender).send("01011112222", "123456");
  }

  @Test
  @DisplayName("sendCode: 잘못된 phone 형식 → IllegalArgumentException")
  void sendCode_형식오류_예외() {
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class, () -> service.sendCode("12345"));
  }

  @Test
  @DisplayName("sendCode: 기존 row 있으면 reissue (cooldown 없음 · attempts 리셋 · verified 리셋)")
  void sendCode_기존_row_reissue() {
    PhoneVerification existing =
        PhoneVerification.builder()
            .phone("01011112222")
            .code("111111")
            .expiresAt(LocalDateTime.now().plusMinutes(3))
            .build();
    // 기존 시도 흔적 시뮬레이션
    existing.incrementAttempts();
    existing.incrementAttempts();
    org.junit.jupiter.api.Assertions.assertEquals(2, existing.getAttempts());
    when(repository.findByPhone("01011112222")).thenReturn(Optional.of(existing));

    service.sendCode("01011112222");

    // reissue 이후 attempts 0 · verified false 리셋 확인 (회귀 방어)
    org.junit.jupiter.api.Assertions.assertEquals(
        0, existing.getAttempts(), "reissue 시 attempts 리셋");
    org.junit.jupiter.api.Assertions.assertFalse(existing.isVerified(), "reissue 시 verified 리셋");
    org.mockito.Mockito.verify(smsSender)
        .send(
            org.mockito.ArgumentMatchers.eq("01011112222"),
            org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  @DisplayName("verifyCode: 정상 코드 → true + verified 표시")
  void verifyCode_정상() {
    PhoneVerification pv =
        PhoneVerification.builder()
            .phone("01011112222")
            .code("123456")
            .expiresAt(LocalDateTime.now().plusMinutes(3))
            .build();
    when(repository.findByPhone("01011112222")).thenReturn(Optional.of(pv));

    boolean ok = service.verifyCode("010-1111-2222", "123456");

    assertThat(ok).isTrue();
    assertThat(pv.isVerified()).isTrue();
  }

  @Test
  @DisplayName("verifyCode: 코드 불일치 → false + attempts 증가")
  void verifyCode_불일치_attempts증가() {
    PhoneVerification pv =
        PhoneVerification.builder()
            .phone("01011112222")
            .code("123456")
            .expiresAt(LocalDateTime.now().plusMinutes(3))
            .build();
    when(repository.findByPhone("01011112222")).thenReturn(Optional.of(pv));

    boolean ok = service.verifyCode("01011112222", "999999");

    assertThat(ok).isFalse();
    assertThat(pv.getAttempts()).isEqualTo(1);
  }

  @Test
  @DisplayName("verifyCode: 만료 → false")
  void verifyCode_만료() {
    PhoneVerification pv =
        PhoneVerification.builder()
            .phone("01011112222")
            .code("123456")
            .expiresAt(LocalDateTime.now().minusMinutes(1))
            .build();
    when(repository.findByPhone("01011112222")).thenReturn(Optional.of(pv));

    assertThat(service.verifyCode("01011112222", "123456")).isFalse();
  }

  @Test
  @DisplayName("verifyCode: attempts >= maxAttempts → 잠금(false)")
  void verifyCode_잠금() throws Exception {
    PhoneVerification pv =
        PhoneVerification.builder()
            .phone("01011112222")
            .code("123456")
            .expiresAt(LocalDateTime.now().plusMinutes(3))
            .build();
    // attempts 강제 5
    Field af = PhoneVerification.class.getDeclaredField("attempts");
    af.setAccessible(true);
    af.setInt(pv, 5);
    when(repository.findByPhone("01011112222")).thenReturn(Optional.of(pv));

    assertThat(service.verifyCode("01011112222", "123456")).isFalse();
  }

  @Test
  @DisplayName("SmsRateLimiter: 1분 3회 초과 시 false")
  void rateLimiter_1분_3회() {
    SmsRateLimiter limiter = new SmsRateLimiter();
    assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
    assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
    assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
    assertThat(limiter.tryAcquire("1.1.1.1")).isFalse();
    // 다른 IP 는 독립
    assertThat(limiter.tryAcquire("2.2.2.2")).isTrue();
  }

  @Test
  @DisplayName("sendCode: mock 모드에서는 항상 mockFixedCode 저장")
  void sendCode_mock모드_고정코드() {
    when(repository.findByPhone("01033334444")).thenReturn(Optional.empty());
    doAnswer(inv -> null).when(smsSender).send(anyString(), anyString());

    service.sendCode("01033334444");

    verify(smsSender).send("01033334444", "123456");
  }
}

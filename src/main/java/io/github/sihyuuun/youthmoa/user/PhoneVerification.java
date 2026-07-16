package io.github.sihyuuun.youthmoa.user;

import io.github.sihyuuun.youthmoa.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * F-signup-01: 휴대폰 인증 코드 저장.
 *
 * <p>phone unique — 같은 번호로 재요청 시 기존 row 를 update (UPSERT 패턴). 요청 시 code / expiresAt 재설정 + attempts
 * = 0 + verified = false 로 리셋.
 */
@Getter
@Entity
@Table(name = "phone_verifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneVerification extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 20)
  private String phone;

  @Column(nullable = false, length = 6)
  private String code;

  @Column(nullable = false)
  private LocalDateTime expiresAt;

  @Column(nullable = false)
  private int attempts;

  @Column(nullable = false)
  private boolean verified;

  @Column private LocalDateTime verifiedAt;

  @Column private LocalDateTime lastSentAt;

  @Builder
  private PhoneVerification(String phone, String code, LocalDateTime expiresAt) {
    this.phone = phone;
    this.code = code;
    this.expiresAt = expiresAt;
    this.attempts = 0;
    this.verified = false;
    this.lastSentAt = LocalDateTime.now();
  }

  /** 재발송 시 코드/만료 갱신 + attempts 리셋 + verified 리셋. */
  public void reissue(String newCode, LocalDateTime newExpiresAt) {
    this.code = newCode;
    this.expiresAt = newExpiresAt;
    this.attempts = 0;
    this.verified = false;
    this.verifiedAt = null;
    this.lastSentAt = LocalDateTime.now();
  }

  public void incrementAttempts() {
    this.attempts++;
  }

  public void markVerified() {
    this.verified = true;
    this.verifiedAt = LocalDateTime.now();
  }

  public boolean isExpired(LocalDateTime now) {
    return expiresAt.isBefore(now);
  }

  public boolean isLocked(int maxAttempts) {
    return attempts >= maxAttempts;
  }
}

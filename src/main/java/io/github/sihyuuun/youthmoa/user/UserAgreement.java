package io.github.sihyuuun.youthmoa.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * F-signup-terms-agreement: 약관 동의 이력.
 *
 * <p>이력이므로 UPDATE 없이 INSERT 만 한다. 철회·재동의는 새 행으로 남긴다. {@code (user, term)} UNIQUE 제약을 걸지 않는다 — 감사·분쟁
 * 대응 목적. 조회 성능은 {@code idx_user_agreements_user_term} 복합 인덱스로 보완.
 */
@Getter
@Entity
@Table(
    name = "user_agreements",
    indexes = @Index(name = "idx_user_agreements_user_term", columnList = "user_id, term_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAgreement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "term_id", nullable = false)
  private Term term;

  /** 동의 당시 Term.version 스냅샷 — 약관 개정 후 재동의 필요 판정용. */
  @Column(nullable = false)
  private int agreedVersion;

  /** 선택 약관 철회 대비. 필수 약관은 항상 true (미동의 시 가입 자체가 실패). */
  @Column(nullable = false)
  private boolean agreed;

  @Column(nullable = false, updatable = false)
  private LocalDateTime agreedAt;

  @Builder
  private UserAgreement(
      User user, Term term, int agreedVersion, boolean agreed, LocalDateTime agreedAt) {
    this.user = user;
    this.term = term;
    this.agreedVersion = agreedVersion;
    this.agreed = agreed;
    this.agreedAt = agreedAt;
  }
}

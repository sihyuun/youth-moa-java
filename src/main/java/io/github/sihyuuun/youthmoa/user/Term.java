package io.github.sihyuuun.youthmoa.user;

import io.github.sihyuuun.youthmoa.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * F-signup-terms-agreement: 약관 정의 엔티티.
 *
 * <p>회원가입 폼에 노출되는 약관 목록의 진리 소스. {@code code} 로 조회하며 admin 이 CRUD 할 수 있어야 하므로 enum 이 아닌 문자열 컬럼으로
 * 유지한다. 본문은 {@code contentPath} 로 정적 템플릿을 참조 (Q2 결정: 경로 참조 유지).
 */
@Getter
@Entity
@Table(
    name = "terms",
    uniqueConstraints = @UniqueConstraint(name = "uk_terms_code", columnNames = "code"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Term extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** UNIQUE 식별자. 예: SERVICE / PRIVACY / MARKETING. code 로 폼 바인딩·조회. */
  @Column(nullable = false, length = 50)
  private String code;

  /** 폼 라벨 (예: 회원가입약관) */
  @Column(nullable = false, length = 100)
  private String title;

  /** 약관 본문 경로 (예: /terms). Q2 결정: 정적 템플릿 참조 유지. */
  @Column(nullable = false, length = 200)
  private String contentPath;

  /** 필수 동의 여부. false 면 UI 에 (선택) 라벨 (Q5). */
  @Column(nullable = false)
  private boolean required;

  /** 개정 시 증가. UserAgreement.agreedVersion 스냅샷과 대조해 재동의 필요 판정. */
  @Column(nullable = false)
  private int version;

  /** 폼 노출 순서. */
  @Column(nullable = false)
  private int sortOrder;

  /** 비활성 약관은 회원가입 폼에서 제외 (과거 이력은 보존). */
  @Column(nullable = false)
  private boolean isActive;

  @Builder
  private Term(
      String code,
      String title,
      String contentPath,
      boolean required,
      int version,
      int sortOrder,
      boolean isActive) {
    this.code = code;
    this.title = title;
    this.contentPath = contentPath;
    this.required = required;
    this.version = version;
    this.sortOrder = sortOrder;
    this.isActive = isActive;
  }
}

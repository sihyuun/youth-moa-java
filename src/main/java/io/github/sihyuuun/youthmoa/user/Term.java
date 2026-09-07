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

  /**
   * (Deprecated by A-admin-terms-crud Qn-2 B, 2026-09-04) 약관 본문 경로 (예: /terms). 초기
   * F-signup-terms-agreement Q2 결정으로 정적 템플릿 참조를 유지했으나, admin CRUD 실사용성을 위해 {@link #content} DB TEXT
   * 컬럼으로 이관. signup 화면은 이제 {@code content} 를 직접 렌더한다. 컬럼 자체는 하위 호환 · legacy fallback 용도로 유지.
   */
  @Column(nullable = false, length = 200)
  private String contentPath;

  /**
   * A-admin-terms-crud (Qn-2 B, 2026-09-04): 약관 본문 (HTML). admin form 에서 편집 · 저장 전 OWASP HTML
   * sanitizer 적용. signup 화면은 {@code th:utext} 로 직접 렌더.
   */
  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

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
      String content,
      boolean required,
      int version,
      int sortOrder,
      boolean isActive) {
    this.code = code;
    this.title = title;
    this.contentPath = contentPath;
    this.content = content;
    this.required = required;
    this.version = version;
    this.sortOrder = sortOrder;
    this.isActive = isActive;
  }

  /**
   * A-admin-terms-crud (Qn-5 A · Qn-6 A · Qn-9 A, 2026-09-04): 약관 편집 도메인 메서드.
   *
   * <p>code 는 편집 대상 아님 (Qn-9 readonly). {@code bumpVersion=true} 면 version+1. content 는 이미 sanitize
   * 된 문자열이 들어온다고 가정 (AdminTermService 가 저장 전 sanitize).
   */
  public void updateContent(
      String title,
      String contentPath,
      String content,
      boolean required,
      int sortOrder,
      boolean isActive,
      boolean bumpVersion) {
    this.title = title;
    this.contentPath = contentPath;
    this.content = content;
    this.required = required;
    this.sortOrder = sortOrder;
    this.isActive = isActive;
    if (bumpVersion) {
      this.version = this.version + 1;
    }
  }
}

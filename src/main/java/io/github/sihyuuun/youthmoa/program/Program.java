package io.github.sihyuuun.youthmoa.program;

import io.github.sihyuuun.youthmoa.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "program")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Program extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(nullable = false, length = 100)
  private String organization;

  // ⏸ Q2 결정 (2026-06-30) — 카테고리 4종 보류 동안 nullable 로 변경.
  //   살릴 경우 nullable=false 로 복귀.
  @Column(length = 50)
  private String category;

  @Column(length = 50)
  private String region;

  @Column(length = 500)
  private String imageUrl;

  @Lob
  @Column(nullable = false)
  private String content;

  /**
   * 260826 P9 후속: 통합 검색 대상용 요약 텍스트. content 는 @Lob → CLOB 이라 lower(CLOB) 불가. VARCHAR(300) summary 를
   * 별도 관리해 검색 매칭에만 사용 (화면 노출 X). NULL 허용. admin 트랙 도입 후 pre-persist 훅으로 content 자동 절단 예정.
   */
  @Column(length = 300)
  private String summary;

  /**
   * 자격요건 (연령/거주지/기타). F4 spec 에서 기존 @Lob requirements 를 @Embeddable 로 재편.
   *
   * <p>컬럼: eligibility_age / eligibility_region / eligibility_etc (모두 nullable — 결정 Q3-B, 화면에서 기본
   * 문구로 대체).
   */
  @Embedded private ProgramEligibility eligibility;

  private LocalDate startDate;

  private LocalDate endDate;

  @Column(length = 500)
  private String applyUrl;

  @Column(nullable = false)
  private boolean isActive;

  @Column private Integer capacity;

  @Builder
  private Program(
      String title,
      String organization,
      String category,
      String region,
      String imageUrl,
      String content,
      String summary,
      ProgramEligibility eligibility,
      LocalDate startDate,
      LocalDate endDate,
      String applyUrl,
      Boolean isActive,
      Integer capacity) {
    this.title = title;
    this.organization = organization;
    this.category = category;
    this.region = region;
    this.imageUrl = imageUrl;
    this.content = content;
    this.summary = summary;
    this.eligibility = eligibility;
    this.startDate = startDate;
    this.endDate = endDate;
    this.applyUrl = applyUrl;
    this.isActive = isActive != null ? isActive : true;
    this.capacity = capacity;
  }

  public void update(
      String title,
      String organization,
      String category,
      String region,
      String imageUrl,
      String content,
      String summary,
      ProgramEligibility eligibility,
      LocalDate startDate,
      LocalDate endDate,
      String applyUrl,
      Integer capacity) {
    this.title = title;
    this.organization = organization;
    this.category = category;
    this.region = region;
    this.imageUrl = imageUrl;
    this.content = content;
    this.summary = summary;
    this.eligibility = eligibility;
    this.startDate = startDate;
    this.endDate = endDate;
    this.applyUrl = applyUrl;
    this.capacity = capacity;
  }

  public void activate() {
    this.isActive = true;
  }

  public void deactivate() {
    this.isActive = false;
  }

  /**
   * 260826 P9 후속: summary 가 명시되지 않았으면 content 앞 300자로 자동 파생. DataInitializer 시드 · admin CRUD
   * 공용. @PrePersist / @PreUpdate 훅에서 자동 호출된다. 명시적 summary 값이 있으면 그 값 우선.
   */
  public void deriveSummaryFromContentIfMissing() {
    if (this.summary == null && this.content != null) {
      this.summary = this.content.length() > 300 ? this.content.substring(0, 300) : this.content;
    }
  }

  /**
   * 260826 P9 후속: 저장/갱신 시점에 summary 를 자동 파생. admin CRUD 든 시드든 이 훅으로 일관 처리. DataInitializer 의 명시적
   * forEach 는 훅이 있어도 안전 (idempotent). 두 층 안전망.
   */
  @PrePersist
  @PreUpdate
  private void prePersistOrUpdate() {
    deriveSummaryFromContentIfMissing();
  }

  public boolean hasCapacityLimit() {
    return capacity != null;
  }

  public ProgramStatus getStatus() {
    // F0f-fix-3 (2026-07-20): 4개 명시 상태. "마감(isFull)" 은 파생.
    //   isActive=false → SUSPENDED (운영중단, 관리자 강제, 복구 가능)
    //   endDate < today → ENDED (기간 만료, 자연 종료)
    //   today < startDate → UPCOMING
    //   그 외 → OPEN
    if (!isActive) return ProgramStatus.SUSPENDED;
    LocalDate today = LocalDate.now();
    if (startDate != null && today.isBefore(startDate)) return ProgramStatus.UPCOMING;
    if (endDate != null && today.isAfter(endDate)) return ProgramStatus.ENDED;
    return ProgramStatus.OPEN;
  }

  /** endDate까지 남은 일수. endDate가 없으면 -1, 이미 지났으면 음수. */
  public long getDaysUntilDeadline() {
    if (endDate == null) return -1;
    return ChronoUnit.DAYS.between(LocalDate.now(), endDate);
  }

  /**
   * D-day 표시 레이블 (예: "D-3", "D-DAY", "종료")
   *
   * <p>기간 만료(days &lt; 0)는 "종료" 로 표기. isFull(정원 100%) 은 별개 파생값이며 여기서 다루지 않음.
   */
  public String getDdayLabel() {
    long days = getDaysUntilDeadline();
    if (days == -1) return "";
    if (days < 0) return "종료";
    if (days == 0) return "D-DAY";
    return "D-" + days;
  }
}

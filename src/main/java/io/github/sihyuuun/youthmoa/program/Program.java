package io.github.sihyuuun.youthmoa.program;

import io.github.sihyuuun.youthmoa.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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

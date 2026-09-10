package io.github.sihyuuun.youthmoa.program;

import io.github.sihyuuun.youthmoa.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

  /**
   * 260826 chore: @Lob → @JdbcTypeCode(LONGVARCHAR). PG 는 text · H2 는 VARCHAR(MAX) 매핑으로 Hibernate 6
   * SQM 이 STRING 확정 → lower/upper/substring 표준 문자열 함수 사용 가능. LONG32VARCHAR 는 SQM 이 STRING 아닌 별도
   * 타입으로 취급해 lower(LONG32VARCHAR) 실패 (실측). LONGVARCHAR 는 String 계열.
   */
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
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

  // ============== A3-1 admin-program-form (2026-09-10 · V12) ==============

  /** 신청 시작일 (nullable — 기존 시드 및 관리자 미입력 프로그램은 null 허용). */
  @Column(name = "apply_start_date")
  private LocalDate applyStartDate;

  /** 신청 마감일. */
  @Column(name = "apply_end_date")
  private LocalDate applyEndDate;

  /** 진행 장소. */
  @Column(length = 200)
  private String venue;

  /** 문의처. */
  @Column(length = 100)
  private String contact;

  /** 신청 승인 방식. NOT NULL DEFAULT MANUAL (V12). 실 실행 로직 (AUTO 즉시 승인) 은 A4 스코프. */
  @Enumerated(EnumType.STRING)
  @Column(name = "approval_mode", nullable = false, length = 10)
  private ApprovalMode approvalMode;

  /** 이용 약관 내용 (nullable). */
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "terms_service")
  private String termsService;

  /** 개인정보 처리방침 내용 (nullable). */
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "terms_privacy")
  private String termsPrivacy;

  /** 마케팅 수신 약관 내용 (nullable). */
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "terms_marketing")
  private String termsMarketing;

  /** 프로그램 짧은 설명 (prototype "프로그램 설명" 필드). */
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  private String description;

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
      Integer capacity,
      LocalDate applyStartDate,
      LocalDate applyEndDate,
      String venue,
      String contact,
      ApprovalMode approvalMode,
      String termsService,
      String termsPrivacy,
      String termsMarketing,
      String description) {
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
    this.applyStartDate = applyStartDate;
    this.applyEndDate = applyEndDate;
    this.venue = venue;
    this.contact = contact;
    this.approvalMode = approvalMode != null ? approvalMode : ApprovalMode.MANUAL;
    this.termsService = termsService;
    this.termsPrivacy = termsPrivacy;
    this.termsMarketing = termsMarketing;
    this.description = description;
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

  /**
   * A3-1 admin-program-form (2026-09-10): 관리자 3탭 폼 저장. 신설 컬럼 8종 함께 갱신. isActive 도 수동 편집 가능 (Qn-Δ6
   * A).
   */
  public void updateFromAdminForm(
      String title,
      String organization,
      String category,
      String region,
      String imageUrl,
      String description,
      String content,
      LocalDate startDate,
      LocalDate endDate,
      LocalDate applyStartDate,
      LocalDate applyEndDate,
      String venue,
      String contact,
      Integer capacity,
      ApprovalMode approvalMode,
      String termsService,
      String termsPrivacy,
      String termsMarketing,
      boolean isActive) {
    this.title = title;
    this.organization = organization;
    this.category = category;
    this.region = region;
    this.imageUrl = imageUrl;
    this.description = description;
    this.content = content;
    this.startDate = startDate;
    this.endDate = endDate;
    this.applyStartDate = applyStartDate;
    this.applyEndDate = applyEndDate;
    this.venue = venue;
    this.contact = contact;
    this.capacity = capacity;
    this.approvalMode = approvalMode != null ? approvalMode : ApprovalMode.MANUAL;
    this.termsService = termsService;
    this.termsPrivacy = termsPrivacy;
    this.termsMarketing = termsMarketing;
    this.isActive = isActive;
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

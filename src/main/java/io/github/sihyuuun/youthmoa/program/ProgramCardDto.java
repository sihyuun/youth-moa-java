package io.github.sihyuuun.youthmoa.program;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import lombok.Getter;

/**
 * 프로그램 카드 표시용 DTO. Program 엔티티에 실시간 신청자 수(applicantCount)를 조합한 뷰 모델.
 *
 * <p>CapacityBar (prototype.tsx L204~228 매칭) 2-line 레이아웃:
 *
 * <pre>
 * [primaryLabel (colorClass 색)]   [secondaryLabel (textTri)]
 * [━━━━━━ bar (colorClass 색, pct 폭) ━━━━━━]
 * </pre>
 *
 * <p>상태별 라벨 매핑:
 *
 * <ul>
 *   <li>UPCOMING → primary="신청 오픈 예정", secondary="MM/dd 오픈" (startDate 기준, Q1=b)
 *   <li>ENDED / full(pct≥100) → primary="모집 마감", secondary="100%"
 *   <li>OPEN (capacity 있음) → primary="정원 N/M명", secondary="N%", color 임계(70/90) 반영
 *   <li>OPEN (capacity=null) → primary="모집중", secondary=null, bar 미표시
 * </ul>
 */
@Getter
public class ProgramCardDto {

  private static final DateTimeFormatter OPEN_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd");

  private final Program program;
  private final long applicantCount;

  private final int pct;
  private final String colorClass;
  private final String primaryLabel;
  private final String secondaryLabel;

  /** 카드 기간 라벨. prototype 매칭: 같은 해 `2024-08-01~08-31`, 다른 해 `2024-12-25~2025-01-15`. */
  private final String dateLabel;

  // ─── 상세 페이지 전용 CapacityBar (prototype L945~951 매칭) ─────────
  /** 상세 화면 강조 라벨 예: "신청 오픈까지 5일" / "모집중 · 마감까지 3일" / "모집 마감" */
  private final String detailHeadline;

  /** 상세 화면 부가 안내 문구 예: "현재 신청률 30% · 경쟁률 0.3:1" */
  private final String detailSubtext;

  /**
   * 상세 배경 강조 여부. true = primaryBg 강조 / false = borderLight (마감·중단 케이스).
   *
   * <p>prototype L945 background 분기 매칭.
   */
  private final boolean detailEmphasized;

  // ─── 카드 CTA 5분기 (F0f-fix-1, spec §PR1 · C-Q1 도입, C-Q7=a 수동/기간 마감 분리) ───
  /** CTA 타입: apply / openAlert / waitlist / expired / inactive */
  private final String ctaType;

  /** CTA 라벨 텍스트 */
  private final String ctaLabel;

  /** CTA color variant: primary / secondary / muted */
  private final String ctaColorClass;

  /** CTA 아이콘 fragment 이름: check / bell / null */
  private final String ctaIcon;

  /** CTA disabled 여부 (expired / inactive) */
  private final boolean ctaDisabled;

  public ProgramCardDto(Program program, long applicantCount) {
    this.program = program;
    this.applicantCount = applicantCount;

    ProgramStatus status = program.getStatus();
    Integer capacity = program.getCapacity();

    // 기간 라벨 (prototype 매칭)
    if (program.getStartDate() == null || program.getEndDate() == null) {
      this.dateLabel = null;
    } else if (program.getStartDate().getYear() == program.getEndDate().getYear()) {
      this.dateLabel =
          program.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
              + "~"
              + program.getEndDate().format(DateTimeFormatter.ofPattern("MM-dd"));
    } else {
      this.dateLabel =
          program.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
              + "~"
              + program.getEndDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    if (status == ProgramStatus.UPCOMING) {
      this.pct = 0;
      this.colorClass = "secondary";
      this.primaryLabel = "신청 오픈 예정";
      this.secondaryLabel =
          program.getStartDate() != null
              ? program.getStartDate().format(OPEN_DATE_FORMAT) + " 오픈"
              : null;
    } else if (status == ProgramStatus.ENDED) {
      this.pct = 100;
      this.colorClass = "muted";
      this.primaryLabel = "모집 마감";
      this.secondaryLabel = "100%";
    } else if (capacity == null || capacity == 0) {
      // OPEN + 정원 제한 없음 → bar 미표시 (showBar=false)
      this.pct = 0;
      this.colorClass = "primary";
      this.primaryLabel = "모집중";
      this.secondaryLabel = null;
    } else {
      double ratio = (double) applicantCount / capacity;
      this.pct = Math.min(100, (int) Math.round(ratio * 100));
      if (applicantCount >= capacity) {
        // OPEN 상태이지만 정원 100% 달성 → 마감 취급 (파생 isFull)
        this.colorClass = "muted";
        this.primaryLabel = "모집 마감";
        this.secondaryLabel = "100%";
      } else if (ratio >= 0.9) {
        this.colorClass = "error";
        this.primaryLabel = "정원 " + applicantCount + "/" + capacity + "명";
        this.secondaryLabel = this.pct + "%";
      } else if (ratio >= 0.7) {
        this.colorClass = "warning";
        this.primaryLabel = "정원 " + applicantCount + "/" + capacity + "명";
        this.secondaryLabel = this.pct + "%";
      } else {
        this.colorClass = "primary";
        this.primaryLabel = "정원 " + applicantCount + "/" + capacity + "명";
        this.secondaryLabel = this.pct + "%";
      }
    }

    // ── 상세 페이지 전용 라벨 (prototype L947~951) ──
    LocalDate today = LocalDate.now();
    boolean full =
        (status != ProgramStatus.UPCOMING)
            && capacity != null
            && capacity > 0
            && applicantCount >= capacity;
    boolean closedByDate = (status == ProgramStatus.ENDED);

    if (status == ProgramStatus.UPCOMING && program.getStartDate() != null) {
      long daysUntilOpen = ChronoUnit.DAYS.between(today, program.getStartDate());
      this.detailHeadline = "신청 오픈까지 " + daysUntilOpen + "일";
      this.detailSubtext = "오픈 알림을 신청하면 시작 시 알려드려요.";
      this.detailEmphasized = true;
    } else if (full || closedByDate) {
      this.detailHeadline = "모집 마감";
      this.detailSubtext = "정원이 마감되었습니다. 알림을 신청하면 빈자리가 생길 시 알려드려요.";
      this.detailEmphasized = false;
    } else if (capacity == null || capacity == 0) {
      this.detailHeadline = "모집중";
      this.detailSubtext = "정원 제한 없이 신청 가능합니다.";
      this.detailEmphasized = true;
    } else {
      // OPEN with capacity — 마감까지 N일 + 신청률 · 경쟁률
      long daysUntilDeadline =
          program.getEndDate() != null ? ChronoUnit.DAYS.between(today, program.getEndDate()) : -1;
      String stateWord;
      if ("error".equals(this.colorClass)) stateWord = "마감임박";
      else if ("warning".equals(this.colorClass)) stateWord = "서두르세요";
      else stateWord = "모집중";
      String deadlineText;
      if (daysUntilDeadline < 0) deadlineText = "";
      else if (daysUntilDeadline == 0) deadlineText = " · 오늘 마감";
      else deadlineText = " · 마감까지 " + daysUntilDeadline + "일";
      this.detailHeadline = stateWord + deadlineText;
      double ratio = (double) applicantCount / capacity;
      double competition = Math.round(ratio * 10.0) / 10.0;
      this.detailSubtext = "현재 신청률 " + this.pct + "% · 경쟁률 " + competition + ":1";
      this.detailEmphasized = true;
    }

    // ── CTA 5분기 (F0f-fix-3, 2026-07-20 정책 확정) ────────────
    //   SUSPENDED       → inactive (disabled, "운영이 중단되었어요")
    //   UPCOMING        → openAlert (secondary bell, "오픈 알림 받기")
    //   ENDED (기간 만료) → expired  (disabled 회색, "비슷한 프로그램 보기")
    //   OPEN + full     → waitlist (muted bell, "빈자리 알림 받기")
    //   OPEN            → apply    (primary check, "신청하기")
    boolean ctaFull = capacity != null && capacity > 0 && applicantCount >= capacity;
    if (status == ProgramStatus.SUSPENDED) {
      this.ctaType = "inactive";
      this.ctaLabel = "운영이 중단되었어요";
      this.ctaColorClass = "muted";
      this.ctaIcon = null;
      this.ctaDisabled = true;
    } else if (status == ProgramStatus.UPCOMING) {
      this.ctaType = "openAlert";
      this.ctaLabel = "오픈 알림 받기";
      this.ctaColorClass = "secondary";
      this.ctaIcon = "bell";
      this.ctaDisabled = false;
    } else if (status == ProgramStatus.ENDED && !ctaFull) {
      this.ctaType = "expired";
      this.ctaLabel = "비슷한 프로그램 보기";
      this.ctaColorClass = "muted";
      this.ctaIcon = null;
      this.ctaDisabled = true;
    } else if (ctaFull) {
      this.ctaType = "waitlist";
      this.ctaLabel = "빈자리 알림 받기";
      this.ctaColorClass = "muted";
      this.ctaIcon = "bell";
      this.ctaDisabled = false;
    } else {
      this.ctaType = "apply";
      this.ctaLabel = "신청하기";
      this.ctaColorClass = "primary";
      this.ctaIcon = "check";
      this.ctaDisabled = false;
    }
  }

  // Program 위임 접근자
  public Long getId() {
    return program.getId();
  }

  public String getTitle() {
    return program.getTitle();
  }

  public String getOrganization() {
    return program.getOrganization();
  }

  public String getRegion() {
    return program.getRegion();
  }

  public String getImageUrl() {
    return program.getImageUrl();
  }

  public Integer getCapacity() {
    return program.getCapacity();
  }

  public ProgramStatus getStatus() {
    return program.getStatus();
  }

  public String getDdayLabel() {
    return program.getDdayLabel();
  }

  public long getDaysUntilDeadline() {
    return program.getDaysUntilDeadline();
  }

  public java.time.LocalDate getStartDate() {
    return program.getStartDate();
  }

  public java.time.LocalDate getEndDate() {
    return program.getEndDate();
  }
}

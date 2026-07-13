package io.github.sihyuuun.youthmoa.program;

import java.time.format.DateTimeFormatter;
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
 *   <li>CLOSED / full(pct≥100) → primary="모집 마감", secondary="100%"
 *   <li>ACTIVE (capacity 있음) → primary="정원 N/M명", secondary="N%", color 임계(70/90) 반영
 *   <li>ACTIVE (capacity=null) → primary="모집중", secondary=null, bar 미표시
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
    } else if (status == ProgramStatus.CLOSED) {
      this.pct = 100;
      this.colorClass = "muted";
      this.primaryLabel = "모집 마감";
      this.secondaryLabel = "100%";
    } else if (capacity == null || capacity == 0) {
      // ACTIVE + 정원 제한 없음 → bar 미표시 (showBar=false)
      this.pct = 0;
      this.colorClass = "primary";
      this.primaryLabel = "모집중";
      this.secondaryLabel = null;
    } else {
      double ratio = (double) applicantCount / capacity;
      this.pct = Math.min(100, (int) Math.round(ratio * 100));
      if (applicantCount >= capacity) {
        // ACTIVE 상태이지만 정원 100% 달성 → 마감 취급
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

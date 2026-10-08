package io.github.sihyuuun.youthmoa.program;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.center.Center;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ProgramCardDto 비율 계산 · primaryLabel · secondaryLabel 단위 테스트 (prototype.tsx L188~228 2-line 매칭).
 *
 * <p>Program.getStatus() 가 날짜 기반으로 결정되므로 startDate/endDate 를 조작해 UPCOMING/ENDED/OPEN 상태를 유도.
 */
class ProgramCardDtoTest {

  private static final Center CENTER =
      Center.builder().name("테스트 기관").region("수원시").isFeatured(false).build();

  private Program activeProgram(Integer capacity) {
    return Program.builder()
        .title("테스트 프로그램")
        .center(CENTER)
        .content("내용")
        .startDate(LocalDate.now().minusDays(1))
        .endDate(LocalDate.now().plusDays(10))
        .capacity(capacity)
        .build();
  }

  private Program upcomingProgram() {
    return Program.builder()
        .title("예정 프로그램")
        .center(CENTER)
        .content("내용")
        .startDate(LocalDate.now().plusDays(5))
        .endDate(LocalDate.now().plusDays(20))
        .build();
  }

  private Program closedProgram() {
    return Program.builder()
        .title("마감 프로그램")
        .center(CENTER)
        .content("내용")
        .startDate(LocalDate.now().minusDays(20))
        .endDate(LocalDate.now().minusDays(1))
        .build();
  }

  @Test
  @DisplayName("신청 비율 90% (applied < capacity) → colorClass=error, primaryLabel=정원 N/M명")
  void pct_90_isError_primaryLabelShowsCount() {
    ProgramCardDto dto = new ProgramCardDto(activeProgram(10), 9);
    assertThat(dto.getPct()).isEqualTo(90);
    assertThat(dto.getColorClass()).isEqualTo("error");
    assertThat(dto.getPrimaryLabel()).isEqualTo("정원 9/10명");
    assertThat(dto.getSecondaryLabel()).isEqualTo("90%");
  }

  @Test
  @DisplayName("신청 비율 100% (applied == capacity) → full 취급: muted, primaryLabel=모집 마감")
  void pct_100_isFull_shownAsClosed() {
    ProgramCardDto dto = new ProgramCardDto(activeProgram(10), 10);
    assertThat(dto.getPct()).isEqualTo(100);
    assertThat(dto.getColorClass()).isEqualTo("muted");
    assertThat(dto.getPrimaryLabel()).isEqualTo("모집 마감");
    assertThat(dto.getSecondaryLabel()).isEqualTo("100%");
  }

  @Test
  @DisplayName("신청 비율 70% → colorClass=warning, primaryLabel=정원 N/M명 (prototype 은 서두르세요 텍스트 미사용)")
  void pct_70_isWarning() {
    ProgramCardDto dto = new ProgramCardDto(activeProgram(10), 7);
    assertThat(dto.getPct()).isEqualTo(70);
    assertThat(dto.getColorClass()).isEqualTo("warning");
    assertThat(dto.getPrimaryLabel()).isEqualTo("정원 7/10명");
    assertThat(dto.getSecondaryLabel()).isEqualTo("70%");
  }

  @Test
  @DisplayName("신청 비율 89% → warning 유지")
  void pct_89_isWarning() {
    ProgramCardDto dto = new ProgramCardDto(activeProgram(100), 89);
    assertThat(dto.getPct()).isEqualTo(89);
    assertThat(dto.getColorClass()).isEqualTo("warning");
    assertThat(dto.getPrimaryLabel()).isEqualTo("정원 89/100명");
  }

  @Test
  @DisplayName("신청 비율 50% → colorClass=primary, primaryLabel=정원 N/M명")
  void pct_50_isPrimary() {
    ProgramCardDto dto = new ProgramCardDto(activeProgram(10), 5);
    assertThat(dto.getPct()).isEqualTo(50);
    assertThat(dto.getColorClass()).isEqualTo("primary");
    assertThat(dto.getPrimaryLabel()).isEqualTo("정원 5/10명");
  }

  @Test
  @DisplayName("capacity=null → colorClass=primary, primaryLabel=모집중, secondaryLabel=null")
  void capacityNull_showsRecruiting() {
    ProgramCardDto dto = new ProgramCardDto(activeProgram(null), 0);
    assertThat(dto.getColorClass()).isEqualTo("primary");
    assertThat(dto.getPrimaryLabel()).isEqualTo("모집중");
    assertThat(dto.getSecondaryLabel()).isNull();
  }

  @Test
  @DisplayName("UPCOMING → colorClass=secondary, primaryLabel=신청 오픈 예정, secondaryLabel=MM/dd 오픈")
  void upcoming_isSecondary_showsOpenDate() {
    ProgramCardDto dto = new ProgramCardDto(upcomingProgram(), 0);
    assertThat(dto.getStatus()).isEqualTo(ProgramStatus.UPCOMING);
    assertThat(dto.getPct()).isEqualTo(0);
    assertThat(dto.getColorClass()).isEqualTo("secondary");
    assertThat(dto.getPrimaryLabel()).isEqualTo("신청 오픈 예정");
    // MM/dd 오픈 (미래 startDate 5일 후)
    assertThat(dto.getSecondaryLabel()).endsWith(" 오픈");
  }

  @Test
  @DisplayName(
      "ENDED → colorClass=muted, primaryLabel='종료된 프로그램' (prototype capInfo.label 정합), secondaryLabel=null")
  void closed_isMuted() {
    ProgramCardDto dto = new ProgramCardDto(closedProgram(), 5);
    assertThat(dto.getStatus()).isEqualTo(ProgramStatus.ENDED);
    assertThat(dto.getPct()).isEqualTo(100);
    assertThat(dto.getColorClass()).isEqualTo("muted");
    assertThat(dto.getPrimaryLabel()).isEqualTo("종료된 프로그램");
    assertThat(dto.getSecondaryLabel()).isNull();
  }

  @Test
  @DisplayName("신청자 0명, capacity 있음 → pct=0, primary, primaryLabel=정원 0/N명")
  void pct_zero_withCapacity() {
    ProgramCardDto dto = new ProgramCardDto(activeProgram(20), 0);
    assertThat(dto.getPct()).isEqualTo(0);
    assertThat(dto.getColorClass()).isEqualTo("primary");
    assertThat(dto.getPrimaryLabel()).isEqualTo("정원 0/20명");
    assertThat(dto.getSecondaryLabel()).isEqualTo("0%");
  }

  // ─── F0f-fix-1: CTA 5분기 경계값 ───

  @Test
  @DisplayName("CTA: OPEN + pct<100 → apply/신청하기/primary/check")
  void cta_apply() {
    ProgramCardDto dto = new ProgramCardDto(activeProgram(10), 5);
    assertThat(dto.getCtaType()).isEqualTo("apply");
    assertThat(dto.getCtaLabel()).isEqualTo("신청하기");
    assertThat(dto.getCtaColorClass()).isEqualTo("primary");
    assertThat(dto.getCtaIcon()).isEqualTo("check");
    assertThat(dto.isCtaDisabled()).isFalse();
  }

  @Test
  @DisplayName("CTA: UPCOMING → openAlert/오픈 알림 받기/secondary/bell")
  void cta_openAlert() {
    ProgramCardDto dto = new ProgramCardDto(upcomingProgram(), 0);
    assertThat(dto.getCtaType()).isEqualTo("openAlert");
    assertThat(dto.getCtaLabel()).isEqualTo("오픈 알림 받기");
    assertThat(dto.getCtaColorClass()).isEqualTo("secondary");
    assertThat(dto.getCtaIcon()).isEqualTo("bell");
    assertThat(dto.isCtaDisabled()).isFalse();
  }

  @Test
  @DisplayName("CTA: OPEN + 만석(pct=100) → waitlist/빈자리 알림 받기/muted/bell")
  void cta_waitlist_full() {
    ProgramCardDto dto = new ProgramCardDto(activeProgram(10), 10);
    assertThat(dto.getCtaType()).isEqualTo("waitlist");
    assertThat(dto.getCtaLabel()).isEqualTo("빈자리 알림 받기");
    assertThat(dto.getCtaColorClass()).isEqualTo("muted");
    assertThat(dto.getCtaIcon()).isEqualTo("bell");
    assertThat(dto.isCtaDisabled()).isFalse();
  }

  @Test
  @DisplayName("CTA: ENDED + pct<100 (기간 만료) → expired/지난 프로그램/muted/disabled")
  void cta_expired() {
    ProgramCardDto dto = new ProgramCardDto(closedProgram(), 3);
    assertThat(dto.getCtaType()).isEqualTo("expired");
    assertThat(dto.getCtaLabel()).isEqualTo("지난 프로그램");
    assertThat(dto.getCtaColorClass()).isEqualTo("muted");
    assertThat(dto.getCtaIcon()).isNull();
    assertThat(dto.isCtaDisabled()).isTrue();
  }

  // ─── D5-Q1a reverify2 (2026-10-02): detail 서브텍스트 신청기간 축 보강 ───

  @Test
  @DisplayName(
      "detail UPCOMING: applyStartDate 미래 + startDate 과거 → '신청 오픈까지 N일' 은 applyStartDate 기준 계산")
  void detailHeadline_upcoming_usesApplyStartDate_overStartDate() {
    // 운영기간은 이미 시작했지만(startDate 과거) 신청은 아직 시작 전(applyStartDate 미래)
    // 상태 파생은 applyStartDate 기준 → UPCOMING, "신청 오픈까지 N일" 산술도 applyStartDate 기준이어야 함
    Program p =
        Program.builder()
            .title("신청기간 축 검증 프로그램")
            .center(CENTER)
            .content("내용")
            .startDate(LocalDate.now().minusDays(10))
            .endDate(LocalDate.now().plusDays(20))
            .applyStartDate(LocalDate.now().plusDays(5))
            .applyEndDate(LocalDate.now().plusDays(15))
            .build();
    ProgramCardDto dto = new ProgramCardDto(p, 0);
    assertThat(dto.getStatus()).isEqualTo(ProgramStatus.UPCOMING);
    // applyStartDate 기준 5일 (startDate 기준이면 -10일 → "신청 오픈까지 -10일" 모순)
    assertThat(dto.getDetailHeadline()).isEqualTo("신청 오픈까지 5일");
    assertThat(dto.isDetailEmphasized()).isTrue();
  }

  @Test
  @DisplayName(
      "detail OPEN with capacity: applyEndDate 가 endDate 와 다를 때 '마감까지 N일' 은 applyEndDate 기준 계산")
  void detailHeadline_open_usesApplyEndDate_overEndDate() {
    // 운영기간은 아직 많이 남았지만 신청 마감은 임박
    // 뱃지 D-day 와 서브텍스트가 같은 축(applyEndDate) 을 써야 함
    Program p =
        Program.builder()
            .title("신청 마감 임박 프로그램")
            .center(CENTER)
            .content("내용")
            .startDate(LocalDate.now().minusDays(5))
            .endDate(LocalDate.now().plusDays(60))
            .applyStartDate(LocalDate.now().minusDays(5))
            .applyEndDate(LocalDate.now().plusDays(3))
            .capacity(10)
            .build();
    ProgramCardDto dto = new ProgramCardDto(p, 5);
    assertThat(dto.getStatus()).isEqualTo(ProgramStatus.OPEN);
    // applyEndDate 기준 3일 (endDate 기준이면 60일)
    assertThat(dto.getDetailHeadline()).contains("마감까지 3일");
  }

  @Test
  @DisplayName(
      "detail UPCOMING: applyStartDate null + startDate 미래 → startDate 폴백 (D5-Q1d 백필 전 레거시 데이터)")
  void detailHeadline_upcoming_fallbackToStartDate_whenApplyStartNull() {
    // 레거시 row (applyStartDate null) — Q1d 백필 전까지 startDate 폴백 유지
    ProgramCardDto dto = new ProgramCardDto(upcomingProgram(), 0);
    assertThat(dto.getStatus()).isEqualTo(ProgramStatus.UPCOMING);
    // upcomingProgram startDate = today + 5
    assertThat(dto.getDetailHeadline()).isEqualTo("신청 오픈까지 5일");
  }

  @Test
  @DisplayName("CTA: SUSPENDED (운영 중단) → inactive/운영이 중단되었어요/muted/disabled")
  void cta_inactive() {
    Program p =
        Program.builder()
            .title("중단 프로그램")
            .center(CENTER)
            .content("내용")
            .startDate(LocalDate.now().plusDays(3))
            .endDate(LocalDate.now().plusDays(30))
            .capacity(10)
            .isActive(false)
            .build();
    ProgramCardDto dto = new ProgramCardDto(p, 0);
    assertThat(dto.getStatus()).isEqualTo(ProgramStatus.SUSPENDED);
    assertThat(dto.getCtaType()).isEqualTo("inactive");
    assertThat(dto.getCtaLabel()).isEqualTo("운영이 중단되었어요");
    assertThat(dto.getCtaColorClass()).isEqualTo("muted");
    assertThat(dto.isCtaDisabled()).isTrue();
  }
}

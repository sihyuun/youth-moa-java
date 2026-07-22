package io.github.sihyuuun.youthmoa.program;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ProgramCardDto 비율 계산 · primaryLabel · secondaryLabel 단위 테스트 (prototype.tsx L188~228 2-line 매칭).
 *
 * <p>Program.getStatus() 가 날짜 기반으로 결정되므로 startDate/endDate 를 조작해 UPCOMING/ENDED/OPEN 상태를 유도.
 */
class ProgramCardDtoTest {

  private Program activeProgram(Integer capacity) {
    return Program.builder()
        .title("테스트 프로그램")
        .organization("테스트 기관")
        .content("내용")
        .startDate(LocalDate.now().minusDays(1))
        .endDate(LocalDate.now().plusDays(10))
        .capacity(capacity)
        .build();
  }

  private Program upcomingProgram() {
    return Program.builder()
        .title("예정 프로그램")
        .organization("테스트 기관")
        .content("내용")
        .startDate(LocalDate.now().plusDays(5))
        .endDate(LocalDate.now().plusDays(20))
        .build();
  }

  private Program closedProgram() {
    return Program.builder()
        .title("마감 프로그램")
        .organization("테스트 기관")
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
  @DisplayName("ENDED → colorClass=muted, primaryLabel=모집 마감, secondaryLabel=100%")
  void closed_isMuted() {
    ProgramCardDto dto = new ProgramCardDto(closedProgram(), 5);
    assertThat(dto.getStatus()).isEqualTo(ProgramStatus.ENDED);
    assertThat(dto.getPct()).isEqualTo(100);
    assertThat(dto.getColorClass()).isEqualTo("muted");
    assertThat(dto.getPrimaryLabel()).isEqualTo("모집 마감");
    assertThat(dto.getSecondaryLabel()).isEqualTo("100%");
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

  @Test
  @DisplayName("CTA: SUSPENDED (운영 중단) → inactive/운영이 중단되었어요/muted/disabled")
  void cta_inactive() {
    Program p =
        Program.builder()
            .title("중단 프로그램")
            .organization("기관")
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

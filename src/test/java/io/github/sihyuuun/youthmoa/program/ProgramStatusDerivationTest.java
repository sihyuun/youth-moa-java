package io.github.sihyuuun.youthmoa.program;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.center.Center;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D5-Q1a (2026-10-02): Program.getStatus() / D-day 가 신청기간(applyStart/End) 기준으로 파생됨을 보장하는
 * 상태 매트릭스 테스트.
 *
 * <p>HANDOFF.md L273 "status는 신청기간·정원으로 파생" + 사용자 확정 Set-A Q5=A ("applyEnd &lt; today →
 * ENDED, 운영기간 무관") 를 코드로 못박는다. consumer 지점(ProgramSpec·Repository·Service) 교체는 D5-Q1b 범위이며
 * 본 테스트는 엔티티 파생 메서드만 검증한다.
 *
 * <p>D5-Q1d (2026-10-06 · V27): applyStart/End NOT NULL 승격 완료. case 6 (null 폴백) 삭제됨 — 승격
 * 전제라 unreachable.
 */
class ProgramStatusDerivationTest {

  private static final Center CENTER =
      Center.builder().name("테스트 기관").region("수원시").isFeatured(false).build();

  private Program.ProgramBuilder base() {
    return Program.builder()
        .title("상태 매트릭스 테스트")
        .center(CENTER)
        .content("내용")
        .startDate(LocalDate.now().minusDays(10))
        .endDate(LocalDate.now().plusDays(30));
  }

  @Test
  @DisplayName("case 1 — applyStart > today → UPCOMING (운영기간이 이미 시작했어도)")
  void applyStartFuture_isUpcoming() {
    LocalDate today = LocalDate.now();
    Program p =
        base()
            .applyStartDate(today.plusDays(3))
            .applyEndDate(today.plusDays(10))
            .build();

    assertThat(p.getStatus()).isEqualTo(ProgramStatus.UPCOMING);
  }

  @Test
  @DisplayName("case 2 — applyStart <= today <= applyEnd → OPEN (모집 중)")
  void applyPeriodIncludesToday_isOpen() {
    LocalDate today = LocalDate.now();
    Program p =
        base()
            .applyStartDate(today.minusDays(2))
            .applyEndDate(today.plusDays(5))
            .build();

    assertThat(p.getStatus()).isEqualTo(ProgramStatus.OPEN);
    assertThat(p.getDaysUntilDeadline()).isEqualTo(5);
    assertThat(p.getDdayLabel()).isEqualTo("D-5");
  }

  @Test
  @DisplayName("case 3 — applyEnd < today → ENDED (운영기간이 아직 미래여도. Q5=A)")
  void applyEndPassed_isEnded_evenIfOperatePeriodFuture() {
    LocalDate today = LocalDate.now();
    Program p =
        base()
            // 운영기간은 미래에 걸쳐 있지만 신청은 이미 마감된 시나리오
            .startDate(today.plusDays(10))
            .endDate(today.plusDays(60))
            .applyStartDate(today.minusDays(14))
            .applyEndDate(today.minusDays(1))
            .build();

    assertThat(p.getStatus()).isEqualTo(ProgramStatus.ENDED);
    assertThat(p.getDdayLabel()).isEqualTo("종료");
  }

  @Test
  @DisplayName("case 4 — applyEnd == today → OPEN (경계: 당일 마감까지 신청 가능, D-DAY)")
  void applyEndToday_isOpenAndDDay() {
    LocalDate today = LocalDate.now();
    Program p =
        base()
            .applyStartDate(today.minusDays(5))
            .applyEndDate(today)
            .build();

    assertThat(p.getStatus()).isEqualTo(ProgramStatus.OPEN);
    assertThat(p.getDaysUntilDeadline()).isEqualTo(0);
    assertThat(p.getDdayLabel()).isEqualTo("D-DAY");
  }

  @Test
  @DisplayName("case 5 — isActive=false → SUSPENDED (어떤 신청기간이든 최우선)")
  void inactive_isSuspended_regardlessOfApplyPeriod() {
    LocalDate today = LocalDate.now();
    Program p =
        base()
            .applyStartDate(today.minusDays(2))
            .applyEndDate(today.plusDays(5))
            .isActive(false)
            .build();

    assertThat(p.getStatus()).isEqualTo(ProgramStatus.SUSPENDED);
  }

}

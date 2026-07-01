package io.github.sihyuuun.youthmoa.program;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProgramCardDto 비율 계산 경계값 단위 테스트.
 * Program.getStatus() 가 날짜 기반으로 결정되므로
 * startDate/endDate 를 조작해 UPCOMING/CLOSED/ACTIVE 상태를 유도한다.
 */
class ProgramCardDtoTest {

    // ---- 헬퍼 ----

    private Program activeProgram(Integer capacity) {
        return Program.builder()
                .title("테스트 프로그램").organization("테스트 기관")
                .content("내용").requirements("조건")
                .startDate(LocalDate.now().minusDays(1))   // 어제 시작 → ACTIVE
                .endDate(LocalDate.now().plusDays(10))
                .capacity(capacity)
                .build();
    }

    private Program upcomingProgram() {
        return Program.builder()
                .title("예정 프로그램").organization("테스트 기관")
                .content("내용").requirements("조건")
                .startDate(LocalDate.now().plusDays(5))    // 미래 시작 → UPCOMING
                .endDate(LocalDate.now().plusDays(20))
                .build();
    }

    private Program closedProgram() {
        return Program.builder()
                .title("마감 프로그램").organization("테스트 기관")
                .content("내용").requirements("조건")
                .startDate(LocalDate.now().minusDays(20))
                .endDate(LocalDate.now().minusDays(1))     // 어제 종료 → CLOSED
                .build();
    }

    // ---- 경계값 테스트 ----

    @Test
    @DisplayName("신청 비율 90% 이상 → colorClass=error, barLabel=마감임박")
    void pct_90orAbove_isError() {
        // capacity=10, applicantCount=9 → 90%
        ProgramCardDto dto = new ProgramCardDto(activeProgram(10), 9);

        assertThat(dto.getPct()).isEqualTo(90);
        assertThat(dto.getColorClass()).isEqualTo("error");
        assertThat(dto.getBarLabel()).isEqualTo("마감임박");
    }

    @Test
    @DisplayName("신청 비율 100% → pct=100, colorClass=error")
    void pct_100_isError() {
        // capacity=10, applicantCount=10 → 100%
        ProgramCardDto dto = new ProgramCardDto(activeProgram(10), 10);

        assertThat(dto.getPct()).isEqualTo(100);
        assertThat(dto.getColorClass()).isEqualTo("error");
        assertThat(dto.getBarLabel()).isEqualTo("마감임박");
    }

    @Test
    @DisplayName("신청 비율 70% → colorClass=warning, barLabel=서두르세요")
    void pct_70_isWarning() {
        // capacity=10, applicantCount=7 → 70%
        ProgramCardDto dto = new ProgramCardDto(activeProgram(10), 7);

        assertThat(dto.getPct()).isEqualTo(70);
        assertThat(dto.getColorClass()).isEqualTo("warning");
        assertThat(dto.getBarLabel()).isEqualTo("서두르세요");
    }

    @Test
    @DisplayName("신청 비율 89% → colorClass=warning (90% 미만 경계)")
    void pct_89_isWarning() {
        // capacity=100, applicantCount=89 → 89%
        ProgramCardDto dto = new ProgramCardDto(activeProgram(100), 89);

        assertThat(dto.getPct()).isEqualTo(89);
        assertThat(dto.getColorClass()).isEqualTo("warning");
        assertThat(dto.getBarLabel()).isEqualTo("서두르세요");
    }

    @Test
    @DisplayName("신청 비율 50% → colorClass=primary, barLabel=모집중")
    void pct_50_isPrimary() {
        // capacity=10, applicantCount=5 → 50%
        ProgramCardDto dto = new ProgramCardDto(activeProgram(10), 5);

        assertThat(dto.getPct()).isEqualTo(50);
        assertThat(dto.getColorClass()).isEqualTo("primary");
        assertThat(dto.getBarLabel()).isEqualTo("모집중");
    }

    @Test
    @DisplayName("capacity=null → colorClass=primary, barLabel=모집중, capacityText=정원 제한 없음")
    void capacityNull_showsNoLimit() {
        ProgramCardDto dto = new ProgramCardDto(activeProgram(null), 0);

        assertThat(dto.getColorClass()).isEqualTo("primary");
        assertThat(dto.getBarLabel()).isEqualTo("모집중");
        assertThat(dto.getCapacityText()).isEqualTo("정원 제한 없음");
    }

    @Test
    @DisplayName("UPCOMING 상태 → colorClass=secondary, pct=0, barLabel=신청 오픈 예정")
    void upcoming_isSecondary() {
        ProgramCardDto dto = new ProgramCardDto(upcomingProgram(), 0);

        assertThat(dto.getStatus()).isEqualTo(ProgramStatus.UPCOMING);
        assertThat(dto.getPct()).isEqualTo(0);
        assertThat(dto.getColorClass()).isEqualTo("secondary");
        assertThat(dto.getBarLabel()).isEqualTo("신청 오픈 예정");
    }

    @Test
    @DisplayName("CLOSED 상태 → colorClass=muted, pct=100, barLabel=모집 마감")
    void closed_isMuted() {
        ProgramCardDto dto = new ProgramCardDto(closedProgram(), 5);

        assertThat(dto.getStatus()).isEqualTo(ProgramStatus.CLOSED);
        assertThat(dto.getPct()).isEqualTo(100);
        assertThat(dto.getColorClass()).isEqualTo("muted");
        assertThat(dto.getBarLabel()).isEqualTo("모집 마감");
    }

    @Test
    @DisplayName("capacity 있을 때 capacityText 포맷 확인")
    void capacityText_format() {
        ProgramCardDto dto = new ProgramCardDto(activeProgram(30), 12);

        assertThat(dto.getCapacityText()).isEqualTo("정원 12/30명");
    }

    @Test
    @DisplayName("신청자 0명, capacity 있음 → pct=0, colorClass=primary")
    void pct_zero_withCapacity() {
        ProgramCardDto dto = new ProgramCardDto(activeProgram(20), 0);

        assertThat(dto.getPct()).isEqualTo(0);
        assertThat(dto.getColorClass()).isEqualTo("primary");
        assertThat(dto.getBarLabel()).isEqualTo("모집중");
    }
}

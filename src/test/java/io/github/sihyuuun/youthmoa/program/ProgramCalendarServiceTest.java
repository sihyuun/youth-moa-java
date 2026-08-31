package io.github.sihyuuun.youthmoa.program;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * F0f 캘린더 뷰 서비스 단위 테스트. dc.html §1a/§5a/§6a/§7a 정본 반영.
 *
 * <p>테스트 전략:
 *
 * <ul>
 *   <li>42셀 · DOW · 배치 = 지정 year/month 로 결정론적 검증
 *   <li>색상 매핑 · isFull = LocalDate.now() 기준 상대 시드
 *   <li>nearestMonth = 필터 결과 유무 시나리오
 * </ul>
 */
@DataJpaTest
@AutoConfigureTestDatabase
@Import({JpaConfig.class, ProgramCalendarService.class})
class ProgramCalendarServiceTest {

  @Autowired ProgramRepository programRepository;
  @Autowired ApplicationRepository applicationRepository;
  @Autowired UserRepository userRepository;
  @Autowired ProgramCalendarService service;

  // ─────────── 헬퍼 ───────────

  private Program saveProgram(String title, LocalDate start, LocalDate end, Integer capacity) {
    return programRepository.save(
        Program.builder()
            .title(title)
            .organization("org")
            .category("c")
            .region("수원시")
            .content("c")
            .startDate(start)
            .endDate(end)
            .capacity(capacity)
            .build());
  }

  private User saveUser(String email) {
    return userRepository.save(
        User.builder()
            .email(email)
            .password("pw")
            .name("n")
            .phone("010-0000-0000")
            .role(UserRole.USER)
            .build());
  }

  private void apply(User u, Program p) {
    applicationRepository.save(
        Application.builder().user(u).program(p).status(ApplicationStatus.APPROVED).build());
  }

  // ─────────── 42셀 · DOW · 배치 ───────────

  @Test
  @DisplayName("cells 는 항상 42개 (2026-01, 2026-02, 2026-08 모두)")
  void cells_size_always_42() {
    for (int[] ym : new int[][] {{2026, 1}, {2026, 2}, {2026, 8}}) {
      CalendarViewDto dto = service.calendar("", List.of(), List.of(), ym[0], ym[1]);
      assertThat(dto.getCells()).hasSize(42);
    }
  }

  @Test
  @DisplayName("2026-08-01 은 토요일이라 앞 빈 셀 6개")
  void first_dow_of_2026_08_is_saturday_6() {
    CalendarViewDto dto = service.calendar("", List.of(), List.of(), 2026, 8);
    List<CalendarViewDto.CalendarCell> cells = dto.getCells();
    for (int i = 0; i < 6; i++) {
      assertThat(cells.get(i).getDay()).as("cell %d", i).isNull();
      assertThat(cells.get(i).isInMonth()).isFalse();
    }
    assertThat(cells.get(6).getDay()).isEqualTo(1);
  }

  @Test
  @DisplayName("2026-08 배치: cells[6]=1일, cells[36]=31일, cells[37]=null")
  void days_placement_matches_calendar() {
    CalendarViewDto dto = service.calendar("", List.of(), List.of(), 2026, 8);
    List<CalendarViewDto.CalendarCell> cells = dto.getCells();
    assertThat(cells.get(6).getDay()).isEqualTo(1);
    assertThat(cells.get(36).getDay()).isEqualTo(31);
    assertThat(cells.get(37).getDay()).isNull();
    assertThat(cells.get(37).isInMonth()).isFalse();
  }

  // ─────────── grouping · pills ───────────

  @Test
  @DisplayName("startDate=2026-08-15 프로그램은 cells[20]=15일 셀에 pill")
  void grouping_by_start_date() {
    // 이 프로그램은 status=UPCOMING (start > today, 오랫동안 유지)
    // isActive 만 확인, 필터는 "전체(빈 status)" — spec 은 notEnded() 로 종료만 배제
    saveProgram("P15", LocalDate.of(2026, 8, 15), LocalDate.of(2099, 12, 31), 100);
    CalendarViewDto dto = service.calendar("", List.of(), List.of(), 2026, 8);
    // 15일 = cells[6+14] = cells[20]
    CalendarViewDto.CalendarCell cell = dto.getCells().get(20);
    assertThat(cell.getDay()).isEqualTo(15);
    assertThat(cell.getPills()).hasSize(1);
    assertThat(cell.getPills().get(0).getTitle()).isEqualTo("P15");
  }

  @Test
  @DisplayName("같은 날 3건 → pills 2건 + moreCount 1")
  void max_2_pills_plus_more_count() {
    LocalDate d = LocalDate.of(2026, 8, 15);
    LocalDate far = LocalDate.of(2099, 12, 31);
    saveProgram("A", d, far, 100);
    saveProgram("B", d, far, 100);
    saveProgram("C", d, far, 100);
    CalendarViewDto dto = service.calendar("", List.of(), List.of(), 2026, 8);
    CalendarViewDto.CalendarCell cell = dto.getCells().get(20);
    assertThat(cell.getPills()).hasSize(2);
    assertThat(cell.getMoreCount()).isEqualTo(1);
  }

  // ─────────── 색 매핑 (LocalDate.now() 기준) ───────────

  @Test
  @DisplayName("colorKind: upcoming (start>today) / open (start≤today≤end) / ended (end<today)")
  void color_kind_upcoming_open_ended() {
    LocalDate today = LocalDate.now();

    // 전 달·이 달·다음 달 아무거나로 셋팅. 서비스 필터 spec 은 종료 배제 → ended 케이스는
    // pillColorKind() 유닛으로 직접 검증.
    Program upcoming =
        saveProgram("upcoming", today.plusDays(10), today.plusDays(30), 100);
    Program open = saveProgram("open", today.minusDays(5), today.plusDays(5), 100);
    Program ended = saveProgram("ended", today.minusDays(30), today.minusDays(5), 100);

    assertThat(ProgramCalendarService.pillColorKind(new ProgramCardDto(upcoming, 0)))
        .isEqualTo("upcoming");
    assertThat(ProgramCalendarService.pillColorKind(new ProgramCardDto(open, 0)))
        .isEqualTo("open");
    assertThat(ProgramCalendarService.pillColorKind(new ProgramCardDto(ended, 0)))
        .isEqualTo("ended");
  }

  @Test
  @DisplayName("isFull(applied≥capacity) 인 OPEN 은 colorKind=ended")
  void is_full_maps_to_ended() {
    LocalDate today = LocalDate.now();
    Program p = saveProgram("full", today.minusDays(5), today.plusDays(5), 2);
    User u1 = saveUser("u1@t.com");
    User u2 = saveUser("u2@t.com");
    apply(u1, p);
    apply(u2, p);
    assertThat(ProgramCalendarService.pillColorKind(new ProgramCardDto(p, 2)))
        .isEqualTo("ended");
  }

  // ─────────── nearestMonth ───────────

  @Test
  @DisplayName("현재 월 0건 + 다른 월에 프로그램 있음 → nearestMonth != null")
  void nearest_month_returns_when_current_empty() {
    // 2026-08 을 조회 — 그 달엔 없음. 대신 2026-10 에 프로그램 배치
    saveProgram("P10", LocalDate.of(2026, 10, 5), LocalDate.of(2099, 12, 31), 100);
    CalendarViewDto dto = service.calendar("", List.of(), List.of(), 2026, 8);
    assertThat(dto.getTotalCount()).isZero();
    assertThat(dto.getNearestMonth()).isEqualTo(10);
    assertThat(dto.getNearestYear()).isEqualTo(2026);
    assertThat(dto.getNearestCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("전체 프로그램 0건 → nearestMonth == null")
  void nearest_month_null_when_all_empty() {
    CalendarViewDto dto = service.calendar("", List.of(), List.of(), 2026, 8);
    assertThat(dto.getTotalCount()).isZero();
    assertThat(dto.getNearestMonth()).isNull();
    assertThat(dto.getNearestYear()).isNull();
  }

  @Test
  @DisplayName("동거리 tie-break: 8월 기준 6월/10월 각 1건 → 미래(10월) 우선 (스펙 §3-A #9)")
  void nearest_month_tie_break_prefers_future() {
    // 2026-08 pivot, 2026-06 과 2026-10 에 각 1건 (거리 동일 = 2)
    saveProgram("past", LocalDate.of(2026, 6, 15), LocalDate.of(2099, 12, 31), 100);
    saveProgram("future", LocalDate.of(2026, 10, 15), LocalDate.of(2099, 12, 31), 100);
    CalendarViewDto dto = service.calendar("", List.of(), List.of(), 2026, 8);
    assertThat(dto.getTotalCount()).isZero();
    assertThat(dto.getNearestYear()).isEqualTo(2026);
    assertThat(dto.getNearestMonth()).isEqualTo(10);
    assertThat(dto.getNearestCount()).isEqualTo(1L);
  }
}

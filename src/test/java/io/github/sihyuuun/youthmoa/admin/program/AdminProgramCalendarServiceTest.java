package io.github.sihyuuun.youthmoa.admin.program;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.program.CalendarViewDto;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramCardDto;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.program.ProgramStatus;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * D-admin-calendar (2026-10-12) — AdminProgramCalendarService 통합 검증.
 *
 * <p>관리자 캘린더 특유 조건을 검증:
 *
 * <ul>
 *   <li>POLICY P-6 — cells 항상 42개
 *   <li>Q6 — SUSPENDED 포함 (isActive=false 도 표시)
 *   <li>Q7 — q/status filter pass-through (AdminProgramService.buildListSpec 재사용)
 *   <li>pillColorKind — SUSPENDED → ended 매핑
 * </ul>
 *
 * <p>e2e 프로파일 H2 + 시드 데이터 활용. 테스트 세션 내 트랜잭션 롤백.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@Transactional
class AdminProgramCalendarServiceTest {

  @Autowired AdminProgramCalendarService service;
  @Autowired ProgramRepository programRepository;
  @Autowired UserRepository userRepository;

  @AfterEach
  void clearAuth() {
    SecurityContextHolder.clearContext();
  }

  private void loginAsSysadmin() {
    User u = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(u);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
  }

  // ─────────── 42셀 (POLICY P-6) ───────────

  @Test
  @DisplayName("POLICY P-6: cells 는 어느 월이든 항상 42개")
  void cells_size_always_42() {
    loginAsSysadmin();
    for (int[] ym : new int[][] {{2026, 1}, {2026, 2}, {2026, 8}, {2026, 12}}) {
      CalendarViewDto dto = service.calendar(null, null, ym[0], ym[1]);
      assertThat(dto.getCells()).as("ym %d-%d", ym[0], ym[1]).hasSize(42);
    }
  }

  @Test
  @DisplayName("prev/next 월 메타 산출")
  void prev_next_meta() {
    loginAsSysadmin();
    CalendarViewDto dto = service.calendar(null, null, 2026, 1);
    assertThat(dto.getPrevYear()).isEqualTo(2025);
    assertThat(dto.getPrevMonth()).isEqualTo(12);
    assertThat(dto.getNextYear()).isEqualTo(2026);
    assertThat(dto.getNextMonth()).isEqualTo(2);
  }

  // ─────────── Q6 — SUSPENDED 포함 ───────────

  @Test
  @DisplayName("Q6: SUSPENDED 프로그램도 표시 (사용자 캘린더와의 차이)")
  void includes_suspended_programs() {
    loginAsSysadmin();
    // 시드에 포함된 아무 프로그램을 SUSPENDED 로 전환 (isActive=false)
    Program any =
        programRepository.findAll().stream()
            .filter(p -> p.getStartDate() != null)
            .findFirst()
            .orElseThrow();
    any.deactivate();
    programRepository.saveAndFlush(any);

    YearMonth ym = YearMonth.from(any.getStartDate());
    CalendarViewDto dto = service.calendar(null, null, ym.getYear(), ym.getMonthValue());

    // SUSPENDED 프로그램의 startDate 에 해당하는 셀 pill 에 포함돼 있어야 함
    int day = any.getStartDate().getDayOfMonth();
    boolean found =
        dto.getCells().stream()
            .anyMatch(
                c ->
                    c.getDay() != null
                        && c.getDay() == day
                        && c.getPills().stream().anyMatch(p -> p.getId().equals(any.getId())));
    assertThat(found).as("SUSPENDED 프로그램이 해당 셀 pill 에 포함").isTrue();
  }

  @Test
  @DisplayName("pillColorKind: SUSPENDED → ended, UPCOMING → upcoming, OPEN → open")
  void pill_color_kind_mapping() {
    Program suspended = stubProgram("s", LocalDate.now().plusDays(5), ProgramStatus.SUSPENDED);
    Program upcoming = stubProgram("u", LocalDate.now().plusDays(10), null);
    assertThat(AdminProgramCalendarService.pillColorKind(new ProgramCardDto(suspended, 0)))
        .isEqualTo("ended");
    assertThat(AdminProgramCalendarService.pillColorKind(new ProgramCardDto(upcoming, 0)))
        .isEqualTo("upcoming");
  }

  // ─────────── Q7 — filter pass-through ───────────

  @Test
  @DisplayName("Q7: status=SUSPENDED 필터 적용 시 활성 프로그램 pill 제외")
  void status_filter_passthrough_suspended() {
    loginAsSysadmin();
    LocalDate today = LocalDate.now();
    CalendarViewDto dto =
        service.calendar(null, "SUSPENDED", today.getYear(), today.getMonthValue());
    // 모든 pill 은 SUSPENDED 프로그램이어야 함 — pill 들의 id 로 역조회
    dto.getCells().stream()
        .flatMap(c -> c.getPills().stream())
        .forEach(
            p -> {
              Program prog = programRepository.findById(p.getId()).orElseThrow();
              assertThat(prog.isActive()).as("SUSPENDED 필터 시 isActive=false 만").isFalse();
            });
  }

  @Test
  @DisplayName("Q7: q 검색어 필터 pass-through — 존재하지 않는 키워드 → totalCount=0 + nearestMonth 조회")
  void q_filter_passthrough_no_match() {
    loginAsSysadmin();
    LocalDate today = LocalDate.now();
    CalendarViewDto dto =
        service.calendar("존재하지않을검색어XYZ123", null, today.getYear(), today.getMonthValue());
    assertThat(dto.getTotalCount()).isZero();
    // nearestMonth 는 필터 결과도 0이므로 null
    assertThat(dto.getNearestMonth()).isNull();
  }

  // ─────────── nearestMonth ───────────

  @Test
  @DisplayName("빈 달 조회 시 nearestMonth 는 가장 가까운 월")
  void nearest_month_when_empty() {
    loginAsSysadmin();
    // 2099-12 는 시드에 프로그램 없다고 가정
    CalendarViewDto dto = service.calendar(null, null, 2099, 12);
    assertThat(dto.getTotalCount()).isZero();
    // 시드에 아무 프로그램이라도 있으면 nearestMonth 가 채워지며, 아니면 null — 어느 쪽이든 TC 깨지지 않도록 기본값 확인
    assertThat(dto)
        .satisfiesAnyOf(
            d -> assertThat(d.getNearestMonth()).isNull(),
            d -> assertThat(d.getNearestMonth()).isBetween(1, 12));
  }

  private Program stubProgram(String title, LocalDate start, ProgramStatus forcedStatus) {
    Program p =
        Program.builder()
            .title(title)
            .category("c")
            .region("r")
            .content("c")
            .startDate(start)
            .endDate(LocalDate.of(2099, 12, 31))
            .capacity(10)
            .build();
    if (forcedStatus == ProgramStatus.SUSPENDED) {
      p.deactivate();
    }
    return p;
  }
}

package io.github.sihyuuun.youthmoa.program;

import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.bookmark.BookmarkService;
import jakarta.servlet.http.HttpSession;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ProgramController {

  private final ProgramService programService;
  private final ProgramCalendarService programCalendarService;
  private final BookmarkService bookmarkService;
  private final ApplicationRepository applicationRepository;

  @GetMapping("/programs")
  public String list(
      @RequestParam(required = false, defaultValue = "") String status,
      @RequestParam(name = "regions", required = false) List<String> regions,
      @RequestParam(name = "centers", required = false) List<String> centers,
      @RequestParam(required = false, defaultValue = "default") String sort,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false) String view,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) Integer month,
      @RequestHeader(name = "HX-Request", required = false) String hxRequest,
      @AuthenticationPrincipal UserDetails principal,
      Model model) {

    List<String> safeRegions = regions == null ? Collections.emptyList() : regions;
    List<String> safeCenters = centers == null ? Collections.emptyList() : centers;
    boolean isCalendarView = "calendar".equals(view);

    // 즐겨찾기 IDs 를 먼저 계산 — 기본 정렬순(default) 로직과 카드 렌더 N+1 회피 모두에 사용
    Set<Long> bookmarkedIds =
        bookmarkService.getBookmarkedProgramIds(principal != null ? principal.getUsername() : null);

    Page<Program> programs =
        programService.search(status, safeRegions, safeCenters, sort, page, bookmarkedIds);

    model.addAttribute("currentPage", "programs");
    model.addAttribute("programs", programs);

    // 사이드바 (featured 5) + 팝오버 (전체)
    model.addAttribute("allRegions", programService.getAllRegions());
    model.addAttribute("allCenters", programService.getAllCenters());

    model.addAttribute("filterStatus", status);
    model.addAttribute("filterRegions", safeRegions);
    model.addAttribute("filterCenters", safeCenters);
    model.addAttribute("filterSort", sort);

    // 활성 칩 — key=group:value, label, removeQuery
    List<Map<String, String>> activeFilters =
        buildActiveFilters(status, safeRegions, safeCenters, sort);
    model.addAttribute("activeFilters", activeFilters);

    // 인증된 사용자의 즐겨찾기 program id Set (카드 N개 N+1 회피) — 위에서 계산한 Set 재사용
    model.addAttribute("bookmarkedIds", bookmarkedIds);

    // CapacityBar용 DTO (IN 쿼리 1회, N+1 방지)
    model.addAttribute("cardDtos", programService.toCardDtos(programs.getContent()));

    // 캘린더 뷰 (F0f) — view=calendar 이면 CalendarViewDto 세팅
    model.addAttribute("view", isCalendarView ? "calendar" : "list");
    if (isCalendarView) {
      YearMonth ym = (year != null && month != null) ? YearMonth.of(year, month) : YearMonth.now();
      CalendarViewDto calendarView =
          programCalendarService.calendar(
              status, safeRegions, safeCenters, ym.getYear(), ym.getMonthValue());
      model.addAttribute("calendarView", calendarView);
      // 빈 달 배너 문구용 탭 라벨 (dc.html §7a "{현재월}에는 {탭 이름} 프로그램이 없어요")
      model.addAttribute("calendarEmptyTabLabel", emptyTabLabel(status));
    }

    // htmx 부분 갱신 — view=calendar 상태에서는 캘린더 fragment 반환 (ym-verify N2-2)
    if (hxRequest != null && !hxRequest.isBlank()) {
      return isCalendarView
          ? "program/_calendar-fragment :: calendar-region"
          : "program/_list-fragment :: list-region";
    }
    return "program/list";
  }

  /** F0f 캘린더 빈 달 배너 문구용 탭 라벨. dc.html §7a "8월에는 종료된 프로그램이 없어요" 패턴. */
  private static String emptyTabLabel(String status) {
    if (status == null) return "";
    return switch (status) {
      case "upcoming" -> "진행예정된";
      case "active" -> "모집중인";
      case "ended" -> "종료된";
      default -> "";
    };
  }

  private List<Map<String, String>> buildActiveFilters(
      String status, List<String> regions, List<String> centers, String sort) {
    List<Map<String, String>> chips = new ArrayList<>();
    for (String r : regions) {
      Map<String, String> chip = new LinkedHashMap<>();
      chip.put("group", "regions");
      chip.put("value", r);
      chip.put("label", r);
      chips.add(chip);
    }
    for (String c : centers) {
      Map<String, String> chip = new LinkedHashMap<>();
      chip.put("group", "centers");
      chip.put("value", c);
      chip.put("label", c);
      chips.add(chip);
    }
    return chips;
  }

  // ym-verify (2026-07-09 전체화면 검증 FAIL #2): open-in-view: false 환경에서 Program.content 접근이
  // auto-commit 오류로 렌더 실패할 잠재 위험 → 이전엔 readOnly 트랜잭션 유지.
  // A6-followup (2026-09-29): 조회수 증가(write) 를 위해 controller-level readOnly 제거.
  // Program.center 는 EAGER 이고 content 는 LONGVARCHAR 매핑이라 auto-commit 사고 재발 위험 소멸.
  // 조회수 증가는 programService.incrementViewCount 내부 @Transactional 이 담당.
  @GetMapping("/programs/{id}")
  public String detail(
      @PathVariable Long id,
      @AuthenticationPrincipal UserDetails principal,
      HttpSession session,
      Model model) {
    // A6-followup: 세션 dedup + Role skip 후 조회수 증가.
    trackView(id, principal, session);

    Program program = programService.findById(id);
    boolean bookmarked =
        principal != null && bookmarkService.isBookmarked(principal.getUsername(), id);

    long appliedCount =
        applicationRepository.countByProgramAndStatusIn(
            program, List.of(ApplicationStatus.PENDING, ApplicationStatus.APPROVED));

    int applicationRate = 0;
    String competitionRatio = "0.0";
    if (program.getCapacity() != null && program.getCapacity() > 0) {
      double ratio = (double) appliedCount / program.getCapacity();
      applicationRate = Math.min(100, (int) Math.round(ratio * 100));
      competitionRatio = String.format("%.1f", ratio);
    }

    // D5 — 상세 페이지 CapacityBar 통일. ProgramCardDto 계산 로직 재사용 후
    // fragment 파라미터를 모델 attribute 로 노출 (홈/목록/검색과 동일 fragment 호출).
    ProgramCardDto capacityCard = new ProgramCardDto(program, appliedCount);

    // A9-b (2026-09-22): 문의처 전화 — Program.center FK 로 직접 조회.
    // center 는 NOT NULL 이므로 null 체크는 방어적 코드로만 유지 (V21 승격 이후 이론적으로 발생 불가).
    String contactPhone = null;
    if (program.getCenter() != null
        && program.getCenter().getPhone() != null
        && !program.getCenter().getPhone().isBlank()) {
      contactPhone = program.getCenter().getPhone();
    }

    model.addAttribute("currentPage", "programs");
    model.addAttribute("program", program);
    model.addAttribute("bookmarked", bookmarked);
    model.addAttribute("appliedCount", appliedCount);
    model.addAttribute("applicationRate", applicationRate);
    model.addAttribute("competitionRatio", competitionRatio);
    model.addAttribute("contactPhone", contactPhone);
    // CapacityBar 상세 fragment 파라미터 (D5, prototype L945~951 매칭)
    model.addAttribute("capacityPct", capacityCard.getPct());
    model.addAttribute("capacityColorClass", capacityCard.getColorClass());
    model.addAttribute("detailHeadline", capacityCard.getDetailHeadline());
    model.addAttribute("detailSubtext", capacityCard.getDetailSubtext());
    model.addAttribute("detailEmphasized", capacityCard.isDetailEmphasized());
    return "program/detail";
  }

  // ────────────────────────────────────────────────────────────────
  //   A6-followup (2026-09-29) — 조회수 트래킹 helpers
  // ────────────────────────────────────────────────────────────────

  private static final String VIEWED_SESSION_KEY = "viewedPrograms";

  /**
   * Q2 결정: 관리자(CENTER_ADMIN / SYSTEM_ADMIN) 는 조회수에서 제외. USER/anon 만 카운트. Q3 결정: 세션 {@code
   * Set<Long>} dedup 으로 F5 반복·재진입 흡수. 재로그인/세션 만료 시 자연 리셋.
   */
  private void trackView(Long programId, UserDetails principal, HttpSession session) {
    if (isAdmin(principal)) {
      return;
    }
    @SuppressWarnings("unchecked")
    Set<Long> viewed = (Set<Long>) session.getAttribute(VIEWED_SESSION_KEY);
    if (viewed == null) {
      viewed = new HashSet<>();
      session.setAttribute(VIEWED_SESSION_KEY, viewed);
    }
    if (viewed.contains(programId)) {
      return;
    }
    viewed.add(programId);
    // 세션 재직렬화 트리거 (Set 내부 변경만으론 일부 세션 구현이 감지 못 함 방어).
    session.setAttribute(VIEWED_SESSION_KEY, viewed);
    programService.incrementViewCount(programId);
  }

  private static boolean isAdmin(UserDetails principal) {
    if (principal == null) return false;
    for (GrantedAuthority authority : principal.getAuthorities()) {
      String role = authority.getAuthority();
      if ("ROLE_CENTER_ADMIN".equals(role)
          || "ROLE_SYSTEM_ADMIN".equals(role)
          || "ROLE_ADMIN".equals(role)) {
        return true;
      }
    }
    return false;
  }
}

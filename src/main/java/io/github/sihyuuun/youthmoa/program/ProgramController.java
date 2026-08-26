package io.github.sihyuuun.youthmoa.program;

import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.bookmark.BookmarkService;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ProgramController {

  private final ProgramService programService;
  private final BookmarkService bookmarkService;
  private final ApplicationRepository applicationRepository;
  private final CenterRepository centerRepository;

  @GetMapping("/programs")
  public String list(
      @RequestParam(required = false, defaultValue = "") String status,
      @RequestParam(name = "regions", required = false) List<String> regions,
      @RequestParam(name = "centers", required = false) List<String> centers,
      @RequestParam(required = false, defaultValue = "default") String sort,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestHeader(name = "HX-Request", required = false) String hxRequest,
      @AuthenticationPrincipal UserDetails principal,
      Model model) {

    List<String> safeRegions = regions == null ? Collections.emptyList() : regions;
    List<String> safeCenters = centers == null ? Collections.emptyList() : centers;

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

    // htmx 부분 갱신
    if (hxRequest != null && !hxRequest.isBlank()) {
      return "program/_list-fragment :: list-region";
    }
    return "program/list";
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
  // auto-commit 오류로 렌더 실패할 잠재 위험 → 컨트롤러 트랜잭션 부착으로 안전 확보.
  // 260826 chore/content-lob-to-text: content 는 이제 @JdbcTypeCode(LONGVARCHAR) 매핑 (PG text · H2
  // VARCHAR(MAX)) 이라 @Lob oid 스트리밍 케이스는 사라졌지만 lazy 관계·다른 fetch 사고 방어 목적으로 readOnly 트랜잭션 유지.
  @GetMapping("/programs/{id}")
  @Transactional(readOnly = true)
  public String detail(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails principal, Model model) {
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

    // 문의처 전화 — Program.organization → Center.phone 조회 (centers.csv 실 데이터).
    // 매칭 실패 시 null → 뷰에서 "문의처 미등록" fallback.
    String contactPhone =
        centerRepository
            .findByName(program.getOrganization())
            .map(io.github.sihyuuun.youthmoa.center.Center::getPhone)
            .filter(p -> p != null && !p.isBlank())
            .orElse(null);

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
}

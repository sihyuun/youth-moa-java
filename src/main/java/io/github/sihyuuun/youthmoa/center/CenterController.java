package io.github.sihyuuun.youthmoa.center;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * 청년센터 3-column 목록 + 인라인 상세 패널.
 *
 * <p>F0h-c2: `/centers` 와 `/centers/{id}` 를 단일 메서드로 처리. detailId 존재 시 인라인 상세 패널 렌더. 별도 `detail.html`
 * 라우트는 제거.
 */
@Controller
@RequiredArgsConstructor
public class CenterController {

  private final CenterService centerService;

  @Value("${youthmoa.kakao.map-app-key:}")
  private String kakaoMapAppKey;

  @GetMapping({"/centers", "/centers/{detailId}"})
  public String list(
      @PathVariable(required = false) Long detailId,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String region,
      @RequestParam(required = false, defaultValue = "false") boolean onlyActive,
      @RequestParam(required = false, defaultValue = "name") String sort,
      Model model) {
    List<CenterListItem> centers = centerService.list(q, region, onlyActive, sort);
    List<String> regions = centerService.distinctActiveRegions();

    Center detailCenter = null;
    Integer detailProgramCount = null;
    if (detailId != null) {
      detailCenter =
          centerService
              .findById(detailId)
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "청년센터를 찾을 수 없습니다."));
      detailProgramCount = centerService.programCountFor(detailCenter.getName());
    }

    model.addAttribute("currentPage", "centers");
    model.addAttribute("centers", centers);
    model.addAttribute("regions", regions);
    model.addAttribute("detailCenter", detailCenter);
    model.addAttribute("detailProgramCount", detailProgramCount);
    model.addAttribute("filterQ", q == null ? "" : q);
    model.addAttribute("filterRegion", region == null ? "" : region);
    model.addAttribute("filterOnlyActive", onlyActive);
    model.addAttribute("filterSort", sort);
    model.addAttribute("kakaoMapAppKey", kakaoMapAppKey);
    return "center/list";
  }

  /**
   * F0h-c2 개정(2026-07-09): 상세 패널 innerHTML fragment. `.centers-detail-col` 내부에 주입될 마크업만 반환.
   * `centers-detail.js` 가 카드 클릭 시 HTMX ajax 로 호출.
   */
  @GetMapping("/centers/{id}/detail-fragment")
  public String detailFragment(
      @PathVariable Long id,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String region,
      @RequestParam(required = false, defaultValue = "false") boolean onlyActive,
      @RequestParam(required = false, defaultValue = "name") String sort,
      Model model) {
    Center detailCenter =
        centerService
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "청년센터를 찾을 수 없습니다."));
    Integer detailProgramCount = centerService.programCountFor(detailCenter.getName());

    model.addAttribute("detailCenter", detailCenter);
    model.addAttribute("detailProgramCount", detailProgramCount);
    model.addAttribute("filterQ", q == null ? "" : q);
    model.addAttribute("filterRegion", region == null ? "" : region);
    model.addAttribute("filterOnlyActive", onlyActive);
    model.addAttribute("filterSort", sort);
    return "center/list-fragments :: detail-panel-content";
  }

  /**
   * F0h-c2 개정(2026-07-09): 카드 리스트 fragment. `.centers-list-scroll` 내부 innerHTML 로 주입.
   * compact=true 면 compact 카드, false 면 full 카드. activeId 일치 카드에 is-active 클래스 부여.
   */
  @GetMapping("/centers/cards")
  public String cardsFragment(
      @RequestParam(required = false, defaultValue = "false") boolean compact,
      @RequestParam(required = false) Long activeId,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String region,
      @RequestParam(required = false, defaultValue = "false") boolean onlyActive,
      @RequestParam(required = false, defaultValue = "name") String sort,
      Model model) {
    List<CenterListItem> centers = centerService.list(q, region, onlyActive, sort);
    model.addAttribute("centers", centers);
    model.addAttribute("compact", compact);
    model.addAttribute("activeId", activeId);
    model.addAttribute("filterQ", q == null ? "" : q);
    model.addAttribute("filterRegion", region == null ? "" : region);
    model.addAttribute("filterOnlyActive", onlyActive);
    model.addAttribute("filterSort", sort);
    return "center/list-fragments :: card-list-content";
  }
}

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
}

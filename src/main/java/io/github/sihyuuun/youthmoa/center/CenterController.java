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
 * 청년센터 목록·상세.
 *
 * <p>@Controller (vs @RestController) — Thymeleaf 뷰 렌더용. 반환 String 은 view name 으로 해석.
 *
 * <p>{@code kakaoMapAppKey} 는 application.yml 의 {@code youthmoa.kakao.map-app-key} 값. 미설정(빈 문자열) 이면
 * 템플릿의 {@code th:if} 로 카카오맵 SDK script 태그를 아예 안 렌더 → 지도 자리엔 fallback 문구 표시.
 */
@Controller
@RequiredArgsConstructor
public class CenterController {

  private final CenterService centerService;

  @Value("${youthmoa.kakao.map-app-key:}")
  private String kakaoMapAppKey;

  @GetMapping("/centers")
  public String list(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String region,
      @RequestParam(required = false, defaultValue = "false") boolean onlyActive,
      @RequestParam(required = false, defaultValue = "name") String sort,
      Model model) {
    List<CenterListItem> centers = centerService.list(q, region, onlyActive, sort);
    List<String> regions = centerService.distinctActiveRegions();

    model.addAttribute("currentPage", "centers");
    model.addAttribute("centers", centers);
    model.addAttribute("regions", regions);
    model.addAttribute("filterQ", q == null ? "" : q);
    model.addAttribute("filterRegion", region == null ? "" : region);
    model.addAttribute("filterOnlyActive", onlyActive);
    model.addAttribute("filterSort", sort);
    model.addAttribute("kakaoMapAppKey", kakaoMapAppKey);
    return "center/list";
  }

  @GetMapping("/centers/{id}")
  public String detail(@PathVariable Long id, Model model) {
    Center center =
        centerService
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "청년센터를 찾을 수 없습니다."));

    model.addAttribute("currentPage", "centers");
    model.addAttribute("center", center);
    model.addAttribute("kakaoMapAppKey", kakaoMapAppKey);
    return "center/detail";
  }
}

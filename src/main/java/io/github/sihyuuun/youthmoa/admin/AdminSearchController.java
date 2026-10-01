package io.github.sihyuuun.youthmoa.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * A8-search (2026-10-01) — 관리자 헤더 글로벌 검색 드롭다운 엔드포인트.
 *
 * <p>HTMX keyup (debounce 200ms) 에서 호출되어 fragment HTML 조각을 반환한다. 사용자 /search 와 분리된 전용 네임스페이스 (Q4 결정
 * — 공지 제외 · RBAC 격리 · 짧은 드롭다운 UX 전용).
 *
 * <p>GET /admin/search/dropdown?q=... — fragment: admin/fragments/_search-dropdown :: dropdown
 *
 * <p>인증: SYSTEM_ADMIN · CENTER_ADMIN 만 접근. 미인증 → 302 (SecurityConfig), USER → 403.
 */
@Controller
@RequestMapping("/admin/search")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN','CENTER_ADMIN')")
public class AdminSearchController {

  private final AdminSearchService adminSearchService;

  @GetMapping("/dropdown")
  public String dropdown(
      @RequestParam(name = "q", required = false, defaultValue = "") String q, Model model) {
    AdminSearchResult result = adminSearchService.search(q);
    model.addAttribute("result", result);
    // hasQuery: 빈 쿼리(공백 포함)는 드롭다운 자체를 닫힘 상태로 유지. 쿼리는 있는데 결과 0 일 때만
    // "검색 결과가 없어요" 안내를 열림 상태로 노출한다.
    model.addAttribute("hasQuery", q != null && !q.isBlank());
    return "admin/fragments/_search-dropdown :: dropdown";
  }
}

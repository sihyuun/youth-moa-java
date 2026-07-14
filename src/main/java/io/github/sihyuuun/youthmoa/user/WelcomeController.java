package io.github.sihyuuun.youthmoa.user;

import io.github.sihyuuun.youthmoa.region.Region;
import io.github.sihyuuun.youthmoa.region.RegionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * F-signup-03: /welcome 온보딩 화면.
 *
 * <p>prototype.tsx L1541~1591 이식. 관심 지역 12개 + 관심 분야 7개 토글 선택.
 *
 * <p>SecurityConfig 에서 /welcome 은 authenticated 로 매핑. signup 성공 후 자동 로그인 헬퍼 ({@link
 * #autoLogin(String, HttpServletRequest, HttpServletResponse, SecurityContextRepository)}) 로 진입.
 */
@Controller
@RequiredArgsConstructor
public class WelcomeController {

  private final RegionRepository regionRepository;
  private final UserService userService;

  @GetMapping("/welcome")
  public String welcome(Model model) {
    List<String> regionNames =
        regionRepository.findAllByIsFeaturedTrueOrderByNameAsc().stream().map(Region::getName).toList();
    model.addAttribute("welcomeRegions", regionNames);
    model.addAttribute("welcomeCategories", UserInterestCategory.ALL);
    if (!model.containsAttribute("welcomeForm")) {
      model.addAttribute("welcomeForm", new WelcomeForm());
    }
    return "user/welcome";
  }

  @PostMapping("/welcome")
  public String save(
      @ModelAttribute("welcomeForm") WelcomeForm form,
      @org.springframework.security.core.annotation.AuthenticationPrincipal UserDetails principal,
      RedirectAttributes redirectAttributes) {
    userService.updateInterests(principal.getUsername(), form.getRegions(), form.getCategories());
    redirectAttributes.addFlashAttribute("welcomeToast", "관심 정보가 저장되었어요. 딱 맞는 프로그램을 추천해드릴게요!");
    return "redirect:/?welcomed=personalized";
  }

  @PostMapping("/welcome/skip")
  public String skip(RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute("welcomeToast", "청년모아에 오신 걸 환영해요!");
    return "redirect:/?welcomed=skip";
  }

  /**
   * F-signup-03: signup 성공 후 자동 로그인. Spring Security 7 방식.
   *
   * <p>절차: (1) 세션 고정 방어를 위해 changeSessionId, (2)
   * UsernamePasswordAuthenticationToken.authenticated 로 토큰 생성, (3) SecurityContext 에 세팅, (4)
   * SecurityContextRepository 로 세션 반영 (7.x 필수).
   */
  public static void autoLogin(
      String email,
      UserService userService,
      HttpServletRequest request,
      HttpServletResponse response,
      SecurityContextRepository securityContextRepository) {
    UserDetails principal = userService.loadUserByUsername(email);
    // 세션 고정 공격 방어. changeSessionId 는 활성 세션 필요 → 없으면 먼저 세션 생성.
    if (request.getSession(false) == null) {
      request.getSession(true);
    } else {
      request.changeSessionId();
    }
    Authentication auth =
        UsernamePasswordAuthenticationToken.authenticated(
            principal, null, principal.getAuthorities());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, request, response);
  }
}

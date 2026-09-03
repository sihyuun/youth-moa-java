package io.github.sihyuuun.youthmoa.admin;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * A1 관리자 로그인 페이지. POST 는 Spring Security formLogin 이 처리하므로 GET 만 담당.
 *
 * <p>로그인 실패 시 SecurityConfig 의 failureHandler 가 세션에 {@code savedUsername} 을 저장 → 재렌더 시 아이디 인풋에 미리
 * 채워둔다.
 */
@Controller
public class AdminLoginController {

  @GetMapping("/admin/login")
  public String loginForm(HttpSession session, Model model) {
    Object saved = session.getAttribute("savedUsername");
    if (saved != null) {
      model.addAttribute("savedUsername", saved);
      session.removeAttribute("savedUsername");
    } else {
      model.addAttribute("savedUsername", "");
    }
    return "admin/login";
  }
}

package io.github.sihyuuun.youthmoa.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모바일 하단 탭바 active 판정용 currentTab 모델 attribute 를 모든 페이지에 자동 주입한다.
 *
 * <p>Claude Design 가이드 rev.2 F2 규칙: 경로 prefix 매칭, 쿼리스트링 무시.
 *
 * <ul>
 *   <li>{@code /} → {@code home}
 *   <li>{@code /programs}, {@code /programs/{id}}, {@code /programs/{id}/apply}, {@code
 *       /apply/complete} → {@code programs} (탭바는 루트만 렌더)
 *   <li>{@code /centers}, {@code /centers/{id}} → {@code centers}
 *   <li>{@code /mypage}, {@code /mypage?tab=*}, {@code /mypage/profile/edit} → {@code mypage}
 *   <li>{@code /search}, {@code /notifications}, {@code /notices}, 인증·정책 페이지 → {@code null} (탭바
 *       미렌더)
 * </ul>
 *
 * <p>템플릿에서 {@code th:if="${currentTab != null}"} 로 탭바 fragment include 를 조건부 처리한다.
 */
@ControllerAdvice(annotations = Controller.class)
public class LayoutAdvice {

  @ModelAttribute("currentTab")
  public String currentTab(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path == null) return null;
    // 260821: 탭바 렌더는 루트 4화면에만. 상세·폼·편집·완료 등 하위 페이지는 null 로 반환하여
    // fragments/tabbar :: tabbar include 자체를 skip (footer.html th:if 조건).
    if (path.equals("/")) return "home";
    if (path.equals("/programs")) return "programs";
    if (path.equals("/centers")) return "centers";
    // 마이페이지는 쿼리 파라미터 (?tab=X) 로 탭 전환하지만 여전히 루트. profile/edit 은 하위.
    if (path.equals("/mypage")) return "mypage";
    return null;
  }
}

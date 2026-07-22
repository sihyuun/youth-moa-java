package io.github.sihyuuun.youthmoa.render;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * U-COMMON-02 헤더 드롭다운 렌더 검증 (spec §PR-B, HANDOFF §4-S.3 D1~D3).
 *
 * <ul>
 *   <li>D2 A안: 프로필 카드 (아바타 + 이름 + 이메일, 클릭 시 /mypage) + 구분선 + 로그아웃
 *   <li>D1: 드롭다운 초기 hidden, aria-expanded="false", data-dropdown-trigger 부착
 *   <li>알림 벨도 동일 정합 (hover 폐지, click 토글)
 *   <li>정적 리소스 검증: {@code /js/common-ui.js} 에 {@code window.Dropdown} 노출, {@code /css/main.css} 에
 *       keyframes dropdown-enter + hover 규칙 부재
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class HeaderDropdownRenderTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  private org.springframework.test.web.servlet.request.RequestPostProcessor authed() {
    User u = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  @Test
  void 유저_드롭다운_프로필_카드_마크업_존재() throws Exception {
    mockMvc
        .perform(get("/").with(authed()))
        .andExpect(status().isOk())
        // D2 A안 프로필 카드 셸
        .andExpect(content().string(containsString("header-dropdown-profile")))
        .andExpect(content().string(containsString("header-dropdown-profile-avatar")))
        .andExpect(content().string(containsString("header-dropdown-profile-name")))
        .andExpect(content().string(containsString("header-dropdown-profile-email")))
        // 이메일 (UserPrincipal.email) 렌더
        .andExpect(content().string(containsString("seed1@youth-moa.test")))
        // /mypage 링크
        .andExpect(content().string(containsString("href=\"/mypage\"")))
        // 로그아웃 form 유지
        .andExpect(content().string(containsString("action=\"/logout\"")));
  }

  @Test
  void 유저_드롭다운_초기_hidden_aria_expanded_false() throws Exception {
    mockMvc
        .perform(get("/").with(authed()))
        .andExpect(status().isOk())
        // trigger data-* 및 aria-expanded 초기 false
        .andExpect(
            content().string(containsString("data-dropdown-trigger=\"header-user-dropdown\"")))
        .andExpect(
            content().string(containsString("data-dropdown-trigger=\"header-notif-dropdown\"")))
        .andExpect(content().string(containsString("aria-expanded=\"false\"")))
        // 패널 초기 hidden — id 뒤 마크업에 hidden 속성이 붙어 있는지 확인
        .andExpect(content().string(containsString("id=\"header-user-dropdown\"")))
        .andExpect(content().string(containsString("id=\"header-notif-dropdown\"")))
        .andExpect(content().string(containsString("header-bell-dropdown-wrap")))
        .andExpect(content().string(containsString(" hidden")));
  }

  @Test
  void common_ui_js_에_window_Dropdown_노출() throws Exception {
    mockMvc
        .perform(get("/js/common-ui.js"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("window.Dropdown")))
        .andExpect(content().string(containsString("dropdown-enter")))
        // Q7 — 다른 dropdown 자동 close
        .andExpect(content().string(containsString("closeOne")));
  }

  @Test
  void main_css_에_keyframes_dropdown_enter_존재_hover_규칙_부재() throws Exception {
    String css =
        mockMvc
            .perform(get("/css/main.css"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    // keyframes 정의
    org.assertj.core.api.Assertions.assertThat(css).contains("@keyframes dropdown-enter");
    org.assertj.core.api.Assertions.assertThat(css).contains(".dropdown-enter");
    // hover / focus-within 오픈 규칙 제거 확인 (D1 hover 폐지)
    org.assertj.core.api.Assertions.assertThat(css)
        .doesNotContain(".header-user-menu:hover")
        .doesNotContain(".header-user-menu:focus-within")
        .doesNotContain(".header-bell-menu:hover")
        .doesNotContain(".header-bell-menu:focus-within");
  }

  @Test
  void 유저_드롭다운_구_마크업_dead_class_부재() throws Exception {
    // 기존 header-dropdown-item 으로 렌더되던 "마이페이지" 링크는 프로필 카드로 대체됨.
    // 로그아웃 버튼만 header-dropdown-item 유지.
    String body =
        mockMvc
            .perform(get("/").with(authed()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    // "마이페이지" 라는 텍스트 라벨 (기존 링크 텍스트) 은 프로필 카드에서는 이름/이메일이 대체
    // 따라서 header-dropdown-profile 내부에 마이페이지 텍스트가 없어야 함
    org.assertj.core.api.Assertions.assertThat(body).doesNotContain(">마이페이지</a>");
  }
}

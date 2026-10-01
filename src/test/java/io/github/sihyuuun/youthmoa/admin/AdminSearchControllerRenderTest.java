package io.github.sihyuuun.youthmoa.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
 * A8-search (2026-10-01) — 관리자 헤더 글로벌 검색 fragment 렌더 + RBAC 회귀 방어.
 *
 * <p>검증 축: ① SYSTEM_ADMIN 쿼리 응답 200 + fragment 마크업 ② 비인증 → 302 ③ USER role → 403 ④ 빈 쿼리 → is-open
 * 미부여.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminSearchControllerRenderTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  @Test
  void SYSTEM_ADMIN_쿼리_있을_때_fragment_가_열림_상태로_렌더() throws Exception {
    User admin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(admin);

    mockMvc
        .perform(get("/admin/search/dropdown").param("q", "청년").with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("admin-header-search-dropdown")))
        .andExpect(content().string(containsString("is-open")))
        // Thymeleaf 표현식 잔존 금지 (렌더 실패 조기 감지)
        .andExpect(content().string(not(containsString("${"))))
        .andExpect(content().string(not(containsString("th:text"))));
  }

  @Test
  void 빈_쿼리는_닫힘_상태로_렌더() throws Exception {
    User admin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(admin);

    mockMvc
        .perform(get("/admin/search/dropdown").param("q", "").with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("admin-header-search-dropdown")))
        .andExpect(content().string(not(containsString("is-open"))))
        .andExpect(content().string(not(containsString("검색 결과가 없어요"))));
  }

  @Test
  void 결과_없는_쿼리는_빈_상태_메시지() throws Exception {
    User admin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(admin);

    mockMvc
        .perform(
            get("/admin/search/dropdown")
                .param("q", "zzzxyznonexistquery12345")
                .with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("is-open")))
        .andExpect(content().string(containsString("검색 결과가 없어요")));
  }

  @Test
  void 비인증_요청은_로그인_리다이렉트() throws Exception {
    mockMvc
        .perform(get("/admin/search/dropdown").param("q", "청년"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/login"));
  }

  @Test
  void USER_role_요청은_403() throws Exception {
    User seed = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(seed);
    mockMvc
        .perform(get("/admin/search/dropdown").param("q", "청년").with(user(principal)))
        .andExpect(status().isForbidden());
  }

  @Test
  void CENTER_ADMIN_도_200() throws Exception {
    User admin = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(admin);
    mockMvc
        .perform(get("/admin/search/dropdown").param("q", "청년").with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("admin-header-search-dropdown")));
  }
}

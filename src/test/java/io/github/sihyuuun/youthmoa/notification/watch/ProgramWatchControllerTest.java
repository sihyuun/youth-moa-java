package io.github.sihyuuun.youthmoa.notification.watch;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
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
 * A7-watcher-ui (2026-09-28) — {@link ProgramWatchController} RBAC + fragment 응답 검증.
 *
 * <p>{@code AdminUserControllerRbacTest} 패턴 승계: @SpringBootTest + e2e 프로파일로 실 SecurityFilterChain
 * + @PreAuthorize 를 통과시켜 CENTER_ADMIN 통과 · USER 403 · 익명 302 을 실제 필터체인 관점에서 검증.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class ProgramWatchControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;
  @Autowired ProgramRepository programRepository;

  @Test
  void POST_watch_toggle_anonymous_redirects_to_login() throws Exception {
    Program p = programRepository.findAll().stream().findFirst().orElseThrow();
    mockMvc
        .perform(post("/admin/programs/" + p.getId() + "/watch/toggle").with(csrf()))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  void POST_watch_toggle_USER_role_forbidden() throws Exception {
    User seed = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
    Program p = programRepository.findAll().stream().findFirst().orElseThrow();
    mockMvc
        .perform(
            post("/admin/programs/" + p.getId() + "/watch/toggle")
                .with(user(new UserPrincipal(seed)))
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void POST_watch_toggle_CENTER_ADMIN_returns_fragment() throws Exception {
    User center1 = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    Program p = programRepository.findAll().stream().findFirst().orElseThrow();
    mockMvc
        .perform(
            post("/admin/programs/" + p.getId() + "/watch/toggle")
                .with(user(new UserPrincipal(center1)))
                .with(csrf()))
        .andExpect(status().isOk())
        // fragment 안에 watch-btn 클래스 렌더 확인 (list-watch-btn 기본값)
        .andExpect(content().string(org.hamcrest.Matchers.containsString("watch-btn")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("list-watch-btn")));
  }

  @Test
  void POST_watch_toggle_SYSTEM_ADMIN_returns_fragment_detail_styleClass() throws Exception {
    // FOLLOW-admin-program-detail-readonly (2026-10-07 · Q8):
    //   watch-button 은 상세 헤더 전용으로 이관됨. styleClass hx-vals 왕복 자체는 임의 문자열에 대해 동일하게 유지돼야 함.
    User sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    Program p = programRepository.findAll().stream().findFirst().orElseThrow();
    mockMvc
        .perform(
            post("/admin/programs/" + p.getId() + "/watch/toggle")
                .param("styleClass", "detail-watch-btn")
                .with(user(new UserPrincipal(sysadmin)))
                .with(csrf()))
        .andExpect(status().isOk())
        // hx-vals 왕복 확인: detail-watch-btn styleClass 로 렌더
        .andExpect(content().string(org.hamcrest.Matchers.containsString("detail-watch-btn")));
  }
}

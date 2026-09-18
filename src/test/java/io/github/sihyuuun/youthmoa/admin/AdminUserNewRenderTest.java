package io.github.sihyuuun.youthmoa.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
 * A5-1 admin-staff-management (2026-09-18): {@code GET /admin/users/new} Thymeleaf 실 렌더 회귀 방어.
 *
 * <p>prototype L1946~2045 정합 (옵션 B 편입) — 이메일 · 이름 · 성별 · 권한 radio (사용자·관리자) · 관리자 sub-radio · 소속
 * 센터.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminUserNewRenderTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  private org.springframework.test.web.servlet.request.RequestPostProcessor sysadmin() {
    User u = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  @Test
  void GET_new_renders_form_with_prototype_fields() throws Exception {
    mockMvc
        .perform(get("/admin/users/new").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("신규 사용자 등록")))
        // 이메일 · 이름 · 성별
        .andExpect(content().string(containsString("data-testid=\"admin-user-new-email\"")))
        .andExpect(content().string(containsString("data-testid=\"admin-user-new-name\"")))
        .andExpect(content().string(containsString("name=\"gender\"")))
        // 권한 radio (사용자·관리자 2옵션 · prototype L1996~2005 정합)
        .andExpect(
            content().string(containsString("data-testid=\"admin-user-new-account-type-user\"")))
        .andExpect(
            content().string(containsString("data-testid=\"admin-user-new-account-type-admin\"")))
        // 관리자 sub-radio (CENTER_ADMIN · SYSTEM_ADMIN)
        .andExpect(content().string(containsString("data-testid=\"admin-user-new-role-center\"")))
        .andExpect(content().string(containsString("data-testid=\"admin-user-new-role-system\"")))
        // 소속 센터 select
        .andExpect(content().string(containsString("data-testid=\"admin-user-new-center\"")))
        // 자동생성 안내
        .andExpect(content().string(containsString("자동으로 생성")))
        // 제출 버튼
        .andExpect(content().string(containsString("data-testid=\"admin-user-new-submit\"")))
        // Thymeleaf 표현식 잔존 없음 — ${...} + th:*="..." 만 실제 미평가 표식.
        // (inline style 의 "width:" 는 substring "th:" 를 자연 포함하므로 검사에서 제외)
        .andExpect(content().string(not(containsString("${"))))
        .andExpect(content().string(not(containsString("th:action="))))
        .andExpect(content().string(not(containsString("th:each="))));
  }
}

package io.github.sihyuuun.youthmoa.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * FOLLOW-program-duplicate RBAC (ym-verify U1 보강): {@code POST /admin/programs/{id}/duplicate} 는
 * 메서드 레벨 {@code @PreAuthorize("hasRole('SYSTEM_ADMIN')")} 로 SYSTEM_ADMIN 전용. 클래스 레벨은 CENTER_ADMIN 도
 * 허용하지만 duplicate 액션은 정책상 SYSTEM_ADMIN only.
 *
 * <p>spec §7 (Q7) · AdminUserControllerRbacTest 와 동일 패턴. 어노테이션 정적 분석만으로는 실제 Spring Security filter
 * chain + method security 가 403 을 반환하는지 보장되지 않으므로 MockMvc 로 실측.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminProgramDuplicateControllerRbacTest {

  private static final long SEED_PROGRAM_ID = 1L;

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  @Test
  void POST_duplicate_anonymous_redirects_to_admin_login() throws Exception {
    mockMvc
        .perform(post("/admin/programs/" + SEED_PROGRAM_ID + "/duplicate").with(csrf()))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  void POST_duplicate_CENTER_ADMIN_forbidden() throws Exception {
    // Q7 — duplicate 는 SYSTEM_ADMIN 전용. 클래스 레벨 hasAnyRole 통과하지만
    // 메서드 레벨 @PreAuthorize("hasRole('SYSTEM_ADMIN')") 로 CENTER_ADMIN 차단.
    User center1 = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    mockMvc
        .perform(
            post("/admin/programs/" + SEED_PROGRAM_ID + "/duplicate")
                .with(user(new UserPrincipal(center1)))
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void POST_duplicate_USER_forbidden() throws Exception {
    // 일반 사용자는 클래스 레벨에서 이미 차단.
    User seed = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
    mockMvc
        .perform(
            post("/admin/programs/" + SEED_PROGRAM_ID + "/duplicate")
                .with(user(new UserPrincipal(seed)))
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void POST_duplicate_SYSTEM_ADMIN_302_redirect() throws Exception {
    // 긍정 경로: SYSTEM_ADMIN 은 복제 성공 → 복제본 편집 폼 redirect.
    User sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    mockMvc
        .perform(
            post("/admin/programs/" + SEED_PROGRAM_ID + "/duplicate")
                .with(user(new UserPrincipal(sysadmin)))
                .with(csrf()))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  void POST_duplicate_no_csrf_forbidden() throws Exception {
    // CSRF 방어: 토큰 없이 POST → 403.
    User sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    mockMvc
        .perform(
            post("/admin/programs/" + SEED_PROGRAM_ID + "/duplicate")
                .with(user(new UserPrincipal(sysadmin))))
        .andExpect(status().isForbidden());
  }
}

package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
 * FOLLOW-waitlist-auto-approve (2026-10-08): {@code POST /admin/programs/{id}/auto-approve}
 * round-trip 검증. Q1 A 토글이 DB 에 반영되는지 & 302 PRG 응답인지.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminProgramAutoApproveTest {

  private static final long PROGRAM_ID = 1L;

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;
  @Autowired ProgramRepository programRepository;

  private org.springframework.test.web.servlet.request.RequestPostProcessor sysadmin() {
    User u = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  @Test
  void POST_auto_approve_enabled_true_토글_켜짐_PRG_302() throws Exception {
    mockMvc
        .perform(
            post("/admin/programs/" + PROGRAM_ID + "/auto-approve")
                .param("enabled", "true")
                .with(sysadmin())
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/programs/" + PROGRAM_ID));

    Program reloaded = programRepository.findById(PROGRAM_ID).orElseThrow();
    assertThat(reloaded.isAutoApproveWhenFull()).isTrue();
  }

  @Test
  void POST_auto_approve_enabled_false_토글_꺼짐_PRG_302() throws Exception {
    // 선행: ON 상태로 세팅 후 OFF 라운드트립 확인
    Program p = programRepository.findById(PROGRAM_ID).orElseThrow();
    p.enableAutoApproveWhenFull();
    programRepository.save(p);

    mockMvc
        .perform(
            post("/admin/programs/" + PROGRAM_ID + "/auto-approve")
                .param("enabled", "false")
                .with(sysadmin())
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/programs/" + PROGRAM_ID));

    Program reloaded = programRepository.findById(PROGRAM_ID).orElseThrow();
    assertThat(reloaded.isAutoApproveWhenFull()).isFalse();
  }

  @Test
  void POST_auto_approve_익명_401_or_redirect() throws Exception {
    mockMvc
        .perform(
            post("/admin/programs/" + PROGRAM_ID + "/auto-approve")
                .param("enabled", "true")
                .with(csrf()))
        .andExpect(status().is3xxRedirection());
  }
}

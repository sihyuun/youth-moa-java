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
 * A3-1 admin-program-form (2026-09-10): {@code GET /admin/programs/new} 및 {@code GET
 * /admin/programs/{id}} 편집 폼 Thymeleaf 렌더 검증 (F0h-c2 사고 재발 방지).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminProgramFormRenderTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  private org.springframework.test.web.servlet.request.RequestPostProcessor sysadmin() {
    User u = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor centerAdmin() {
    User u = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  @Test
  void GET_admin_programs_new_렌더_3탭_및_필드() throws Exception {
    mockMvc
        .perform(get("/admin/programs/new").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("프로그램 신규 등록")))
        // 3탭 라벨
        .andExpect(content().string(containsString("data-tab-target=\"tab-info\"")))
        .andExpect(content().string(containsString("data-tab-target=\"tab-apply\"")))
        .andExpect(content().string(containsString("data-tab-target=\"tab-terms\"")))
        // 탭 1 필드
        .andExpect(content().string(containsString("name=\"title\"")))
        .andExpect(content().string(containsString("name=\"organization\"")))
        .andExpect(content().string(containsString("name=\"content\"")))
        .andExpect(content().string(containsString("name=\"description\"")))
        .andExpect(content().string(containsString("name=\"imageUrl\"")))
        // 탭 2 필드
        .andExpect(content().string(containsString("name=\"applyStartDate\"")))
        .andExpect(content().string(containsString("name=\"applyEndDate\"")))
        .andExpect(content().string(containsString("name=\"venue\"")))
        .andExpect(content().string(containsString("name=\"contact\"")))
        .andExpect(content().string(containsString("name=\"capacity\"")))
        .andExpect(content().string(containsString("name=\"approvalMode\"")))
        // 탭 3 필드
        .andExpect(content().string(containsString("name=\"termsService\"")))
        .andExpect(content().string(containsString("name=\"termsPrivacy\"")))
        .andExpect(content().string(containsString("name=\"termsMarketing\"")))
        // 저장 버튼 (신규 → 등록)
        .andExpect(content().string(containsString(">등록</button>")))
        // 신규 모드에서 삭제 모달 미노출
        .andExpect(content().string(not(containsString("program-delete-modal"))))
        // Thymeleaf 표현식 잔존 없음
        .andExpect(content().string(not(containsString("${"))))
        .andExpect(content().string(not(containsString("th:field"))));
  }

  @Test
  void GET_admin_programs_id_렌더_편집_모드_prefilled() throws Exception {
    mockMvc
        .perform(get("/admin/programs/1").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("프로그램 편집")))
        // 저장 버튼 (편집 → 저장)
        .andExpect(content().string(containsString(">저장</button>")))
        // 편집 모드에서 삭제 모달·버튼 노출
        .andExpect(content().string(containsString("program-delete-modal")))
        .andExpect(content().string(containsString("admin-btn--danger")))
        // F4/F0c 진입 링크 (Qn-B A)
        .andExpect(content().string(containsString("/admin/programs/1/eligibility")))
        .andExpect(content().string(containsString("/admin/programs/1/dynamic-fields")))
        // 시드 프로그램 #1 의 title 이 prefilled (취업역량 강화 워크숍)
        .andExpect(content().string(containsString("취업역량 강화 워크숍")));
  }

  @Test
  void POST_admin_programs_new_CENTER_ADMIN_403() throws Exception {
    // Qn-1 A: 등록은 SYSTEM_ADMIN only
    mockMvc
        .perform(get("/admin/programs/new").with(centerAdmin()))
        .andExpect(status().isForbidden());
  }

  @Test
  void GET_admin_programs_new_익명_login_리다이렉트() throws Exception {
    mockMvc.perform(get("/admin/programs/new")).andExpect(status().is3xxRedirection());
  }
}

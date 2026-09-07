package io.github.sihyuuun.youthmoa.admin;

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
 * A-admin-terms-crud (2026-09-04) — 관리자 약관 화면 Thymeleaf 실 렌더 회귀 방어.
 *
 * <p>list · new · edit 3 뷰. Qn-1 B (CENTER_ADMIN 조회 가능) 도 함께 검증.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminTermFormRenderTest {

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
  void GET_admin_terms_list_렌더_시드_2건_및_신규버튼() throws Exception {
    mockMvc
        .perform(get("/admin/terms").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("약관 관리")))
        .andExpect(content().string(containsString("+ 신규 등록")))
        .andExpect(content().string(containsString("SERVICE")))
        .andExpect(content().string(containsString("PRIVACY")))
        .andExpect(content().string(containsString("admin-term-code-pill")));
  }

  @Test
  void GET_admin_terms_list_centerAdmin_조회_가능_Qn1B() throws Exception {
    // 목록은 200. Qn-1 B: 신규 등록 버튼은 SYSTEM_ADMIN 만 노출.
    mockMvc
        .perform(get("/admin/terms").with(centerAdmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("약관 관리")));
  }

  @Test
  void GET_admin_terms_new_form_렌더() throws Exception {
    mockMvc
        .perform(get("/admin/terms/new").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("약관 등록")))
        .andExpect(content().string(containsString("name=\"code\"")))
        .andExpect(content().string(containsString("name=\"content\"")))
        .andExpect(content().string(containsString("name=\"contentPath\"")));
  }

  @Test
  void GET_admin_terms_new_centerAdmin_403_Qn1B() throws Exception {
    mockMvc.perform(get("/admin/terms/new").with(centerAdmin())).andExpect(status().isForbidden());
  }

  @Test
  void GET_admin_terms_1_editForm_렌더_bumpVersion_체크박스_및_readonly_code() throws Exception {
    mockMvc
        .perform(get("/admin/terms/1").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("약관 편집")))
        // Qn-6 A: bumpVersion 체크박스
        .andExpect(content().string(containsString("name=\"bumpVersion\"")))
        .andExpect(content().string(containsString("이번 수정은 개정입니다")))
        // Qn-9 A: code readonly
        .andExpect(content().string(containsString("readonly")));
  }

  @Test
  void GET_admin_terms_1_editForm_centerAdmin_조회_가능_수정불가배너() throws Exception {
    mockMvc
        .perform(get("/admin/terms/1").with(centerAdmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("약관 편집")))
        // Qn-1 B: CENTER_ADMIN 조회 가능하지만 UD 불가 안내 배너
        .andExpect(content().string(containsString("시스템 관리자만")));
  }
}

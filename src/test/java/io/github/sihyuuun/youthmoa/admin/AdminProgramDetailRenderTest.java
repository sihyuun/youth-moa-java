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

/** A2 (2026-09-09) — 관리자 프로그램 상세 페이지 렌더 검증. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminProgramDetailRenderTest {

  private static final long PROGRAM_ID = 1L;

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  private org.springframework.test.web.servlet.request.RequestPostProcessor sysadmin() {
    User u = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  @Test
  void GET_admin_program_detail_렌더_기본정보_하위관리_링크() throws Exception {
    mockMvc
        .perform(get("/admin/programs/" + PROGRAM_ID).with(sysadmin()))
        .andExpect(status().isOk())
        // 헤더 액션
        .andExpect(content().string(containsString("사용자 화면 미리보기")))
        // 편집·삭제 disabled (A3 이월)
        .andExpect(content().string(containsString("disabled")))
        // 카드 타이틀
        .andExpect(content().string(containsString(">기본 정보</h3>")))
        .andExpect(content().string(containsString(">하위 관리</h3>")))
        .andExpect(content().string(containsString(">자격요건</h3>")))
        .andExpect(content().string(containsString(">프로그램 설명</h3>")))
        // 하위 관리 링크 (F4 · F0c)
        .andExpect(
            content().string(containsString("/admin/programs/" + PROGRAM_ID + "/eligibility")))
        .andExpect(
            content().string(containsString("/admin/programs/" + PROGRAM_ID + "/dynamic-fields")))
        // 사용자 화면 미리보기 링크
        .andExpect(content().string(containsString("href=\"/programs/" + PROGRAM_ID + "\"")))
        // GNB 활성
        .andExpect(content().string(containsString("class=\"admin-nav-link active\">프로그램 관리</a>")))
        // Thymeleaf 표현식 잔존 없음
        .andExpect(content().string(not(containsString("${"))))
        .andExpect(content().string(not(containsString("th:each"))));
  }

  @Test
  void GET_admin_program_detail_없는_프로그램_404() throws Exception {
    mockMvc
        .perform(get("/admin/programs/999999").with(sysadmin()))
        .andExpect(status().isNotFound());
  }

  @Test
  void GET_admin_program_detail_익명_login_리다이렉트() throws Exception {
    mockMvc.perform(get("/admin/programs/" + PROGRAM_ID)).andExpect(status().is3xxRedirection());
  }
}

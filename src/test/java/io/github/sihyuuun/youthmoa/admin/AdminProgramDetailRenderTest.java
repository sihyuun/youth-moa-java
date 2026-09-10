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
 * A2 (2026-09-09) + A3-1 (2026-09-10 · Qn-A A: 상세 = 편집 폼): {@code GET /admin/programs/{id}} 는 이제 편집 폼을
 * 렌더한다. 기존 read-only detail 페이지는 폐기됨. 신규 render 검증은 {@link AdminProgramFormRenderTest} 에도 있음.
 */
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
  void GET_admin_program_detail_는_편집_폼으로_렌더된다() throws Exception {
    mockMvc
        .perform(get("/admin/programs/" + PROGRAM_ID).with(sysadmin()))
        .andExpect(status().isOk())
        // 편집 모드 타이틀
        .andExpect(content().string(containsString("프로그램 편집")))
        // 3탭
        .andExpect(content().string(containsString(">프로그램 정보</button>")))
        .andExpect(content().string(containsString(">신청 정보</button>")))
        .andExpect(content().string(containsString(">약관 정보</button>")))
        // 하위 관리 링크 (F4 · F0c)
        .andExpect(
            content().string(containsString("/admin/programs/" + PROGRAM_ID + "/eligibility")))
        .andExpect(
            content().string(containsString("/admin/programs/" + PROGRAM_ID + "/dynamic-fields")))
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

package io.github.sihyuuun.youthmoa.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * F4-admin-eligibility (2026-09-09) — 편집 폼 Thymeleaf 실 렌더 회귀 방어.
 *
 * <p>배경 (F0h-c2 회고): {@code compileJava} + {@code JpaMappingTest} 만으로는 Thymeleaf 파싱·SpEL·fragment
 * 렌더 이슈를 감지 못 함. RBAC 매트릭스도 여기서 함께 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminProgramEligibilityFormRenderTest {

  private static final long PROGRAM_ID = 7L;

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
  void GET_admin_eligibility_form_렌더_prefilled_3필드() throws Exception {
    mockMvc
        .perform(get("/admin/programs/" + PROGRAM_ID + "/eligibility").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("자격요건 편집")))
        // 3필드 name 속성
        .andExpect(content().string(containsString("name=\"age\"")))
        .andExpect(content().string(containsString("name=\"region\"")))
        .andExpect(content().string(containsString("name=\"etc\"")))
        // maxlength 검증
        .andExpect(content().string(containsString("maxlength=\"100\"")))
        .andExpect(content().string(containsString("maxlength=\"200\"")))
        // seed program #7 = 청년 문화예술 스쿨: age = "만 19세 ~ 39세 청년"
        .andExpect(content().string(containsString("만 19세 ~ 39세 청년")))
        // 라벨
        .andExpect(content().string(containsString(">연령<")))
        .andExpect(content().string(containsString(">거주지<")))
        .andExpect(content().string(containsString(">기타 조건<")))
        // 저장 버튼
        .andExpect(content().string(containsString(">저장<")))
        // Thymeleaf 표현식 잔존 없음
        .andExpect(content().string(not(containsString("${"))))
        .andExpect(content().string(not(containsString("th:each"))));
  }

  @Test
  void GET_admin_eligibility_form_없는_프로그램_404() throws Exception {
    mockMvc
        .perform(get("/admin/programs/999999/eligibility").with(sysadmin()))
        .andExpect(status().isNotFound());
  }

  // ================= RBAC (Qn-1 A) =================

  @Test
  void GET_admin_eligibility_form_centerAdmin_403() throws Exception {
    mockMvc
        .perform(get("/admin/programs/" + PROGRAM_ID + "/eligibility").with(centerAdmin()))
        .andExpect(status().isForbidden());
  }

  @Test
  void POST_admin_eligibility_centerAdmin_403() throws Exception {
    mockMvc
        .perform(
            post("/admin/programs/" + PROGRAM_ID + "/eligibility")
                .with(centerAdmin())
                .with(csrf())
                .param("age", "차단")
                .param("region", "차단")
                .param("etc", "차단"))
        .andExpect(status().isForbidden());
  }

  @Test
  void GET_admin_eligibility_form_익명_login_리다이렉트() throws Exception {
    mockMvc
        .perform(get("/admin/programs/" + PROGRAM_ID + "/eligibility"))
        .andExpect(status().is3xxRedirection());
  }

  // ================= PRG redirect (Qn-8 A) =================

  @Test
  void POST_admin_eligibility_sysadmin_302_redirect_self() throws Exception {
    mockMvc
        .perform(
            post("/admin/programs/" + PROGRAM_ID + "/eligibility")
                .with(sysadmin())
                .with(csrf())
                .param("age", "만 20세 ~ 30세")
                .param("region", "테스트 지역")
                .param("etc", "테스트 기타"))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                .string("Location", "/admin/programs/" + PROGRAM_ID + "/eligibility"));

    // 원복
    mockMvc
        .perform(
            post("/admin/programs/" + PROGRAM_ID + "/eligibility")
                .with(sysadmin())
                .with(csrf())
                .param("age", "만 19세 ~ 39세 청년")
                .param("region", "의왕시 거주 또는 활동")
                .param("etc", "문화예술 입문자 대상"))
        .andExpect(status().is3xxRedirection());
  }
}

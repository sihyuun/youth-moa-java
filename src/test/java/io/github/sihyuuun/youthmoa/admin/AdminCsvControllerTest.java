package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * A8 admin-bulk-csv (2026-09-17): {@link AdminCsvController} CSV export 검증.
 *
 * <p>확인 항목:
 *
 * <ul>
 *   <li>P-CSV-2: UTF-8 BOM (0xEF 0xBB 0xBF) prefix
 *   <li>Content-Type: text/csv; charset=UTF-8
 *   <li>Content-Disposition: attachment; filename=...
 *   <li>Users CSV = SYSTEM_ADMIN 전용 (CENTER_ADMIN 은 403)
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminCsvControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  @Test
  void GET_users_csv_returns_200_with_bom() throws Exception {
    User sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    MvcResult result =
        mockMvc
            .perform(get("/admin/users/export.csv").with(user(new UserPrincipal(sysadmin))))
            .andReturn();
    HttpServletResponse resp = result.getResponse();
    assertThat(resp.getStatus()).isEqualTo(200);
    assertThat(resp.getContentType()).contains("text/csv");
    assertThat(resp.getHeader("Content-Disposition")).contains("attachment");
    assertThat(resp.getHeader("Content-Disposition")).contains("filename=\"users_");

    byte[] body = result.getResponse().getContentAsByteArray();
    assertThat(body.length).isGreaterThan(3);
    // UTF-8 BOM
    assertThat(body[0] & 0xFF).isEqualTo(0xEF);
    assertThat(body[1] & 0xFF).isEqualTo(0xBB);
    assertThat(body[2] & 0xFF).isEqualTo(0xBF);
    String content = new String(body, java.nio.charset.StandardCharsets.UTF_8);
    // BOM 이후 헤더 첫 줄
    assertThat(content).contains("id");
    assertThat(content).contains("email");
    assertThat(content).contains("role");
  }

  @Test
  void GET_users_csv_center_admin_forbidden() throws Exception {
    User center = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    mockMvc
        .perform(get("/admin/users/export.csv").with(user(new UserPrincipal(center))))
        .andExpect(r -> assertThat(r.getResponse().getStatus()).isEqualTo(403));
  }

  @Test
  void GET_programs_csv_returns_200() throws Exception {
    User sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    MvcResult result =
        mockMvc
            .perform(get("/admin/programs/export.csv").with(user(new UserPrincipal(sysadmin))))
            .andReturn();
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getContentType()).contains("text/csv");
    String content =
        result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    assertThat(content).contains("title");
    // A9-b (2026-09-22): CSV 헤더 organization → centerName (Q-A9-b-4)
    assertThat(content).contains("centerName");
    assertThat(content).contains("capacity");
  }

  @Test
  void GET_applications_csv_returns_200() throws Exception {
    User sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    MvcResult result =
        mockMvc
            .perform(
                get("/admin/programs/1/applications/export.csv")
                    .with(user(new UserPrincipal(sysadmin))))
            .andReturn();
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    String content =
        result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    assertThat(content).contains("applicantEmail");
    assertThat(content).contains("appliedAt");
  }

  @Test
  void GET_users_csv_with_ids_restricts_export() throws Exception {
    User sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    Long id = sysadmin.getId();
    MvcResult result =
        mockMvc
            .perform(
                get("/admin/users/export.csv")
                    .param("ids", String.valueOf(id))
                    .with(user(new UserPrincipal(sysadmin))))
            .andReturn();
    String content =
        result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    // 헤더 라인 + 1 데이터 라인만 존재 (BOM 포함)
    long dataLines = content.lines().filter(l -> !l.isBlank()).count() - 1; // 헤더 제외
    assertThat(dataLines).isEqualTo(1);
  }
}

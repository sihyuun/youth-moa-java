package io.github.sihyuuun.youthmoa.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A1: /admin/login 실 Thymeleaf 렌더 회귀 방어 (F0h-c2 사고 재발 방지).
 *
 * <p>MockMvc 로 실제 뷰 리졸빙을 통과시켜 SpEL/파싱 오류를 잡는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminLoginRenderTest {

  @Autowired MockMvc mockMvc;

  @Test
  void 관리자_로그인_페이지가_필수_마크업을_렌더한다() throws Exception {
    mockMvc
        .perform(get("/admin/login"))
        .andExpect(status().isOk())
        // Auth Header + 뱃지
        .andExpect(content().string(containsString("admin-auth-header")))
        .andExpect(content().string(containsString(">ADMIN<")))
        // 타이틀
        .andExpect(content().string(containsString("관리자 로그인")))
        .andExpect(content().string(containsString("청년모아 관리자 페이지에 오신 것을 환영합니다")))
        // Form
        .andExpect(content().string(containsString("id=\"adminLoginForm\"")))
        .andExpect(content().string(containsString("action=\"/admin/login\"")))
        .andExpect(content().string(containsString("name=\"username\"")))
        .andExpect(content().string(containsString("name=\"password\"")))
        // 사용자 페이지 링크 (아이디/비밀번호 찾기)
        .andExpect(content().string(containsString("/find-id")))
        .andExpect(content().string(containsString("/find-password")))
        // A1 미포함 (deviation): 회원가입 버튼 부재
        .andExpect(content().string(not(containsString(">회원가입<"))))
        // CSS 로드
        .andExpect(content().string(containsString("/css/admin.css")))
        // CSRF meta
        .andExpect(content().string(containsString("name=\"_csrf\"")))
        // Thymeleaf 표현식 잔존 검사
        .andExpect(content().string(not(containsString("${"))));
  }

  @Test
  void error_파라미터일_때_에러_alert_노출() throws Exception {
    mockMvc
        .perform(get("/admin/login").param("error", ""))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("admin-auth-alert--error")))
        .andExpect(content().string(containsString("아이디 또는 비밀번호가 올바르지 않습니다.")));
  }

  @Test
  void logout_파라미터일_때_로그아웃_alert_노출() throws Exception {
    mockMvc
        .perform(get("/admin/login").param("logout", ""))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("admin-auth-alert--success")))
        .andExpect(content().string(containsString("로그아웃되었습니다.")));
  }
}

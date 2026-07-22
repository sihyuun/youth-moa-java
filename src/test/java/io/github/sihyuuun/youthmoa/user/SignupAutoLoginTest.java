package io.github.sihyuuun.youthmoa.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * F-signup-03 §A-Q6=a: signup 성공 후 자동 로그인.
 *
 * <p>POST /signup 응답이 /welcome 으로 302 하고, 세션에 SecurityContext 가 저장되어 있는지 확인.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class SignupAutoLoginTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  @Test
  void POST_signup_302_welcome_세션에_SecurityContext_저장() throws Exception {
    // e2e 프로파일에서 시드 이메일과 겹치지 않는 새 email 사용
    String email = "auto-login-test@youth-moa.test";
    // 이미 있으면 삭제 후 진행 (반복 실행 대비)
    userRepository.findByEmail(email).ifPresent(userRepository::delete);

    // F-signup-01: signup 은 세션 phoneVerifiedAt / phoneVerifiedNumber 를 재확인.
    org.springframework.mock.web.MockHttpSession preSession =
        new org.springframework.mock.web.MockHttpSession();
    preSession.setAttribute(
        PhoneVerificationController.SESSION_KEY_VERIFIED_AT, LocalDateTime.now());
    preSession.setAttribute(PhoneVerificationController.SESSION_KEY_VERIFIED_NUMBER, "01099998888");

    MvcResult result =
        mockMvc
            .perform(
                post("/signup")
                    .session(preSession)
                    .with(csrf())
                    .param("email", email)
                    .param("password", "abc12345")
                    .param("passwordConfirm", "abc12345")
                    .param("name", "자동로그인테스트")
                    .param("phone", "01099998888")
                    .param("gender", "MALE")
                    .param("birthDateText", "1995-01-01")
                    .param("zipcode", "12345")
                    .param("address", "경기도 수원시")
                    .param("addressDetail", "101호")
                    .param("termsAgreed", "true")
                    .param("privacyAgreed", "true")
                    .param("emailChecked", "true"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/welcome"))
            .andReturn();

    HttpSession session = result.getRequest().getSession(false);
    org.junit.jupiter.api.Assertions.assertNotNull(session, "세션 없음");
    Object ctx =
        session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    org.junit.jupiter.api.Assertions.assertTrue(
        ctx instanceof SecurityContext, "SecurityContext 미저장: " + ctx);
    SecurityContext sc = (SecurityContext) ctx;
    org.junit.jupiter.api.Assertions.assertNotNull(sc.getAuthentication(), "Authentication null");
    org.junit.jupiter.api.Assertions.assertEquals(
        email,
        ((org.springframework.security.core.userdetails.UserDetails)
                sc.getAuthentication().getPrincipal())
            .getUsername(),
        "principal username 불일치");

    // response 는 사용 안 하지만 참조 유지 (경고 방지)
    MockHttpServletResponse response = result.getResponse();
    org.junit.jupiter.api.Assertions.assertEquals(302, response.getStatus());

    // 뒷정리
    userRepository.findByEmail(email).ifPresent(userRepository::delete);
  }
}

package io.github.sihyuuun.youthmoa.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * F-signup-01: 회원가입 시 세션 phoneVerifiedAt/Number 3중 검증.
 *
 * <p>4 케이스:
 *
 * <ul>
 *   <li>세션 없음 → 폼으로 재렌더 (200)
 *   <li>세션 만료 → 폼으로 재렌더 (200)
 *   <li>세션 번호와 폼 phone 불일치 → 폼으로 재렌더 (200)
 *   <li>정상 → /welcome redirect (302)
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class SignupPhoneVerifiedTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  private static final String BASE_EMAIL_PREFIX = "sp-phone-verified-";

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder buildPost(
      String email, String phone) {
    return post("/signup")
        .with(csrf())
        .param("email", email)
        .param("password", "abc12345")
        .param("passwordConfirm", "abc12345")
        .param("name", "홍길동")
        .param("phone", phone)
        .param("gender", "MALE")
        .param("birthDateText", "1995-01-01")
        .param("zipcode", "12345")
        .param("address", "경기도 수원시")
        .param("addressDetail", "101호")
        .param("termsAgreed", "true")
        .param("privacyAgreed", "true")
        .param("emailChecked", "true");
  }

  private void cleanup(String email) {
    userRepository.findByEmail(email).ifPresent(userRepository::delete);
  }

  @Test
  @DisplayName("세션 인증정보 없음 → signup 폼 재렌더 (302 없음)")
  void 세션_없음() throws Exception {
    String email = BASE_EMAIL_PREFIX + "no-session@test";
    cleanup(email);
    mockMvc.perform(buildPost(email, "01011112222")).andExpect(status().isOk());
    // 저장 안 됨
    org.junit.jupiter.api.Assertions.assertTrue(userRepository.findByEmail(email).isEmpty());
  }

  @Test
  @DisplayName("세션 만료 (31분 전) → 폼 재렌더")
  void 세션_만료() throws Exception {
    String email = BASE_EMAIL_PREFIX + "expired@test";
    cleanup(email);
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(
        PhoneVerificationController.SESSION_KEY_VERIFIED_AT, LocalDateTime.now().minusMinutes(31));
    session.setAttribute(PhoneVerificationController.SESSION_KEY_VERIFIED_NUMBER, "01011112222");

    mockMvc.perform(buildPost(email, "01011112222").session(session)).andExpect(status().isOk());
    org.junit.jupiter.api.Assertions.assertTrue(userRepository.findByEmail(email).isEmpty());
  }

  @Test
  @DisplayName("세션 번호와 폼 phone 불일치 → 폼 재렌더")
  void 번호_불일치() throws Exception {
    String email = BASE_EMAIL_PREFIX + "mismatch@test";
    cleanup(email);
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(PhoneVerificationController.SESSION_KEY_VERIFIED_AT, LocalDateTime.now());
    session.setAttribute(PhoneVerificationController.SESSION_KEY_VERIFIED_NUMBER, "01099998888");

    mockMvc.perform(buildPost(email, "01011112222").session(session)).andExpect(status().isOk());
    org.junit.jupiter.api.Assertions.assertTrue(userRepository.findByEmail(email).isEmpty());
  }

  @Test
  @DisplayName("정상 세션 → 회원가입 성공 + phoneVerified=true 저장")
  void 정상() throws Exception {
    String email = BASE_EMAIL_PREFIX + "ok@test";
    cleanup(email);
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(PhoneVerificationController.SESSION_KEY_VERIFIED_AT, LocalDateTime.now());
    session.setAttribute(PhoneVerificationController.SESSION_KEY_VERIFIED_NUMBER, "01011112222");

    mockMvc
        .perform(buildPost(email, "01011112222").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/welcome"));

    User saved =
        userRepository.findByEmail(email).orElseThrow(() -> new AssertionError("가입된 사용자 없음"));
    org.junit.jupiter.api.Assertions.assertTrue(
        saved.isPhoneVerified(), "phoneVerified=true 이어야 함");
    cleanup(email);
  }
}

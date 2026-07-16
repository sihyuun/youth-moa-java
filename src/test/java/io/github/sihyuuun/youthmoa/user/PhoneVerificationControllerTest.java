package io.github.sihyuuun.youthmoa.user;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.sihyuuun.youthmoa.common.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.test.web.servlet.MockMvc;

/**
 * F-signup-01 PhoneVerificationController @WebMvcTest.
 *
 * <p>SecurityConfig 를 함께 로드해 CSRF·permitAll 매칭까지 검증.
 */
@WebMvcTest(PhoneVerificationController.class)
@Import(SecurityConfig.class)
class PhoneVerificationControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean PhoneVerificationService phoneVerificationService;
  @MockitoBean SmsRateLimiter smsRateLimiter;

  // SecurityConfig 가 요구하는 빈들 mock
  @MockitoBean UserDetailsService userDetailsService;
  @MockitoBean PasswordEncoder passwordEncoder;
  @MockitoBean PersistentTokenRepository persistentTokenRepository;
  // HeaderNotificationAdvice 등 @ControllerAdvice 가 자동 로드되며 의존하는 서비스도 mock 필요
  @MockitoBean io.github.sihyuuun.youthmoa.notification.NotificationService notificationService;
  @MockitoBean UserRepository userRepository;

  @Test
  void send_code_정상() throws Exception {
    when(smsRateLimiter.tryAcquire(anyString())).thenReturn(true);
    doNothing().when(phoneVerificationService).sendCode(anyString());

    mockMvc
        .perform(
            post("/api/phone/send-code")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"01011112222\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true));
  }

  @Test
  void send_code_rate_limit_429() throws Exception {
    when(smsRateLimiter.tryAcquire(anyString())).thenReturn(false);

    mockMvc
        .perform(
            post("/api/phone/send-code")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"01011112222\"}"))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void send_code_형식오류_400() throws Exception {
    when(smsRateLimiter.tryAcquire(anyString())).thenReturn(true);
    doThrow(new IllegalArgumentException("올바른 휴대폰 번호 형식이 아닙니다."))
        .when(phoneVerificationService)
        .sendCode(anyString());

    mockMvc
        .perform(
            post("/api/phone/send-code")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"12345\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void verify_code_성공() throws Exception {
    when(phoneVerificationService.verifyCode(anyString(), anyString())).thenReturn(true);

    mockMvc
        .perform(
            post("/api/phone/verify-code")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"01011112222\",\"code\":\"123456\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true));
  }

  @Test
  void verify_code_실패_400() throws Exception {
    when(phoneVerificationService.verifyCode(anyString(), anyString())).thenReturn(false);

    mockMvc
        .perform(
            post("/api/phone/verify-code")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"01011112222\",\"code\":\"999999\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.ok").value(false));
  }

  @Test
  void csrf_없으면_403() throws Exception {
    mockMvc
        .perform(
            post("/api/phone/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"01011112222\"}"))
        .andExpect(status().isForbidden());
  }
}

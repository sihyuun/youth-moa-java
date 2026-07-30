package io.github.sihyuuun.youthmoa.user;

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
 * F-signup-01: signup.html 렌더 회귀 방어.
 *
 * <p>2026-07-16 재디자인: 사이드 버튼 1개 (btn-send-code) + 재전송 버튼 제거, 타이머 input 내부 배치, 완료 배지가 code-row 자리를
 * 대체.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class SignupRenderTest {

  @Autowired MockMvc mockMvc;

  @Test
  void signup_페이지가_인증_UI_마크업을_렌더한다() throws Exception {
    mockMvc
        .perform(get("/signup"))
        .andExpect(status().isOk())
        // 사이드 버튼 1개 (인증요청/재요청/재인증 라벨 전환)
        .andExpect(content().string(containsString("id=\"btn-send-code\"")))
        // 재전송 버튼은 완전히 제거됨
        .andExpect(content().string(not(containsString("id=\"btn-resend-code\""))))
        // 코드 행 · 확인 버튼
        .andExpect(content().string(containsString("id=\"code-row\"")))
        .andExpect(content().string(containsString("id=\"verify-code\"")))
        .andExpect(content().string(containsString("id=\"btn-verify-code\"")))
        // 코드 input wrap + 내부 타이머
        .andExpect(content().string(containsString("signup-code-input-wrap")))
        .andExpect(content().string(containsString("id=\"code-timer\"")))
        .andExpect(content().string(containsString("signup-code-timer")))
        // 완료 배지 (hidden 초기 상태) + 문구
        .andExpect(content().string(containsString("id=\"phone-verified-badge\"")))
        .andExpect(content().string(containsString("휴대폰 인증이 완료되었어요.")))
        // 새 placeholder
        .andExpect(content().string(containsString("인증번호 6자리")))
        // 기존 유지
        .andExpect(content().string(containsString("id=\"postcodeSearchBtn\"")))
        // CSRF meta
        .andExpect(content().string(containsString("name=\"_csrf\"")))
        // JS 로드
        .andExpect(content().string(containsString("/js/signup-phone.js")));
  }

  @Test
  void signup_페이지가_카드형_마크업과_공통_토큰을_사용한다() throws Exception {
    // F0f-fix-6 후속: signup-section → form-card, signup-input → form-input 통합.
    // 회귀 방어 — 옛 클래스가 되살아나면 실패.
    mockMvc
        .perform(get("/signup"))
        .andExpect(status().isOk())
        // 카드 3개 (계정 정보·개인 정보·이용약관 동의) — 공통 .form-card 사용
        .andExpect(content().string(containsString("class=\"form-card\"")))
        // 통일된 .form-input 사용 (profile-edit 와 공유)
        .andExpect(content().string(containsString("class=\"form-input\"")))
        // 성별 pill (fullWidth 카드형)
        .andExpect(content().string(containsString("signup-gender-pill")))
        // 로그인 링크 (재디자인 후 하단 배치)
        .andExpect(content().string(containsString("signup-login-link")))
        // 옛 로컬 클래스 부재 (회귀 방어)
        .andExpect(content().string(not(containsString("signup-section"))))
        .andExpect(content().string(not(containsString("class=\"signup-input"))));
  }

  @Test
  void signup_페이지가_활성_약관을_동적_렌더한다() throws Exception {
    // F-signup-terms-agreement: DataInitializer 가 시드한 SERVICE·PRIVACY 2건이 나와야 한다.
    // 마스터 토글·(필수) 라벨·약관보기 링크는 UX 축이라 계약과 무관하게 회귀 방어.
    mockMvc
        .perform(get("/signup"))
        .andExpect(status().isOk())
        // 전체 동의 (마스터 토글) — 하드코딩 마크업 유지
        .andExpect(content().string(containsString("id=\"agreeAll\"")))
        .andExpect(content().string(containsString("전체 동의")))
        // 활성 약관 2건 동적 렌더 — 안정 셀렉터 data-term-code
        .andExpect(content().string(containsString("data-term-code=\"SERVICE\"")))
        .andExpect(content().string(containsString("data-term-code=\"PRIVACY\"")))
        // 폼 name 은 Map 바인딩 규약
        .andExpect(content().string(containsString("name=\"agreements[SERVICE]\"")))
        .andExpect(content().string(containsString("name=\"agreements[PRIVACY]\"")))
        // 필수 라벨 + 약관보기
        .andExpect(content().string(containsString("(필수)")))
        .andExpect(content().string(containsString("약관보기")))
        // 옛 하드코딩 name 이 사라졌는지 (회귀 방어)
        .andExpect(content().string(not(containsString("name=\"termsAgreed\""))))
        .andExpect(content().string(not(containsString("name=\"privacyAgreed\""))));
  }
}

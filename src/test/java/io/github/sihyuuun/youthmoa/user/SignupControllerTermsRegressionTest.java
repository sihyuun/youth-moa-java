package io.github.sihyuuun.youthmoa.user;

import static org.hamcrest.Matchers.containsString;
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
 * A-admin-terms-crud (2026-09-04) 회원가입 회귀 방지.
 *
 * <p>admin 트랙 조작 (약관 추가·비활성 전환) 이 signup GET/POST 500 을 일으키면 안 된다. 이 테스트는 시드 상태 (활성 필수 2건) 를 기준으로만
 * 검증하되, admin CRUD 도입 후 signup 페이지가 200 을 유지하고 term.content 를 렌더에 embed 하는지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class SignupControllerTermsRegressionTest {

  @Autowired MockMvc mockMvc;

  @Test
  void GET_signup_렌더_시드_필수2건_data_term_content_embed() throws Exception {
    mockMvc
        .perform(get("/signup"))
        .andExpect(status().isOk())
        // 활성 필수 약관 2건 렌더
        .andExpect(content().string(containsString("data-term-code=\"SERVICE\"")))
        .andExpect(content().string(containsString("data-term-code=\"PRIVACY\"")))
        // A-admin-terms-crud Qn-2 B: term.content 를 data-term-content 로 embed
        .andExpect(content().string(containsString("data-term-content")));
  }
}

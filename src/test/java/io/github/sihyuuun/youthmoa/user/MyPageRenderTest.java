package io.github.sihyuuun.youthmoa.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** admin-security 회귀 방어: /mypage 렌더 확인 (Thymeleaf 파싱 오류 검출용). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class MyPageRenderTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  private org.springframework.test.web.servlet.request.RequestPostProcessor authed() {
    User u = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  @Test
  void mypage_history_렌더() throws Exception {
    mockMvc.perform(get("/mypage").with(authed())).andExpect(status().isOk());
  }

  @Test
  void mypage_favorites_렌더() throws Exception {
    mockMvc.perform(get("/mypage?tab=favorites").with(authed())).andExpect(status().isOk());
  }
}

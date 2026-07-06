package io.github.sihyuuun.youthmoa.user;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** F0i: 아이디/비밀번호 찾기 컨트롤러 단위 검증. */
@WebMvcTest(FindAccountController.class)
@Import(FindAccountControllerTest.SecurityDisableConfig.class)
class FindAccountControllerTest {

  @Autowired MockMvc mockMvc;
  @MockitoBean FindAccountService findAccountService;

  private User user;

  @BeforeEach
  void setUp() throws Exception {
    user = User.builder().email("seeder@youth-moa.test").name("시드유저1").phone("01000000001").build();
    Field id = User.class.getDeclaredField("id");
    id.setAccessible(true);
    id.set(user, 42L);
  }

  @Test
  void GET_findId_returns_200() throws Exception {
    mockMvc
        .perform(get("/find-id"))
        .andExpect(status().isOk())
        .andExpect(view().name("user/find-id"))
        .andExpect(model().attributeExists("findIdRequest"));
  }

  @Test
  void POST_findId_매칭시_결과화면_마스킹_이메일_렌더() throws Exception {
    given(findAccountService.findEmailByNameAndPhone(anyString(), anyString()))
        .willReturn(Optional.of(user));

    mockMvc
        .perform(post("/find-id").param("name", "시드유저1").param("phone", "01000000001"))
        .andExpect(status().isOk())
        .andExpect(view().name("user/find-id-result"))
        .andExpect(model().attribute("maskedEmail", "see***@youth-moa.test"));
  }

  @Test
  void POST_findId_미매칭시_에러메시지_노출() throws Exception {
    given(findAccountService.findEmailByNameAndPhone(anyString(), anyString()))
        .willReturn(Optional.empty());

    mockMvc
        .perform(post("/find-id").param("name", "없음").param("phone", "01099999999"))
        .andExpect(status().isOk())
        .andExpect(view().name("user/find-id"))
        .andExpect(model().attributeExists("errorMsg"));
  }

  @Test
  void POST_findPassword_매칭시_reset_화면() throws Exception {
    given(findAccountService.verifyForPasswordReset(anyString(), anyString(), anyString()))
        .willReturn(Optional.of(user));

    mockMvc
        .perform(
            post("/find-password")
                .param("email", "seeder@youth-moa.test")
                .param("name", "시드유저1")
                .param("phone", "01000000001"))
        .andExpect(status().isOk())
        .andExpect(view().name("user/find-password-reset"));
  }

  @Test
  void GET_findPasswordReset_세션없으면_findPassword로_리다이렉트() throws Exception {
    mockMvc
        .perform(get("/find-password/reset"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/find-password"));
  }

  @Test
  void POST_findPasswordReset_세션없으면_리다이렉트() throws Exception {
    mockMvc
        .perform(
            post("/find-password/reset")
                .param("password", "Test1234!")
                .param("passwordConfirm", "Test1234!"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/find-password"));
  }

  /** WebMvcTest 에서 Security 필터 무효화 (다른 컨트롤러 테스트와 동일 패턴). */
  static class SecurityDisableConfig {
    @Bean
    SecurityFilterChain testChain(HttpSecurity http) throws Exception {
      http.authorizeHttpRequests(a -> a.anyRequest().permitAll()).csrf(c -> c.disable());
      return http.build();
    }
  }
}

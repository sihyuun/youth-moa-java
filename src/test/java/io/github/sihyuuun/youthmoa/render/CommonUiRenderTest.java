package io.github.sihyuuun.youthmoa.render;

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
 * U-COMMON-01 공통 Modal/Toast 인프라 렌더 검증.
 *
 * <ul>
 *   <li>footer fragment 를 include 한 홈 페이지에 {@code <div class="toast-stack"} 존재
 *   <li>{@code /js/common-ui.js} 200 서빙 확인
 *   <li>{@code common-ui.js} 스크립트 태그가 페이지에 포함됨 (defer)
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class CommonUiRenderTest {

  @Autowired MockMvc mockMvc;

  @Test
  void 홈에_toast_stack_컨테이너_렌더() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("class=\"toast-stack\"")))
        .andExpect(content().string(containsString("aria-live=\"polite\"")))
        .andExpect(content().string(containsString("/js/common-ui.js")));
  }

  @Test
  void common_ui_js_정적리소스_200() throws Exception {
    mockMvc
        .perform(get("/js/common-ui.js"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("window.Toast")))
        .andExpect(content().string(containsString("window.Modal")));
  }
}

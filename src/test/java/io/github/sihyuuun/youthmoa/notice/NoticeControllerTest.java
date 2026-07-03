package io.github.sihyuuun.youthmoa.notice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** F0g: 공지사항 목록 · 상세 컨트롤러 검증 (3 TC). */
@WebMvcTest(NoticeController.class)
@Import(NoticeControllerTest.SecurityDisableConfig.class)
class NoticeControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean NoticeService noticeService;

  private Notice sample;

  @BeforeEach
  void setup() throws Exception {
    sample =
        Notice.builder()
            .title("샘플 공지")
            .content("<p>본문</p>")
            .category(NoticeCategory.NOTICE)
            .isPinned(false)
            .build();
    Field f = Notice.class.getDeclaredField("id");
    f.setAccessible(true);
    f.set(sample, 1L);
  }

  @Test
  void list_returnsListView() throws Exception {
    given(noticeService.list(eq(null), eq(0)))
        .willReturn(new PageImpl<>(List.of(sample), PageRequest.of(0, 10), 1));

    mockMvc
        .perform(get("/notices"))
        .andExpect(status().isOk())
        .andExpect(view().name("notice/list"))
        .andExpect(model().attributeExists("notices", "categories"))
        .andExpect(model().attribute("filterCategory", (Object) null));
  }

  @Test
  void list_withCategoryFilter_passesEnum() throws Exception {
    given(noticeService.list(eq(NoticeCategory.EVENT), eq(0)))
        .willReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0));

    mockMvc
        .perform(get("/notices").param("category", "EVENT"))
        .andExpect(status().isOk())
        .andExpect(view().name("notice/list"))
        .andExpect(model().attribute("filterCategory", NoticeCategory.EVENT));
  }

  @Test
  void detail_returnsDetailViewAndAdjacent() throws Exception {
    given(noticeService.detailAndIncreaseView(1L)).willReturn(sample);
    given(noticeService.findPrev(1L)).willReturn(Optional.empty());
    given(noticeService.findNext(1L)).willReturn(Optional.empty());
    given(noticeService.findAttachments(1L)).willReturn(Collections.emptyList());

    mockMvc
        .perform(get("/notices/1"))
        .andExpect(status().isOk())
        .andExpect(view().name("notice/detail"))
        .andExpect(model().attributeExists("notice", "attachments"));
  }

  @Test
  void detail_missing_returns404() throws Exception {
    given(noticeService.detailAndIncreaseView(999L))
        .willThrow(new IllegalArgumentException("not found"));

    // NoticeController 가 IllegalArgumentException → ResponseStatusException(NOT_FOUND) 매핑
    mockMvc.perform(get("/notices/999")).andExpect(status().isNotFound());

    // any() 미참조 경고 회피
    given(noticeService.list(any(), any(Integer.class))).willReturn(null);
  }

  @org.springframework.boot.test.context.TestConfiguration
  static class SecurityDisableConfig {
    @org.springframework.context.annotation.Bean
    org.springframework.security.web.SecurityFilterChain filter(
        org.springframework.security.config.annotation.web.builders.HttpSecurity http)
        throws Exception {
      http.csrf(cs -> cs.disable()).authorizeHttpRequests(a -> a.anyRequest().permitAll());
      return http.build();
    }
  }
}

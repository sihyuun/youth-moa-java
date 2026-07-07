package io.github.sihyuuun.youthmoa.center;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** F0h: 청년센터 3-column 목록 + 인라인 상세 컨트롤러 검증. */
@WebMvcTest(CenterController.class)
@Import(CenterControllerTest.SecurityDisableConfig.class)
class CenterControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean CenterService centerService;

  @MockitoBean io.github.sihyuuun.youthmoa.notification.NotificationService notificationService;
  @MockitoBean io.github.sihyuuun.youthmoa.user.UserRepository userRepository;

  private CenterListItem sampleItem() {
    return new CenterListItem(
        1L,
        "청년바람지대",
        "수원시",
        "경기도 수원시 팔달구 매산로 89",
        "031-228-1234",
        new BigDecimal("37.2636000"),
        new BigDecimal("127.0286000"),
        true,
        3,
        "청년 창업과 네트워킹",
        "평일 09:00~18:00",
        "/images/centers/placeholder-1.png");
  }

  @Test
  void list_returnsListView_withDetailNull() throws Exception {
    given(centerService.list(any(), any(), anyBoolean(), any())).willReturn(List.of(sampleItem()));
    given(centerService.distinctActiveRegions()).willReturn(List.of("수원시", "성남시"));

    mockMvc
        .perform(get("/centers"))
        .andExpect(status().isOk())
        .andExpect(view().name("center/list"))
        .andExpect(model().attributeExists("centers", "regions", "kakaoMapAppKey"))
        .andExpect(model().attribute("detailCenter", (Object) null))
        .andExpect(model().attribute("filterOnlyActive", false));
  }

  @Test
  void list_withRegionFilter_passesParam() throws Exception {
    given(centerService.list(any(), any(), anyBoolean(), any())).willReturn(List.of());
    given(centerService.distinctActiveRegions()).willReturn(List.of());

    mockMvc
        .perform(get("/centers").param("region", "수원시").param("onlyActive", "true"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("filterRegion", "수원시"))
        .andExpect(model().attribute("filterOnlyActive", true));
  }

  @Test
  void detailRoute_setsDetailCenter_sameView() throws Exception {
    Center c =
        Center.builder()
            .name("청년바람지대")
            .region("수원시")
            .address("경기도 수원시 팔달구 매산로 89")
            .phone("031-228-1234")
            .latitude(new BigDecimal("37.2636000"))
            .longitude(new BigDecimal("127.0286000"))
            .description("청년 창업과 네트워킹")
            .operatingHours("평일 09:00~18:00")
            .imageUrl("/images/centers/placeholder-1.png")
            .build();
    given(centerService.findById(1L)).willReturn(Optional.of(c));
    given(centerService.list(any(), any(), anyBoolean(), any())).willReturn(List.of(sampleItem()));
    given(centerService.distinctActiveRegions()).willReturn(List.of("수원시"));

    mockMvc
        .perform(get("/centers/1"))
        .andExpect(status().isOk())
        .andExpect(view().name("center/list"))
        .andExpect(model().attributeExists("detailCenter", "centers", "kakaoMapAppKey"));
  }

  @Test
  void detailRoute_missing_returns404() throws Exception {
    given(centerService.findById(999L)).willReturn(Optional.empty());
    given(centerService.list(any(), any(), anyBoolean(), any())).willReturn(List.of());
    given(centerService.distinctActiveRegions()).willReturn(List.of());
    mockMvc.perform(get("/centers/999")).andExpect(status().isNotFound());
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

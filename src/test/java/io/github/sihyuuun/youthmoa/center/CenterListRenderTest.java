package io.github.sihyuuun.youthmoa.center;

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
 * F0h — 청년센터 목록·상세 실 렌더 검증.
 *
 * <p>@WebMvcTest 는 view name·model 만 검증하고 Thymeleaf 파싱을 안 수행하므로, sec:* 리터럴 노출·모델 shadowing 사고를 못
 * 잡는다. 여기선 e2e 프로파일로 실제 렌더 결과를 assert 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class CenterListRenderTest {

  @Autowired MockMvc mockMvc;

  @Test
  void 센터목록_렌더_및_data속성_노출() throws Exception {
    mockMvc
        .perform(get("/centers"))
        .andExpect(status().isOk())
        // 좌표 채워진 시드 센터 이름 중 하나
        .andExpect(content().string(containsString("청년바람지대")))
        // 리스트 카드의 data-lat 속성 (지도 마커 바인딩용)
        .andExpect(content().string(containsString("data-lat=")))
        .andExpect(content().string(containsString("data-lng=")))
        // Thymeleaf 표현식이 리터럴로 남지 않았는지
        .andExpect(content().string(not(containsString("${centers"))))
        .andExpect(content().string(not(containsString("th:each"))));
  }

  @Test
  void 지역필터_적용() throws Exception {
    mockMvc
        .perform(get("/centers").param("region", "수원시"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("수원시")));
  }

  @Test
  void kakao_appkey_미설정시_SDK_script_안렌더() throws Exception {
    // e2e 프로파일에서 KAKAO_MAP_APP_KEY 환경변수 없으면 빈 문자열 → script 태그 안 나옴
    mockMvc
        .perform(get("/centers"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("dapi.kakao.com/v2/maps/sdk.js?appkey=&"))));
  }

  // ═══════════════════════════════════════════════════════════════════════
  // F0h-c2 개정(2026-07-09) — client-state 재설계 회귀 방어
  // 사고 재발 방지: th:fragment 를 부모 body 안에 두어 인라인 실행되던 NPE +
  //                th:if + th:replace 같은 element 조합의 short-circuit 미동작
  // ═══════════════════════════════════════════════════════════════════════

  @Test
  void F0h_c2_centers_초기렌더는_상세컬럼_hidden() throws Exception {
    // /centers (detailCenter null) → aside hidden 속성 + fragment body 미실행
    mockMvc
        .perform(get("/centers"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("centers-detail-col")))
        // hidden 속성 존재 (detailCenter == null 이므로)
        .andExpect(content().string(containsString("hidden")))
        // detail-panel-content fragment body 는 렌더 안 되어야 함 (없어야 함)
        .andExpect(content().string(not(containsString("centers-detail-close"))))
        // 리스트 컬럼 full 모드 (has-detail 클래스 없음)
        .andExpect(content().string(containsString("centers-list-col")));
  }

  @Test
  void F0h_c2_centers_detailId_직접접근_상세컬럼_렌더() throws Exception {
    mockMvc
        .perform(get("/centers/1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("centers-detail-close")))
        .andExpect(content().string(containsString("has-detail")))
        // Thymeleaf 표현식 잔존 방지
        .andExpect(content().string(not(containsString("${detailCenter"))))
        .andExpect(content().string(not(containsString("th:replace"))));
  }

  @Test
  void F0h_c2_detail_fragment_endpoint() throws Exception {
    // /centers/{id}/detail-fragment 는 detail-panel-content fragment 마크업만 반환
    mockMvc
        .perform(get("/centers/1/detail-fragment"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("centers-detail-close")))
        .andExpect(content().string(containsString("centers-detail-cta")))
        // 전체 레이아웃은 반환되지 않아야 함 (fragment only)
        .andExpect(content().string(not(containsString("centers-layout"))));
  }

  @Test
  void F0h_c2_cards_fragment_endpoint_compact_모드() throws Exception {
    mockMvc
        .perform(get("/centers/cards").param("compact", "true").param("activeId", "1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("center-card-compact")))
        // activeId=1 카드에 is-active 클래스
        .andExpect(content().string(containsString("is-active")))
        // 전체 레이아웃 없이 카드만
        .andExpect(content().string(not(containsString("centers-layout"))));
  }

  // ═══════════════════════════════════════════════════════════════════════
  // F0h-c4 (2026-07-09) — 상세 패널 아이콘 이모지 → SVG 회귀 방어
  // prototype.tsx L54~77, L2056~2086 대조. 이모지·× 텍스트 부재 + SVG 존재 assert.
  // ═══════════════════════════════════════════════════════════════════════

  @Test
  void F0h_c4_상세페이지_이모지_아이콘_부재_그리고_SVG_존재() throws Exception {
    mockMvc
        .perform(get("/centers/1"))
        .andExpect(status().isOk())
        // 이모지 placeholder / 메타 아이콘 / 닫기 X 텍스트 모두 제거됐는지
        .andExpect(content().string(not(containsString("🏢"))))
        .andExpect(content().string(not(containsString("📍"))))
        .andExpect(content().string(not(containsString("🕒"))))
        .andExpect(content().string(not(containsString("📞"))))
        // SVG 아이콘 정상 이식 확인
        .andExpect(content().string(containsString("<svg")))
        .andExpect(content().string(containsString("centers-detail-meta-badge")));
  }

  @Test
  void F0h_c4_detail_fragment_이모지_부재_그리고_SVG_존재() throws Exception {
    mockMvc
        .perform(get("/centers/1/detail-fragment"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("🏢"))))
        .andExpect(content().string(not(containsString("📍"))))
        .andExpect(content().string(not(containsString("🕒"))))
        .andExpect(content().string(not(containsString("📞"))))
        .andExpect(content().string(containsString("<svg")))
        .andExpect(content().string(containsString("centers-detail-meta-badge")));
  }

  @Test
  void F0h_c2_cards_fragment_endpoint_full_모드() throws Exception {
    mockMvc
        .perform(get("/centers/cards").param("compact", "false"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("center-card-full")))
        // compact 마크업은 없어야 함
        .andExpect(content().string(not(containsString("center-card-compact-icon"))));
  }

  // ═══════════════════════════════════════════════════════════════════════
  // F0h-c4 개정(2026-07-09) — FAIL-1/2/3 회귀 방어
  //   FAIL-1: 카드 클릭 → 마커 selected (CustomEvent dispatch/listen)
  //   FAIL-2: 인포윈도우 CTA 를 <a href> 대신 <button data-info-detail>
  //   FAIL-3: htmx afterSwap 스코프 축소 + map 캐싱
  // ═══════════════════════════════════════════════════════════════════════

  @Test
  void F0h_map_인포윈도우_CTA_는_button_태그() throws Exception {
    mockMvc
        .perform(get("/js/center-map.js"))
        .andExpect(status().isOk())
        // button + data-info-detail 마크업 존재
        .andExpect(content().string(containsString("data-info-detail")))
        // <a href="/centers/..."> 형태의 인포윈도우 CTA 는 제거되어야 함
        .andExpect(content().string(not(containsString("href=\"/centers/"))));
  }

  @Test
  void F0h_centers_detail_js_CustomEvent_dispatch() throws Exception {
    mockMvc
        .perform(get("/js/centers-detail.js"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("centers:detail-open")))
        .andExpect(content().string(containsString("centers:detail-close")))
        .andExpect(content().string(containsString("centers:request-detail")));
  }

  @Test
  void F0h_center_map_js_CustomEvent_listener() throws Exception {
    mockMvc
        .perform(get("/js/center-map.js"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("centers:detail-open")))
        .andExpect(content().string(containsString("centers:detail-close")))
        .andExpect(content().string(containsString("centers:request-detail")));
  }

  @Test
  void F0h_center_map_js_panTo_구현() throws Exception {
    // spec §3-4-A (2026-07-09 개정): selectMarker() 내에서 map.panTo(new kakao.maps.LatLng(...)) 호출.
    // 마커가 뷰포트 밖일 때 selected 상태 시각화 보장.
    mockMvc
        .perform(get("/js/center-map.js"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("panTo")))
        .andExpect(content().string(containsString("new kakao.maps.LatLng")));
  }
}

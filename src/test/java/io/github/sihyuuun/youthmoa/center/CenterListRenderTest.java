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
}

package io.github.sihyuuun.youthmoa.program;

import static org.assertj.core.api.Assertions.assertThat;
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
 * F0f — 프로그램 목록 및 캘린더 뷰 렌더 검증.
 *
 * <p>view 파라미터 분기 (grid ↔ calendar) 가 실제로 다른 fragment 를 include 하는지, Thymeleaf 표현식이 리터럴로 남지 않는지 확인.
 * 캘린더 뷰의 시각적 세부는 e2e Playwright 가 담당.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class ProgramListRenderTest {

  @Autowired MockMvc mockMvc;

  @Test
  void 기본_그리드_뷰_렌더() throws Exception {
    mockMvc
        .perform(get("/programs"))
        .andExpect(status().isOk())
        // 기존 목록 fragment 마크업 (필터 바)
        .andExpect(content().string(containsString("view-toggle")))
        // 캘린더 fragment 는 렌더 안 됨
        .andExpect(content().string(not(containsString("program-calendar-grid"))))
        // Thymeleaf 잔존 없음
        .andExpect(content().string(not(containsString("${calendarView"))));
  }

  @Test
  void 캘린더_뷰_렌더_및_기본_마크업() throws Exception {
    mockMvc
        .perform(get("/programs").param("view", "calendar"))
        .andExpect(status().isOk())
        // 캘린더 fragment 진입 확인
        .andExpect(content().string(containsString("program-calendar-grid")))
        .andExpect(content().string(containsString("program-calendar-toolbar")))
        // 3색 범례
        .andExpect(content().string(containsString("진행예정")))
        .andExpect(content().string(containsString("모집중")))
        .andExpect(content().string(containsString("종료")))
        // 요일 헤더
        .andExpect(content().string(containsString(">일<")))
        .andExpect(content().string(containsString(">토<")))
        // 목록 fragment 마크업 (_list-fragment) 은 안 나옴 — 캘린더 뷰이므로
        .andExpect(content().string(not(containsString("program-list-empty"))))
        // Thymeleaf 잔존 없음
        .andExpect(content().string(not(containsString("${cv."))))
        .andExpect(content().string(not(containsString("th:each"))));
  }

  @Test
  void 캘린더_년월_파라미터_반영() throws Exception {
    mockMvc
        .perform(
            get("/programs").param("view", "calendar").param("year", "2026").param("month", "9"))
        .andExpect(status().isOk())
        // 툴바에 지정한 년월 표시
        .andExpect(content().string(containsString("2026년 9월")));
  }

  @Test
  void 캘린더_view_toggle_링크_활성() throws Exception {
    // disabled placeholder 제거 후 view=calendar 링크 존재 (regression: program-list.spec.ts 대응)
    mockMvc
        .perform(get("/programs"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("view=calendar")))
        .andExpect(content().string(not(containsString("view-toggle-btn--disabled"))));
  }

  @Test
  void 빈_달_배너_문구_탭_이름_포함() throws Exception {
    // dc.html §7a: "{현재월}에는 {탭 이름} 프로그램이 없어요"
    // 시드에 없을 먼 미래 (2100/1) + status=ended → "1월에는 종료된 프로그램이 없어요"
    mockMvc
        .perform(
            get("/programs")
                .param("view", "calendar")
                .param("status", "ended")
                .param("year", "2100")
                .param("month", "1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("program-calendar-empty-banner")))
        .andExpect(content().string(containsString("종료된 프로그램이 없어요")));
  }

  @Test
  void HTMX_요청_view_calendar_시_캘린더_fragment_반환() throws Exception {
    // ym-verify N2-2 회귀 방지: HX-Request 헤더 + view=calendar 시 _list-fragment 아닌 캘린더 반환
    mockMvc
        .perform(get("/programs").param("view", "calendar").header("HX-Request", "true"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("program-calendar-grid")))
        .andExpect(content().string(not(containsString("program-list-empty"))));
  }

  @Test
  void HTMX_요청_기본_뷰는_list_fragment_반환() throws Exception {
    // 회귀 방지: HX-Request + view 미지정 (또는 list) → 기존 list fragment 반환
    mockMvc
        .perform(get("/programs").header("HX-Request", "true"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("program-calendar-grid"))));
  }

  @Test
  void 캘린더_토글_링크에_sort_파라미터_포함() throws Exception {
    // ym-verify N2-3 회귀 방지: 목록 뷰에서 인기순 선택 후 캘린더 링크 href 에 sort=popular 유지
    mockMvc
        .perform(get("/programs").param("sort", "popular"))
        .andExpect(status().isOk())
        // 캘린더 링크 href = /programs?view=calendar&status=&regions=&centers=&sort=popular 형태
        .andExpect(content().string(containsString("view=calendar")))
        .andExpect(content().string(containsString("sort=popular")));
  }

  @Test
  void 캘린더뷰에서_status_탭_링크가_view_calendar_유지() throws Exception {
    // 회귀 방지: 캘린더 뷰에서 status-tab 클릭 시 목록으로 튕기지 않도록
    mockMvc
        .perform(get("/programs").param("view", "calendar"))
        .andExpect(status().isOk())
        // status-tabs a[href] 안에 view=calendar 파라미터 포함
        .andExpect(content().string(containsString("view=calendar&amp;status=active")));
  }

  @Test
  void UPCOMING_프로그램_chip_은_M_D_오픈_텍스트() throws Exception {
    // 스펙 §3-A #3 · dc.html §5a: UPCOMING → "M/D 오픈" secondary color, D-N 만들지 않음
    java.time.YearMonth ym = java.time.YearMonth.now().plusMonths(1);
    mockMvc
        .perform(
            get("/programs")
                .param("view", "calendar")
                .param("year", String.valueOf(ym.getYear()))
                .param("month", String.valueOf(ym.getMonthValue())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("program-calendar-panel-card-chip--upcoming")))
        .andExpect(content().string(containsString("오픈")));
  }

  @Test
  void OPEN_프로그램_chip_은_D_N_dark_배경() throws Exception {
    // dc.html §5a: OPEN → D-N, background dark (rgba(0,0,0,0.55)) — chip--open modifier
    mockMvc
        .perform(get("/programs").param("view", "calendar"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("program-calendar-panel-card-chip--open")));
  }

  @Test
  void ENDED_프로그램_chip_은_종료_grey() throws Exception {
    // dc.html §5a: ENDED → "종료" grey (rgba(120,124,130,0.85)) — chip--ended modifier
    java.time.YearMonth ym = java.time.YearMonth.now().minusMonths(1);
    mockMvc
        .perform(
            get("/programs")
                .param("view", "calendar")
                .param("status", "ended")
                .param("year", String.valueOf(ym.getYear()))
                .param("month", String.valueOf(ym.getMonthValue())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("program-calendar-panel-card-chip--ended")));
  }

  @Test
  void 캘린더_패널_카드는_capacityBar_fragment_재사용_showLabel_false() throws Exception {
    // D5-Q3 (2026-10-14): user 캘린더 패널 카드가 공용 capacityBar fragment 를 showLabel=false 로 호출.
    // prototype.tsx L795~807 매칭. 이전 자체 네임스페이스 (program-calendar-panel-card-bar{,-fill})
    // 참조는 user template 에서 제거됨 (CSS 정리는 FOLLOW 티켓).
    String html =
        mockMvc
            .perform(get("/programs").param("view", "calendar"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // 패널 카드 영역 추출 — program-calendar-panel-card-capacity 블록 안쪽
    int capStart = html.indexOf("program-calendar-panel-card-capacity");
    assertThat(capStart).isGreaterThanOrEqualTo(0);

    // 패널 카드 안에 공용 bar 네임스페이스(capacity-bar + capacity-bar-fill) 가 포함됨
    assertThat(html).contains("capacity-bar-fill--");
    // user template 은 더 이상 자체 네임스페이스를 참조하지 않음
    assertThat(html).doesNotContain("program-calendar-panel-card-bar-fill");
    assertThat(html)
        .as("user _calendar-fragment.html 은 program-calendar-panel-card-bar 를 더 이상 직접 참조하지 않음")
        .doesNotContain("<div class=\"program-calendar-panel-card-bar\">");
  }

  @Test
  void 캘린더_뷰_JS_로드() throws Exception {
    // program-calendar.js 는 view=calendar 일 때만 로드
    mockMvc
        .perform(get("/programs").param("view", "calendar"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("program-calendar.js")));

    mockMvc
        .perform(get("/programs"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("program-calendar.js"))));
  }
}

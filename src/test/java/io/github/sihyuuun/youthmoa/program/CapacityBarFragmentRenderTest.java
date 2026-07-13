package io.github.sihyuuun.youthmoa.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * D5 — CapacityBar fragment 실 렌더 검증.
 *
 * <p>DTO 계산 단위 테스트는 ProgramCardDtoTest 가 담당. 이 클래스는 fragment 자체가 파라미터 조합에 대해 의도한 마크업(status
 * 라벨 별도 라인, upcoming 시 openDate 라벨, showBar=false 시 bar 미표시 등)을 렌더하는지, 그리고 상세 페이지가 이 fragment 를
 * 통해 CapacityBar 를 정상 렌더하는지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class CapacityBarFragmentRenderTest {

  @Autowired ApplicationContext ctx;
  @Autowired MockMvc mockMvc;

  private String renderFragment(ProgramCardDto dto) {
    TemplateEngine engine = ctx.getBean(TemplateEngine.class);
    Context c = new Context();
    c.setVariable("pct", dto.getPct());
    c.setVariable("colorClass", dto.getColorClass());
    c.setVariable("barLabel", dto.getBarLabel());
    c.setVariable("capacityText", dto.getCapacityText());
    c.setVariable("showBar", dto.getCapacity() != null);
    c.setVariable("openDateLabel", dto.getOpenDateLabel());
    // Thymeleaf 프로그램적 fragment 렌더 — selector 로 fragment element 지정.
    // fragment 파라미터 이름이 context 변수와 일치하면 자동 바인딩됨.
    return engine.process("fragments/capacity-bar", java.util.Set.of("capacityBar"), c);
  }

  private Program activeProgram(Integer capacity) {
    return Program.builder()
        .title("t")
        .organization("o")
        .content("c")
        .startDate(LocalDate.now().minusDays(1))
        .endDate(LocalDate.now().plusDays(10))
        .capacity(capacity)
        .build();
  }

  private Program upcomingProgram(LocalDate startDate) {
    return Program.builder()
        .title("t")
        .organization("o")
        .content("c")
        .startDate(startDate)
        .endDate(startDate.plusDays(20))
        .capacity(20)
        .build();
  }

  private Program closedProgram() {
    return Program.builder()
        .title("t")
        .organization("o")
        .content("c")
        .startDate(LocalDate.now().minusDays(20))
        .endDate(LocalDate.now().minusDays(1))
        .capacity(10)
        .build();
  }

  // ─── 경계값 5 : 0% / 69% / 70% / 89% / 90% / 100% ───

  @Test
  @DisplayName("pct=0 (신청자 없음) → primary bar 렌더, 마감임박/warning 클래스 없음")
  void render_pct_0() {
    String html = renderFragment(new ProgramCardDto(activeProgram(10), 0));
    assertThat(html).contains("capacity-bar-fill--primary");
    assertThat(html).contains("width:0%");
    assertThat(html).contains("모집중");
    assertThat(html).contains("정원 0/10명");
    assertThat(html).doesNotContain("capacity-bar-fill--warning");
    assertThat(html).doesNotContain("capacity-bar-fill--error");
  }

  @Test
  @DisplayName("pct=69 (69%) → primary/모집중")
  void render_pct_69() {
    // 100명 정원, 69명 신청 → 69%
    String html = renderFragment(new ProgramCardDto(activeProgram(100), 69));
    assertThat(html).contains("capacity-bar-fill--primary");
    assertThat(html).contains("width:69%");
    assertThat(html).contains("모집중");
    assertThat(html).doesNotContain("capacity-bar-fill--warning");
  }

  @Test
  @DisplayName("pct=70 (경계) → warning/서두르세요")
  void render_pct_70() {
    String html = renderFragment(new ProgramCardDto(activeProgram(10), 7));
    assertThat(html).contains("capacity-bar-fill--warning");
    assertThat(html).contains("capacity-bar-status--warning");
    assertThat(html).contains("서두르세요");
    assertThat(html).contains("width:70%");
  }

  @Test
  @DisplayName("pct=89 (90 미만 경계) → warning 유지")
  void render_pct_89() {
    String html = renderFragment(new ProgramCardDto(activeProgram(100), 89));
    assertThat(html).contains("capacity-bar-fill--warning");
    assertThat(html).contains("서두르세요");
    assertThat(html).doesNotContain("capacity-bar-fill--error");
  }

  @Test
  @DisplayName("pct=90 (경계) → error/마감임박")
  void render_pct_90() {
    String html = renderFragment(new ProgramCardDto(activeProgram(10), 9));
    assertThat(html).contains("capacity-bar-fill--error");
    assertThat(html).contains("capacity-bar-status--error");
    assertThat(html).contains("마감임박");
    assertThat(html).contains("width:90%");
  }

  @Test
  @DisplayName("pct=100 (정원 초과 아님) → error/마감임박")
  void render_pct_100() {
    String html = renderFragment(new ProgramCardDto(activeProgram(10), 10));
    assertThat(html).contains("capacity-bar-fill--error");
    assertThat(html).contains("마감임박");
    assertThat(html).contains("width:100%");
  }

  // ─── UPCOMING + openDateLabel 표시 ───

  @Test
  @DisplayName("UPCOMING 프로그램은 우측에 MM/dd 오픈 라벨을 렌더")
  void render_upcoming_showsOpenDate() {
    LocalDate future = LocalDate.now().plusDays(7);
    String expected =
        future.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd")) + " 오픈";
    String html = renderFragment(new ProgramCardDto(upcomingProgram(future), 0));
    assertThat(html).contains("capacity-bar-opendate");
    assertThat(html).contains(expected);
    assertThat(html).contains("신청 오픈 예정");
    assertThat(html).contains("capacity-bar-status--secondary");
  }

  @Test
  @DisplayName("ACTIVE 프로그램은 openDate 라벨을 렌더하지 않음")
  void render_active_hidesOpenDate() {
    String html = renderFragment(new ProgramCardDto(activeProgram(10), 3));
    assertThat(html).doesNotContain("capacity-bar-opendate");
    assertThat(html).doesNotContain(" 오픈</span>");
  }

  @Test
  @DisplayName("CLOSED 프로그램 → muted/모집 마감, openDate 없음")
  void render_closed() {
    String html = renderFragment(new ProgramCardDto(closedProgram(), 5));
    assertThat(html).contains("capacity-bar-fill--muted");
    assertThat(html).contains("모집 마감");
    assertThat(html).doesNotContain("capacity-bar-opendate");
  }

  // ─── 상세 페이지 fragment 통합 렌더 ───

  @Test
  @DisplayName("상세 페이지 /programs/{id} 는 CapacityBar fragment 를 렌더 (통합 확인)")
  void detail_page_rendersFragment() throws Exception {
    // e2e 프로파일 DataInitializer 는 프로그램 시드를 넣음. id=1 은 존재 가정.
    MvcResult result = mockMvc.perform(get("/programs/1")).andReturn();
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    String html = result.getResponse().getContentAsString();

    // fragment 마크업 존재
    assertThat(html).contains("capacity-bar-wrap");
    assertThat(html).contains("capacity-bar-label--status");
    // 색상 임계 클래스 중 하나가 반드시 렌더 (색상은 시드 상태에 따라 다를 수 있으므로 alt-match)
    assertThat(html)
        .containsAnyOf(
            "capacity-bar-fill--primary",
            "capacity-bar-fill--warning",
            "capacity-bar-fill--error",
            "capacity-bar-fill--muted",
            "capacity-bar-status--secondary");
    // 상세 전용 dead 마크업 제거 확인
    assertThat(html).doesNotContain("detail-capacity-bar-fill");
    // Thymeleaf 표현식 잔존 없음
    assertThat(html).doesNotContain("${capacityPct");
    assertThat(html).doesNotContain("th:replace=");
  }
}

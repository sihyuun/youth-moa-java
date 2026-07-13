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
 * D5 — CapacityBar fragment 실 렌더 검증 (prototype.tsx L204~228 2-line 매칭).
 *
 * <p>DTO 계산 로직 단위 테스트는 ProgramCardDtoTest 담당. 여기서는 fragment 자체가 파라미터 조합에 대해 의도한 마크업 (2-line
 * 레이아웃, primary/secondary label, colorClass 클래스 조합) 을 렌더하는지, 상세 페이지 통합 렌더가 되는지 확인.
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
    c.setVariable("primaryLabel", dto.getPrimaryLabel());
    c.setVariable("secondaryLabel", dto.getSecondaryLabel());
    c.setVariable("showBar", dto.getCapacity() != null);
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

  // ─── 경계값 : 0% / 69% / 70% / 89% / 90% / 100% ───

  @Test
  @DisplayName("pct=0 → primary bar, primaryLabel=정원 0/10명, primary 색상")
  void render_pct_0() {
    String html = renderFragment(new ProgramCardDto(activeProgram(10), 0));
    assertThat(html).contains("capacity-bar-fill--primary");
    assertThat(html).contains("capacity-bar-primary--primary");
    assertThat(html).contains("width:0%");
    assertThat(html).contains("정원 0/10명");
    assertThat(html).contains("0%");
    assertThat(html).doesNotContain("capacity-bar-fill--warning");
    assertThat(html).doesNotContain("capacity-bar-fill--error");
  }

  @Test
  @DisplayName("pct=69 → primary/모집중 계열")
  void render_pct_69() {
    String html = renderFragment(new ProgramCardDto(activeProgram(100), 69));
    assertThat(html).contains("capacity-bar-fill--primary");
    assertThat(html).contains("capacity-bar-primary--primary");
    assertThat(html).contains("width:69%");
    assertThat(html).contains("정원 69/100명");
    assertThat(html).doesNotContain("capacity-bar-fill--warning");
  }

  @Test
  @DisplayName("pct=70 → warning 색상, 라벨은 여전히 정원 N/M명 (prototype 정확 매칭)")
  void render_pct_70() {
    String html = renderFragment(new ProgramCardDto(activeProgram(10), 7));
    assertThat(html).contains("capacity-bar-fill--warning");
    assertThat(html).contains("capacity-bar-primary--warning");
    assertThat(html).contains("정원 7/10명");
    assertThat(html).contains("width:70%");
    // prototype 은 "서두르세요" 텍스트 라벨을 쓰지 않음 (색상만으로 긴급도 전달)
    assertThat(html).doesNotContain("서두르세요");
  }

  @Test
  @DisplayName("pct=89 → warning 유지")
  void render_pct_89() {
    String html = renderFragment(new ProgramCardDto(activeProgram(100), 89));
    assertThat(html).contains("capacity-bar-fill--warning");
    assertThat(html).contains("정원 89/100명");
    assertThat(html).doesNotContain("capacity-bar-fill--error");
  }

  @Test
  @DisplayName("pct=90 → error 색상, 라벨은 정원 N/M명")
  void render_pct_90() {
    String html = renderFragment(new ProgramCardDto(activeProgram(10), 9));
    assertThat(html).contains("capacity-bar-fill--error");
    assertThat(html).contains("capacity-bar-primary--error");
    assertThat(html).contains("정원 9/10명");
    assertThat(html).contains("width:90%");
    assertThat(html).doesNotContain("마감임박");
  }

  @Test
  @DisplayName("pct=100 (applied == capacity) → muted/모집 마감 (full 취급)")
  void render_pct_100() {
    String html = renderFragment(new ProgramCardDto(activeProgram(10), 10));
    assertThat(html).contains("capacity-bar-fill--muted");
    assertThat(html).contains("capacity-bar-primary--muted");
    assertThat(html).contains("모집 마감");
    assertThat(html).contains("100%");
    assertThat(html).contains("width:100%");
  }

  // ─── UPCOMING + 오픈일 ───

  @Test
  @DisplayName("UPCOMING → 신청 오픈 예정 + MM/dd 오픈 (secondary color)")
  void render_upcoming_showsOpenDate() {
    LocalDate future = LocalDate.now().plusDays(7);
    String expected =
        future.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd")) + " 오픈";
    String html = renderFragment(new ProgramCardDto(upcomingProgram(future), 0));
    assertThat(html).contains("신청 오픈 예정");
    assertThat(html).contains(expected);
    assertThat(html).contains("capacity-bar-primary--secondary");
    assertThat(html).contains("capacity-bar-fill--secondary");
  }

  @Test
  @DisplayName("ACTIVE 상태는 오픈일 라벨 없음")
  void render_active_hidesOpenDate() {
    String html = renderFragment(new ProgramCardDto(activeProgram(10), 3));
    assertThat(html).doesNotContain("오픈</span>");
    // secondaryLabel 은 pct 로 렌더 (30%)
    assertThat(html).contains("30%");
  }

  @Test
  @DisplayName("CLOSED → muted/모집 마감, secondaryLabel=100%")
  void render_closed() {
    String html = renderFragment(new ProgramCardDto(closedProgram(), 5));
    assertThat(html).contains("capacity-bar-fill--muted");
    assertThat(html).contains("capacity-bar-primary--muted");
    assertThat(html).contains("모집 마감");
    assertThat(html).contains("100%");
    assertThat(html).doesNotContain("오픈</span>");
  }

  // ─── 상세 페이지 통합 ───

  @Test
  @DisplayName("상세 페이지 /programs/{id} 는 CapacityBar fragment 를 렌더")
  void detail_page_rendersFragment() throws Exception {
    MvcResult result = mockMvc.perform(get("/programs/1")).andReturn();
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    String html = result.getResponse().getContentAsString();
    assertThat(html).contains("capacity-bar-wrap");
    assertThat(html).contains("capacity-bar-primary");
    // 임계 클래스 중 하나 필수
    assertThat(html)
        .containsAnyOf(
            "capacity-bar-fill--primary",
            "capacity-bar-fill--warning",
            "capacity-bar-fill--error",
            "capacity-bar-fill--muted",
            "capacity-bar-primary--secondary");
    // 이전 3-line 잔재·상세 전용 dead 마크업 없음
    assertThat(html).doesNotContain("detail-capacity-bar-fill");
    assertThat(html).doesNotContain("capacity-bar-status");
    assertThat(html).doesNotContain("capacity-bar-label--status");
    // Thymeleaf 표현식 잔존 없음
    assertThat(html).doesNotContain("${capacityPct");
    assertThat(html).doesNotContain("th:replace=");
  }
}

package io.github.sihyuuun.youthmoa.program;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@DataJpaTest
@AutoConfigureTestDatabase
@Import(JpaConfig.class)
class ProgramSearchTest {

  @Autowired ProgramRepository programRepository;

  @BeforeEach
  void seed() {
    LocalDate today = LocalDate.now();

    // 진행중 (수원시, 취업)
    programRepository.save(
        Program.builder()
            .title("취업 워크숍")
            .organization("내일스퀘어")
            .category("취업")
            .region("수원시")
            .content("c")
            .startDate(today.minusDays(5))
            .endDate(today.plusDays(5))
            .capacity(30)
            .build());

    // 진행예정 (고양시, 교육)
    programRepository.save(
        Program.builder()
            .title("AI 교육")
            .organization("비행지구")
            .category("교육")
            .region("고양시")
            .content("c")
            .startDate(today.plusDays(10))
            .endDate(today.plusDays(30))
            .capacity(20)
            .build());

    // 마감 (부천시, 교육)
    programRepository.save(
        Program.builder()
            .title("마케팅 종료")
            .organization("원미")
            .category("교육")
            .region("부천시")
            .content("c")
            .startDate(today.minusDays(30))
            .endDate(today.minusDays(5))
            .capacity(15)
            .build());
  }

  @Test
  @DisplayName("status=active 필터는 진행중 프로그램만 반환")
  void filterActive() {
    Specification<Program> spec =
        Specification.where(ProgramSpec.isActive()).and(ProgramSpec.withDateStatus("active"));
    Page<Program> result =
        programRepository.findAll(
            spec, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
    assertThat(result.getContent()).extracting(Program::getTitle).containsExactly("취업 워크숍");
  }

  @Test
  @DisplayName("status=upcoming 필터는 진행예정 프로그램만 반환")
  void filterUpcoming() {
    Specification<Program> spec =
        Specification.where(ProgramSpec.isActive()).and(ProgramSpec.withDateStatus("upcoming"));
    Page<Program> result =
        programRepository.findAll(
            spec, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
    assertThat(result.getContent()).extracting(Program::getTitle).containsExactly("AI 교육");
  }

  @Test
  @DisplayName("status=closed 필터는 마감 프로그램만 반환")
  void filterClosed() {
    Specification<Program> spec =
        Specification.where(ProgramSpec.isActive()).and(ProgramSpec.withDateStatus("closed"));
    Page<Program> result =
        programRepository.findAll(
            spec, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
    assertThat(result.getContent()).extracting(Program::getTitle).containsExactly("마케팅 종료");
  }

  @Test
  @DisplayName("region 필터로 단일 지역만 추출")
  void filterRegion() {
    Specification<Program> spec =
        Specification.where(ProgramSpec.isActive()).and(ProgramSpec.withRegion("고양시"));
    Page<Program> result =
        programRepository.findAll(
            spec, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
    assertThat(result.getContent()).extracting(Program::getRegion).containsExactly("고양시");
  }

  @Test
  @DisplayName("withRegions(List) 는 IN 절로 다중 지역을 추출한다")
  void filterRegionsMultiple() {
    Specification<Program> spec =
        Specification.where(ProgramSpec.isActive())
            .and(ProgramSpec.withRegions(java.util.List.of("수원시", "고양시")));
    Page<Program> result =
        programRepository.findAll(
            spec, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
    assertThat(result.getContent())
        .extracting(Program::getRegion)
        .containsExactlyInAnyOrder("수원시", "고양시");
  }

  @Test
  @DisplayName("withRegions 가 null/빈 리스트 면 조건 미적용")
  void filterRegionsEmpty() {
    assertThat(ProgramSpec.withRegions(null)).isNull();
    assertThat(ProgramSpec.withRegions(java.util.List.of())).isNull();
  }

  @Test
  @DisplayName("withCenters(List) 는 organization IN 절로 청년센터 다중 필터")
  void filterCentersMultiple() {
    Specification<Program> spec =
        Specification.where(ProgramSpec.isActive())
            .and(ProgramSpec.withCenters(java.util.List.of("내일스퀘어", "비행지구")));
    Page<Program> result =
        programRepository.findAll(
            spec, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
    assertThat(result.getContent())
        .extracting(Program::getOrganization)
        .containsExactlyInAnyOrder("내일스퀘어", "비행지구");
  }

  @Test
  @DisplayName("findDistinctRegions는 중복 제거된 지역 목록 반환")
  void distinctRegions() {
    assertThat(programRepository.findDistinctRegions())
        .containsExactlyInAnyOrder("수원시", "고양시", "부천시");
  }

  @Test
  @DisplayName("Program.getStatus()는 날짜에 따라 정확한 상태 반환")
  void statusComputation() {
    LocalDate today = LocalDate.now();
    Program p1 =
        programRepository.save(
            Program.builder()
                .title("t")
                .organization("o")
                .category("c")
                .content("c")
                .startDate(today)
                .endDate(today.plusDays(1))
                .build());
    assertThat(p1.getStatus()).isEqualTo(ProgramStatus.ACTIVE);

    Program p2 =
        programRepository.save(
            Program.builder()
                .title("t")
                .organization("o")
                .category("c")
                .content("c")
                .startDate(today.plusDays(5))
                .endDate(today.plusDays(10))
                .build());
    assertThat(p2.getStatus()).isEqualTo(ProgramStatus.UPCOMING);

    Program p3 =
        programRepository.save(
            Program.builder()
                .title("t")
                .organization("o")
                .category("c")
                .content("c")
                .startDate(today.minusDays(10))
                .endDate(today.minusDays(1))
                .build());
    assertThat(p3.getStatus()).isEqualTo(ProgramStatus.CLOSED);
  }
}

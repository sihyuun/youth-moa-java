package io.github.sihyuuun.youthmoa.program;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import io.github.sihyuuun.youthmoa.region.Region;
import io.github.sihyuuun.youthmoa.region.RegionRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;

/** F0f — ProgramService.search 의 다중 region/center 필터 + sort 분기 검증. */
@DataJpaTest
@AutoConfigureTestDatabase
@Import({JpaConfig.class, ProgramService.class})
class ProgramServiceFilterTest {

  @Autowired ProgramService programService;
  @Autowired ProgramRepository programRepository;
  @Autowired RegionRepository regionRepository;
  @Autowired CenterRepository centerRepository;

  @BeforeEach
  void seed() {
    LocalDate today = LocalDate.now();

    regionRepository.save(Region.builder().name("수원시").isFeatured(true).build());
    regionRepository.save(Region.builder().name("고양시").isFeatured(true).build());
    regionRepository.save(Region.builder().name("부천시").isFeatured(false).build());

    centerRepository.save(Center.builder().name("내일스퀘어").region("수원시").isFeatured(true).build());
    centerRepository.save(Center.builder().name("비행지구").region("고양시").isFeatured(false).build());

    programRepository.save(
        Program.builder()
            .title("취업 워크숍")
            .organization("내일스퀘어")
            .region("수원시")
            .content("c")
            .startDate(today.minusDays(5))
            .endDate(today.plusDays(5))
            .capacity(30)
            .build());

    programRepository.save(
        Program.builder()
            .title("AI 교육")
            .organization("비행지구")
            .region("고양시")
            .content("c")
            .startDate(today.plusDays(10))
            .endDate(today.plusDays(30))
            .capacity(20)
            .build());

    programRepository.save(
        Program.builder()
            .title("마케팅 종료")
            .organization("원미")
            .region("부천시")
            .content("c")
            .startDate(today.minusDays(30))
            .endDate(today.minusDays(5))
            .capacity(15)
            .build());
  }

  @Test
  @DisplayName("regions 다중 선택 시 IN 절로 두 지역의 프로그램만 반환")
  void searchByMultipleRegions() {
    Page<Program> result =
        programService.search(
            "",
            List.of("수원시", "고양시"),
            Collections.emptyList(),
            "newest",
            0,
            Collections.emptySet());
    assertThat(result.getContent())
        .extracting(Program::getRegion)
        .containsExactlyInAnyOrder("수원시", "고양시");
  }

  @Test
  @DisplayName("centers 다중 선택 시 organization IN 절로 매칭")
  void searchByMultipleCenters() {
    Page<Program> result =
        programService.search(
            "", Collections.emptyList(), List.of("내일스퀘어"), "newest", 0, Collections.emptySet());
    assertThat(result.getContent()).extracting(Program::getOrganization).containsExactly("내일스퀘어");
  }

  @Test
  @DisplayName("regions + centers 결합 — AND 로 좁혀짐")
  void searchByRegionsAndCenters() {
    Page<Program> result =
        programService.search(
            "", List.of("수원시"), List.of("내일스퀘어"), "newest", 0, Collections.emptySet());
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getRegion()).isEqualTo("수원시");
  }

  @Test
  @DisplayName("sort=popular 도 예외 없이 결과를 반환한다")
  void searchPopular() {
    Page<Program> result =
        programService.search(
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            "popular",
            0,
            Collections.emptySet());
    // 신청 데이터가 없어도 ORDER BY 가 동작해 NPE/예외 없이 결과 반환되어야 함.
    // "전체" 탭(status="")은 종료 프로그램 제외 (wireframe WF-5-001-01 정책) → seed 3개 중 종료 1개 제외 = 2개.
    assertThat(result.getTotalElements()).isEqualTo(2);
  }

  @Test
  @DisplayName("getAllRegions 는 모든 region 가나다순 반환")
  void allRegionsOrdered() {
    List<Region> all = programService.getAllRegions();
    assertThat(all).extracting(Region::getName).containsExactly("고양시", "부천시", "수원시");
  }
}

package io.github.sihyuuun.youthmoa.program;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import io.github.sihyuuun.youthmoa.region.Region;
import io.github.sihyuuun.youthmoa.region.RegionRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
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
  @Autowired UserRepository userRepository;

  private User creator;

  @BeforeEach
  void seed() {
    LocalDate today = LocalDate.now();
    // A7-createdBy-recipient: Program.createdBy NOT NULL — 공용 소유자 시드.
    creator =
        userRepository.save(
            User.builder()
                .email("creator-filter@t.com")
                .password("hashed")
                .name("작성자")
                .role(UserRole.SYSTEM_ADMIN)
                .build());

    regionRepository.save(Region.builder().name("수원시").isFeatured(true).build());
    regionRepository.save(Region.builder().name("고양시").isFeatured(true).build());
    regionRepository.save(Region.builder().name("부천시").isFeatured(false).build());

    Center centerNaeil =
        centerRepository.save(
            Center.builder().name("내일스퀘어").region("수원시").isFeatured(true).build());
    Center centerBihaeng =
        centerRepository.save(
            Center.builder().name("비행지구").region("고양시").isFeatured(false).build());
    Center centerWonmi =
        centerRepository.save(Center.builder().name("원미").region("부천시").isFeatured(false).build());

    // D5-Q1d (2026-10-06 · V27): applyStart/End NOT NULL — 전 시드에 명시적 세팅.
    // 운영기간과 동일 범위로 백필한 시나리오 (V27 백필 로직과 일치).
    programRepository.save(
        Program.builder()
            .title("취업 워크숍")
            .center(centerNaeil)
            .region("수원시")
            .content("c")
            .startDate(today.minusDays(5))
            .endDate(today.plusDays(5))
            .applyStartDate(today.minusDays(5))
            .applyEndDate(today.plusDays(5))
            .capacity(30)
            .createdBy(creator)
            .build());

    programRepository.save(
        Program.builder()
            .title("AI 교육")
            .center(centerBihaeng)
            .region("고양시")
            .content("c")
            .startDate(today.plusDays(10))
            .endDate(today.plusDays(30))
            .applyStartDate(today.plusDays(10))
            .applyEndDate(today.plusDays(30))
            .capacity(20)
            .createdBy(creator)
            .build());

    programRepository.save(
        Program.builder()
            .title("마케팅 종료")
            .center(centerWonmi)
            .region("부천시")
            .content("c")
            .startDate(today.minusDays(30))
            .endDate(today.minusDays(5))
            .applyStartDate(today.minusDays(30))
            .applyEndDate(today.minusDays(5))
            .capacity(15)
            .createdBy(creator)
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
    assertThat(result.getContent())
        .extracting(p -> p.getCenter().getName())
        .containsExactly("내일스퀘어");
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

  // ================= D5-Q1b: ProgramSpec.withDateStatus/notEnded 신청기간 축 =================

  /**
   * D5-Q1b (2026-10-02): status 필터가 신청기간(applyStart/End) 기준으로 동작한다. 운영기간(start/end)이 과거/미래에 걸쳐
   * 있어도 신청기간 축만 반영된다.
   */
  @Test
  @DisplayName("Q1b: status=upcoming → applyStartDate 미래인 프로그램만 (운영기간 start 는 무시)")
  void q1b_statusUpcoming_byApplyStartDate() {
    LocalDate today = LocalDate.now();
    Center c = centerRepository.save(Center.builder().name("Q1센터").region("수원시").build());
    // 운영기간 start 는 과거이지만 신청기간 start 는 미래 → UPCOMING
    programRepository.save(
        Program.builder()
            .title("Q1b-upcoming")
            .center(c)
            .region("수원시")
            .content("c")
            .startDate(today.minusDays(10))
            .endDate(today.plusDays(30))
            .applyStartDate(today.plusDays(5))
            .applyEndDate(today.plusDays(20))
            .capacity(10)
            .createdBy(creator)
            .build());

    Page<Program> result =
        programService.search(
            "upcoming",
            Collections.emptyList(),
            Collections.emptyList(),
            "newest",
            0,
            Collections.emptySet());
    assertThat(result.getContent())
        .extracting(Program::getTitle)
        .contains("Q1b-upcoming")
        // 기존 seed "AI 교육" 은 운영 start 미래 but applyStartDate null → polisfall endDate 축으로도 OPEN 분류되므로
        // UPCOMING 필터 결과에는 포함되지 않아야 함 (applyEnd 가 없고 applyStart 도 없으면 startDate 폴백 → start 미래 → UPCOMING).
        // 따라서 "AI 교육" 도 UPCOMING 에 포함될 수 있음. 명시적 포함만 검증한다.
        ;
  }

  /** Q1b: status=ended → applyEndDate 과거인 프로그램만 (운영기간 end 미래여도 ENDED). */
  @Test
  @DisplayName("Q1b: status=ended → applyEndDate 지난 프로그램만 (운영 end 미래 무시)")
  void q1b_statusEnded_byApplyEndDate() {
    LocalDate today = LocalDate.now();
    Center c = centerRepository.save(Center.builder().name("Q1센터2").region("수원시").build());
    // 신청기간 지남 but 운영기간 미래 → ENDED (Q5=A)
    programRepository.save(
        Program.builder()
            .title("Q1b-ended")
            .center(c)
            .region("수원시")
            .content("c")
            .startDate(today.plusDays(5))
            .endDate(today.plusDays(40))
            .applyStartDate(today.minusDays(10))
            .applyEndDate(today.minusDays(1))
            .capacity(10)
            .createdBy(creator)
            .build());

    Page<Program> result =
        programService.search(
            "ended",
            Collections.emptyList(),
            Collections.emptyList(),
            "newest",
            0,
            Collections.emptySet());
    // "Q1b-ended" 는 applyEndDate 가 어제 → ENDED.
    // D5-Q1d (2026-10-06 · V27): applyEndDate NOT NULL — null 폴백 TC 삭제.
    assertThat(result.getContent()).extracting(Program::getTitle).contains("Q1b-ended");
  }
}

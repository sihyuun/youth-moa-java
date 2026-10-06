package io.github.sihyuuun.youthmoa.program;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

/**
 * D5-Q1b (2026-10-02): ProgramRepository 의 신청기간 축 쿼리 검증.
 *
 * <ul>
 *   <li>{@link ProgramRepository#findTop4ByIsActiveTrueOrderByApplyEndDateAsc(org.springframework.data.domain.Pageable)}
 *       — applyEndDate 가 가장 가까운 순 (apply null 이면 endDate 폴백)
 *   <li>{@link ProgramRepository#countActiveGroupByCenterId()} — effectiveApplyEnd &gt;= today 인
 *       프로그램만 센터별 카운트
 * </ul>
 *
 * <p>TODO(D5-Q1d): V27 backfill + NOT NULL 승격 후 폴백 TC 삭제, @Query 의 COALESCE 제거와 동시.
 */
@DataJpaTest
@AutoConfigureTestDatabase
@Import(JpaConfig.class)
class ProgramRepositoryQ1bTest {

  @Autowired ProgramRepository programRepository;
  @Autowired CenterRepository centerRepository;
  @Autowired UserRepository userRepository;

  private User creator;
  private Center centerA;
  private Center centerB;

  @BeforeEach
  void seed() {
    creator =
        userRepository.save(
            User.builder()
                .email("q1b-repo@t.com")
                .password("hashed")
                .name("Q1b")
                .role(UserRole.SYSTEM_ADMIN)
                .build());
    centerA = centerRepository.save(Center.builder().name("Q1센터A").region("수원시").build());
    centerB = centerRepository.save(Center.builder().name("Q1센터B").region("수원시").build());
  }

  @Test
  @DisplayName("findTop4...OrderByApplyEndDateAsc: applyEndDate 가까운 순 (운영 endDate 무관)")
  void top4OrderByApplyEnd() {
    LocalDate today = LocalDate.now();
    // 운영 endDate 는 역순이지만 applyEndDate 로 정렬돼야 함
    programRepository.save(prog("A-apply-30", today.plusDays(30), today.plusDays(1)));
    programRepository.save(prog("B-apply-5", today.plusDays(5), today.plusDays(50)));
    programRepository.save(prog("C-apply-15", today.plusDays(15), today.plusDays(100)));

    List<Program> top = programRepository.findTop4ByIsActiveTrueOrderByApplyEndDateAsc(PageRequest.of(0, 4));
    assertThat(top).extracting(Program::getTitle).containsExactly("B-apply-5", "C-apply-15", "A-apply-30");
  }

  @Test
  @DisplayName("findTop4: apply* null 이면 endDate 로 COALESCE 폴백해 정렬")
  void top4FallbackToEndDate() {
    LocalDate today = LocalDate.now();
    programRepository.save(prog("legacy-end-3", null, today.plusDays(3)));
    programRepository.save(prog("new-apply-10", today.plusDays(10), today.plusDays(100)));

    List<Program> top = programRepository.findTop4ByIsActiveTrueOrderByApplyEndDateAsc(PageRequest.of(0, 4));
    assertThat(top).extracting(Program::getTitle).containsExactly("legacy-end-3", "new-apply-10");
  }

  @Test
  @DisplayName("countActiveGroupByCenterId: applyEndDate >= today 인 프로그램만 센터별 카운트")
  void countActiveByApplyEnd() {
    LocalDate today = LocalDate.now();
    // centerA: 신청마감 미래 1건 + 신청마감 과거 1건 (제외)
    programRepository.save(progCenter("A-active", centerA, today.plusDays(5), today.plusDays(50)));
    programRepository.save(progCenter("A-ended", centerA, today.minusDays(1), today.plusDays(50)));
    // centerB: 신청마감 미래 2건
    programRepository.save(progCenter("B-active1", centerB, today.plusDays(1), today.plusDays(5)));
    programRepository.save(progCenter("B-active2", centerB, today.plusDays(10), today.plusDays(20)));

    Map<Long, Long> counts =
        programRepository.countActiveGroupByCenterId().stream()
            .collect(java.util.stream.Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

    assertThat(counts).containsEntry(centerA.getId(), 1L);
    assertThat(counts).containsEntry(centerB.getId(), 2L);
  }

  @Test
  @DisplayName("countActiveGroupByCenterId: apply* null 이어도 endDate 폴백으로 포함")
  void countActiveFallback() {
    LocalDate today = LocalDate.now();
    programRepository.save(progCenter("legacy-A", centerA, null, today.plusDays(10)));

    Map<Long, Long> counts =
        programRepository.countActiveGroupByCenterId().stream()
            .collect(java.util.stream.Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    assertThat(counts).containsEntry(centerA.getId(), 1L);
  }

  private Program prog(String title, LocalDate applyEnd, LocalDate opEnd) {
    LocalDate today = LocalDate.now();
    return Program.builder()
        .title(title)
        .center(centerA)
        .region("수원시")
        .content("c")
        .startDate(today.minusDays(1))
        .endDate(opEnd)
        .applyStartDate(applyEnd != null ? today.minusDays(1) : null)
        .applyEndDate(applyEnd)
        .capacity(10)
        .createdBy(creator)
        .build();
  }

  private Program progCenter(String title, Center c, LocalDate applyEnd, LocalDate opEnd) {
    LocalDate today = LocalDate.now();
    return Program.builder()
        .title(title)
        .center(c)
        .region("수원시")
        .content("c")
        .startDate(today.minusDays(1))
        .endDate(opEnd)
        .applyStartDate(applyEnd != null ? today.minusDays(1) : null)
        .applyEndDate(applyEnd)
        .capacity(10)
        .createdBy(creator)
        .build();
  }
}

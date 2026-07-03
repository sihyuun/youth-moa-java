package io.github.sihyuuun.youthmoa.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * ApplicationRepository.countByProgramIdsAndStatuses 단위 테스트. N+1 방지용 IN 쿼리가 올바른 프로그램별 count 를 반환하는지
 * 검증.
 */
@DataJpaTest
@AutoConfigureTestDatabase
@Import(JpaConfig.class)
class ApplicationRepositoryCountTest {

  @Autowired ApplicationRepository applicationRepository;
  @Autowired ProgramRepository programRepository;
  @Autowired UserRepository userRepository;

  private Program programA;
  private Program programB;
  private Program programC;
  private User user1;
  private User user2;
  private User user3;

  @BeforeEach
  void setUp() {
    programA =
        programRepository.save(
            Program.builder()
                .title("프로그램 A")
                .organization("기관")
                .content("내용")
                .requirements("조건")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .capacity(10)
                .build());

    programB =
        programRepository.save(
            Program.builder()
                .title("프로그램 B")
                .organization("기관")
                .content("내용")
                .requirements("조건")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .capacity(5)
                .build());

    programC =
        programRepository.save(
            Program.builder()
                .title("프로그램 C")
                .organization("기관")
                .content("내용")
                .requirements("조건")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .capacity(20)
                .build());

    user1 =
        userRepository.save(
            User.builder()
                .email("u1@test.com")
                .password("pw")
                .name("유저1")
                .role(UserRole.USER)
                .build());
    user2 =
        userRepository.save(
            User.builder()
                .email("u2@test.com")
                .password("pw")
                .name("유저2")
                .role(UserRole.USER)
                .build());
    user3 =
        userRepository.save(
            User.builder()
                .email("u3@test.com")
                .password("pw")
                .name("유저3")
                .role(UserRole.USER)
                .build());
  }

  private Application apply(User user, Program program, ApplicationStatus status) {
    return applicationRepository.save(
        Application.builder().user(user).program(program).status(status).build());
  }

  @Test
  @DisplayName("각 프로그램의 PENDING+APPROVED 신청자 수를 프로그램별로 집계한다")
  void countByProgramIds_correctPerProgram() {
    // programA: PENDING 2건 / programB: APPROVED 1건 / programC: 신청 없음
    apply(user1, programA, ApplicationStatus.PENDING);
    apply(user2, programA, ApplicationStatus.PENDING);
    apply(user3, programB, ApplicationStatus.APPROVED);

    List<Long> ids = List.of(programA.getId(), programB.getId(), programC.getId());
    List<ApplicationStatus> statuses =
        List.of(ApplicationStatus.PENDING, ApplicationStatus.APPROVED);

    List<Object[]> rows = applicationRepository.countByProgramIdsAndStatuses(ids, statuses);
    Map<Long, Long> countMap =
        rows.stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

    assertThat(countMap.get(programA.getId())).isEqualTo(2L);
    assertThat(countMap.get(programB.getId())).isEqualTo(1L);
    // programC 는 신청 없으므로 결과 row 자체가 없음 (GROUP BY 특성)
    assertThat(countMap).doesNotContainKey(programC.getId());
  }

  @Test
  @DisplayName("CANCELLED 상태는 카운트에서 제외된다")
  void countByProgramIds_excludesCancelled() {
    apply(user1, programA, ApplicationStatus.PENDING);
    apply(user2, programA, ApplicationStatus.CANCELLED); // 제외 대상

    List<Long> ids = List.of(programA.getId());
    List<ApplicationStatus> statuses =
        List.of(ApplicationStatus.PENDING, ApplicationStatus.APPROVED);

    List<Object[]> rows = applicationRepository.countByProgramIdsAndStatuses(ids, statuses);
    Map<Long, Long> countMap =
        rows.stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

    // CANCELLED 제외 → PENDING 1건만 카운트
    assertThat(countMap.get(programA.getId())).isEqualTo(1L);
  }

  @Test
  @DisplayName("programIds 목록에 없는 프로그램은 결과에 포함되지 않는다")
  void countByProgramIds_onlyQueriedIds() {
    apply(user1, programA, ApplicationStatus.PENDING);
    apply(user2, programB, ApplicationStatus.PENDING);

    // programB 만 조회
    List<Long> ids = List.of(programB.getId());
    List<ApplicationStatus> statuses = List.of(ApplicationStatus.PENDING);

    List<Object[]> rows = applicationRepository.countByProgramIdsAndStatuses(ids, statuses);
    Map<Long, Long> countMap =
        rows.stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

    assertThat(countMap).containsOnlyKeys(programB.getId());
    assertThat(countMap.get(programB.getId())).isEqualTo(1L);
  }

  @Test
  @DisplayName("빈 programIds 목록으로 조회하면 빈 결과를 반환한다")
  void countByProgramIds_emptyIds_returnsEmpty() {
    apply(user1, programA, ApplicationStatus.PENDING);

    List<Object[]> rows =
        applicationRepository.countByProgramIdsAndStatuses(
            List.of(), List.of(ApplicationStatus.PENDING));

    assertThat(rows).isEmpty();
  }
}

package io.github.sihyuuun.youthmoa.notification.watch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

/**
 * A7-watcher-ui (2026-09-28) — {@link ProgramWatchService} 단위 테스트.
 *
 * <p>BookmarkServiceTest 와 유사한 통합-슬라이스 방식 (@DataJpaTest + H2). Q-A7W-8 20개 상한 자동 삭제, toggle
 * idempotent, getWatchedProgramIds batch 조회를 검증.
 */
@DataJpaTest
@AutoConfigureTestDatabase
@Import({JpaConfig.class, ProgramWatchService.class})
class ProgramWatchServiceTest {

  @Autowired ProgramWatchService programWatchService;
  @Autowired ProgramWatchRepository programWatchRepository;
  @Autowired UserRepository userRepository;
  @Autowired ProgramRepository programRepository;
  @Autowired CenterRepository centerRepository;

  private User admin;
  private Program program;
  private Center center;

  @BeforeEach
  void seed() {
    center =
        centerRepository.save(Center.builder().name("내일스퀘어").region("수원시").isActive(true).build());
    admin =
        userRepository.save(
            User.builder()
                .email("watch-admin@test.com")
                .password("hashed")
                .name("지켜보기 관리자")
                .role(UserRole.CENTER_ADMIN)
                .center(center)
                .build());

    program = saveProgram("샘플 프로그램");
  }

  private Program saveProgram(String title) {
    return programRepository.save(
        Program.builder()
            .title(title)
            .center(center)
            .category("취업")
            .region("수원시")
            .content("c")
            .startDate(LocalDate.now().minusDays(5))
            .endDate(LocalDate.now().plusDays(10))
            .capacity(30)
            .createdBy(admin)
            .build());
  }

  @Test
  @DisplayName("첫 토글은 지켜보기 등록 → true 반환")
  void toggle_first_adds() {
    boolean result = programWatchService.toggle(admin.getEmail(), program.getId());

    assertThat(result).isTrue();
    assertThat(programWatchRepository.count()).isEqualTo(1);
    assertThat(programWatchRepository.existsByAdminAndProgram(admin, program)).isTrue();
  }

  @Test
  @DisplayName("두 번째 토글은 지켜보기 해제 → false 반환 (idempotent 반복 가능)")
  void toggle_second_removes() {
    programWatchService.toggle(admin.getEmail(), program.getId());
    boolean result = programWatchService.toggle(admin.getEmail(), program.getId());

    assertThat(result).isFalse();
    assertThat(programWatchRepository.count()).isEqualTo(0);
  }

  @Test
  @DisplayName("연속 토글 3번 → 최종 등록 상태")
  void toggle_three_times() {
    programWatchService.toggle(admin.getEmail(), program.getId()); // add
    programWatchService.toggle(admin.getEmail(), program.getId()); // remove
    boolean result = programWatchService.toggle(admin.getEmail(), program.getId()); // add

    assertThat(result).isTrue();
    assertThat(programWatchRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("isWatched — 등록 후 true")
  void isWatched_after_add() {
    programWatchService.toggle(admin.getEmail(), program.getId());
    assertThat(programWatchService.isWatched(admin.getEmail(), program.getId())).isTrue();
  }

  @Test
  @DisplayName("isWatched — email null / 미존재 사용자 / 미존재 프로그램 → false")
  void isWatched_defensive() {
    assertThat(programWatchService.isWatched(null, program.getId())).isFalse();
    assertThat(programWatchService.isWatched("ghost@nowhere.com", program.getId())).isFalse();
    assertThat(programWatchService.isWatched(admin.getEmail(), 999_999L)).isFalse();
  }

  @Test
  @DisplayName("toggle — 존재하지 않는 프로그램 → IllegalArgumentException")
  void toggle_program_notFound() {
    assertThatThrownBy(() -> programWatchService.toggle(admin.getEmail(), 999_999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("프로그램을 찾을 수 없어요");
  }

  @Test
  @DisplayName("toggle — 존재하지 않는 사용자 → IllegalArgumentException")
  void toggle_user_notFound() {
    assertThatThrownBy(() -> programWatchService.toggle("ghost@nowhere.com", program.getId()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("관리자를 찾을 수 없어요");
  }

  @Test
  @DisplayName("getWatchedProgramIds — N개 등록 후 정확한 ID Set 반환")
  void getWatchedProgramIds_returnsAll() {
    Program p2 = saveProgram("샘플 2");
    Program p3 = saveProgram("샘플 3");

    programWatchService.toggle(admin.getEmail(), program.getId());
    programWatchService.toggle(admin.getEmail(), p3.getId());
    // p2 는 등록 안 함

    assertThat(programWatchService.getWatchedProgramIds(admin.getEmail()))
        .containsExactlyInAnyOrder(program.getId(), p3.getId())
        .doesNotContain(p2.getId());
  }

  @Test
  @DisplayName("getWatchedProgramIds — email null 이면 빈 Set")
  void getWatchedProgramIds_null_email() {
    assertThat(programWatchService.getWatchedProgramIds(null)).isEmpty();
  }

  @Test
  @DisplayName("findWatchedPrograms — 최근순 페이지 반환 (대시보드 카드용)")
  void findWatchedPrograms_pagination() {
    Program p2 = saveProgram("샘플 2");
    Program p3 = saveProgram("샘플 3");

    programWatchService.toggle(admin.getEmail(), program.getId());
    programWatchService.toggle(admin.getEmail(), p2.getId());
    programWatchService.toggle(admin.getEmail(), p3.getId());

    var page = programWatchService.findWatchedPrograms(admin.getEmail(), PageRequest.of(0, 2));
    assertThat(page.getContent()).hasSize(2);
    // 실 정렬은 findAllByAdmin 의 default (id) — 카드 상에서는 program.title 등 사용. 개수만 검증.
    assertThat(page.getTotalElements()).isEqualTo(3);
  }

  // ── Q-A7W-8 20개 상한 자동 삭제 정책 ─────────────────────────────────────

  @Test
  @DisplayName("Q-A7W-8: 21번째 등록 시 가장 오래된 1건 자동 삭제 (총 20개 유지)")
  void toggle_maxLimit_evictsOldest() {
    // 20개 시드
    Long firstProgramId = program.getId();
    for (int i = 2; i <= 20; i++) {
      Program p = saveProgram("샘플 " + i);
      programWatchService.toggle(admin.getEmail(), p.getId());
    }
    programWatchService.toggle(admin.getEmail(), firstProgramId);
    assertThat(programWatchRepository.count()).isEqualTo(20);

    // 21번째 등록 → 가장 오래된 (샘플 2) 삭제
    Program overflow = saveProgram("오버플로우");
    boolean added = programWatchService.toggle(admin.getEmail(), overflow.getId());

    assertThat(added).isTrue();
    assertThat(programWatchRepository.count()).isEqualTo(20);
    assertThat(programWatchRepository.existsByAdminAndProgram(admin, overflow)).isTrue();
    Program oldest =
        programRepository.findAll().stream()
            .filter(p -> "샘플 2".equals(p.getTitle()))
            .findFirst()
            .orElseThrow();
    assertThat(programWatchRepository.existsByAdminAndProgram(admin, oldest)).isFalse();
  }

  @Test
  @DisplayName("Q-A7W-1 UNIQUE 제약 준수: 동일 (admin, program) 재등록은 신규 row 생성 안 함 (토글로 처리)")
  void unique_constraint_prevents_duplicate() {
    programWatchService.toggle(admin.getEmail(), program.getId());
    // toggle 두 번째 호출은 삭제 → 재호출은 신규 row 이지만 count 는 항상 <=1
    programWatchService.toggle(admin.getEmail(), program.getId());
    programWatchService.toggle(admin.getEmail(), program.getId());
    assertThat(programWatchRepository.countByAdmin(admin)).isEqualTo(1);
  }
}

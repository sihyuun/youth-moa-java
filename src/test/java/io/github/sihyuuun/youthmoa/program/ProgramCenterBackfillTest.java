package io.github.sihyuuun.youthmoa.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * A9-a (2026-09-21): {@link ProgramCenterBackfill} 단위 검증.
 *
 * <p>@DataJpaTest 환경에서는 ApplicationRunner 자체가 자동 트리거되지 않으므로, 러너를 수동 생성해 로직만 실행한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase
@Import(JpaConfig.class)
class ProgramCenterBackfillTest {

  @Autowired ProgramRepository programRepository;
  @Autowired CenterRepository centerRepository;

  @Test
  @DisplayName("organization → Center.name 매칭 프로그램은 center_id 가 채워진다")
  void backfill_success() {
    // given: Center 하나 + organization 만 채워진 Program 하나
    Center center =
        centerRepository.save(
            Center.builder()
                .name("내일스퀘어 양평")
                .region("양평군")
                .isActive(true)
                .isFeatured(false)
                .build());
    Program program =
        programRepository.save(
            Program.builder()
                .title("취업 특강")
                .organization("내일스퀘어 양평")
                .content("본문")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .build());
    assertThat(program.getCenter()).isNull();

    // when
    ProgramCenterBackfill runner = new ProgramCenterBackfill(programRepository, centerRepository);
    runner.run(null);

    // then
    Program reloaded = programRepository.findById(program.getId()).orElseThrow();
    assertThat(reloaded.getCenter()).isNotNull();
    assertThat(reloaded.getCenter().getId()).isEqualTo(center.getId());
  }

  @Test
  @DisplayName("매칭 실패 organization 이 있으면 fail-fast — IllegalStateException 던지고 부팅 중단")
  void backfill_fails_fast_on_unresolved() {
    // given: Center 없음, Program 하나
    programRepository.save(
        Program.builder()
            .title("존재하지 않는 센터의 프로그램")
            .organization("없는센터")
            .content("본문")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(10))
            .build());

    ProgramCenterBackfill runner = new ProgramCenterBackfill(programRepository, centerRepository);

    assertThatThrownBy(() -> runner.run(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("없는센터")
        .hasMessageContaining("A9-a backfill");
  }

  @Test
  @DisplayName("center_id 이미 채워진 프로그램은 건너뛴다 (재실행 idempotent)")
  void backfill_skips_already_assigned() {
    Center center =
        centerRepository.save(
            Center.builder().name("A센터").region("서울").isActive(true).isFeatured(false).build());
    Program program =
        programRepository.save(
            Program.builder()
                .title("이미 매핑됨")
                .center(center)
                .content("본문")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .build());

    ProgramCenterBackfill runner = new ProgramCenterBackfill(programRepository, centerRepository);
    runner.run(null); // skip 되어야 함 (예외 없이 통과)

    Program reloaded = programRepository.findById(program.getId()).orElseThrow();
    assertThat(reloaded.getCenter().getId()).isEqualTo(center.getId());
  }
}

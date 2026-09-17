package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * A8 admin-bulk-csv (2026-09-17): {@link AdminProgramBulkService} 검증.
 *
 * <p>Qn-P1 결정: bulk publish/unpublish 없음 · bulk deactivate/reactivate (isActive) 만 존재. 소프트 삭제 정합.
 */
@SpringBootTest
@ActiveProfiles("e2e")
class AdminProgramBulkServiceTest {

  @Autowired AdminProgramBulkService bulkService;
  @Autowired ProgramRepository programRepository;

  @AfterEach
  void cleanup() {
    // 시드 프로그램 활성 상태 원복
    programRepository
        .findAll()
        .forEach(
            p -> {
              if (!p.isActive()) {
                p.activate();
                programRepository.save(p);
              }
            });
  }

  @Test
  void bulkDeactivate_softly_marks_inactive() {
    List<Program> programs = programRepository.findAll();
    Long id1 = programs.get(0).getId();
    Long id2 = programs.get(1).getId();
    BulkResult result = bulkService.bulkDeactivate(List.of(id1, id2));
    assertThat(result.getSuccessCount()).isEqualTo(2);
    assertThat(result.getFailCount()).isEqualTo(0);
    assertThat(programRepository.findById(id1).orElseThrow().isActive()).isFalse();
    assertThat(programRepository.findById(id2).orElseThrow().isActive()).isFalse();
  }

  @Test
  void bulkDeactivate_missing_id_fails_softly() {
    BulkResult result = bulkService.bulkDeactivate(List.of(999_999L));
    assertThat(result.getFailCount()).isEqualTo(1);
    assertThat(result.getSuccessCount()).isEqualTo(0);
  }

  @Test
  void bulkReactivate_restores_isActive() {
    Program p = programRepository.findAll().get(0);
    Long id = p.getId();
    bulkService.bulkDeactivate(List.of(id));
    assertThat(programRepository.findById(id).orElseThrow().isActive()).isFalse();
    BulkResult r = bulkService.bulkReactivate(List.of(id));
    assertThat(r.getSuccessCount()).isEqualTo(1);
    assertThat(programRepository.findById(id).orElseThrow().isActive()).isTrue();
  }

  @Test
  void empty_ids_returns_zero() {
    BulkResult r = bulkService.bulkDeactivate(List.of());
    assertThat(r.getTotal()).isEqualTo(0);
  }
}

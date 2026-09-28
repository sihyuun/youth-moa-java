package io.github.sihyuuun.youthmoa.notification.watch;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.user.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * A7-watcher-ui (2026-09-28) — ProgramWatch 조회/삭제.
 *
 * <ul>
 *   <li>{@link #existsByAdminAndProgram} — 토글 시 등록 여부 확인
 *   <li>{@link #deleteByAdminAndProgram} — 토글 해제
 *   <li>{@link #findAllByAdminOrderByCreatedAtDesc(User)} — 20개 상한 검사용 (오래된 자동 삭제)
 *   <li>{@link #findAllByAdmin(User, Pageable)} — 대시보드 카드 최근 5개용
 *   <li>{@link #findAdminsByProgramId} — B-3-B fan-out 진입점
 *   <li>{@link #findProgramIdsByAdmin} — 목록 렌더 시 N+1 방지용 batch 조회
 * </ul>
 */
public interface ProgramWatchRepository extends JpaRepository<ProgramWatch, Long> {

  boolean existsByAdminAndProgram(User admin, Program program);

  void deleteByAdminAndProgram(User admin, Program program);

  /** 20개 상한 감시용. 신규→과거 순. 초과 시 tail (오래된) 잘라 삭제. */
  List<ProgramWatch> findAllByAdminOrderByCreatedAtDesc(User admin);

  /**
   * 대시보드 카드용. program (EAGER) + program.center fetch join 으로 카드 렌더 N+1 회피. Bookmark 패턴 승계 (A9-b
   * center 접근).
   */
  @EntityGraph(attributePaths = {"program", "program.center"})
  Page<ProgramWatch> findAllByAdmin(User admin, Pageable pageable);

  /**
   * B-3-B fan-out — 특정 프로그램을 지켜보는 활성 admin 목록. Q-A7W-6 A: 비활성 admin 은 스킵. Q-A7W-7 A: 프로그램의
   * SUSPENDED 여부와 무관 (본 쿼리에서는 필터 없음, 정책은 Resolver 상위에서 처리 안 함).
   */
  @Query(
      "SELECT w.admin FROM ProgramWatch w "
          + "WHERE w.program.id = :programId AND w.admin.isActive = true "
          + "ORDER BY w.createdAt ASC")
  List<User> findAdminsByProgramId(Long programId);

  /** 목록 화면에서 현재 admin 이 watch 등록한 programId Set. 카드/row N개 렌더 시 batch 조회. */
  @Query("SELECT w.program.id FROM ProgramWatch w WHERE w.admin = :admin")
  List<Long> findProgramIdsByAdmin(User admin);

  long countByAdmin(User admin);
}

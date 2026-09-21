package io.github.sihyuuun.youthmoa.program;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ProgramRepository
    extends JpaRepository<Program, Long>, JpaSpecificationExecutor<Program> {

  Page<Program> findAllByIsActiveTrue(Pageable pageable);

  Page<Program> findAllByCategoryAndIsActiveTrue(String category, Pageable pageable);

  @Query(
      "SELECT DISTINCT p.region FROM Program p WHERE p.isActive = true AND p.region IS NOT NULL ORDER BY p.region")
  List<String> findDistinctRegions();

  /** 홈 Top 4 (모집중 + 마감임박 정렬). */
  List<Program> findTop4ByIsActiveTrueOrderByEndDateAsc();

  /** 홈 Quick Stats — 모집중 프로그램 카운트. */
  long countByIsActiveTrue();

  /**
   * A9-a (2026-09-21): 센터별 진행중 프로그램 카운트 — Center FK 기반. Row = {@code (centerId: Long, count: Long)}.
   *
   * <p>center_id 가 null 인 row 는 제외 (backfill 이 채워 놓기 때문에 정상 상태에서는 없어야 함).
   *
   * <p>사용처: F0h-c2 센터 카드 "진행중 프로그램 N건" 배지.
   */
  @Query(
      "SELECT p.center.id, COUNT(p) FROM Program p "
          + "WHERE p.isActive = true "
          + "AND p.center IS NOT NULL "
          + "AND (p.endDate IS NULL OR p.endDate >= CURRENT_DATE) "
          + "GROUP BY p.center.id")
  List<Object[]> countActiveGroupByCenterId();
}

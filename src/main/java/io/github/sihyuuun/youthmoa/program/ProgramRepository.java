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
   * F0h-c2: 특정 센터(organization 매칭) 의 진행중 프로그램 카운트.
   *
   * <p>Program 은 Center FK 를 갖지 않으므로 organization 문자열 매칭으로 근사. isActive=true 이고 endDate 가 오늘 이후(포함)
   * 인 프로그램만 카운트.
   */
  @org.springframework.data.jpa.repository.Query(
      "SELECT p.organization, COUNT(p) FROM Program p "
          + "WHERE p.isActive = true "
          + "AND (p.endDate IS NULL OR p.endDate >= CURRENT_DATE) "
          + "GROUP BY p.organization")
  java.util.List<Object[]> countActiveGroupByOrganization();
}

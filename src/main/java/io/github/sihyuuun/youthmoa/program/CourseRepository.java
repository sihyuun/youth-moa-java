package io.github.sihyuuun.youthmoa.program;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** A3-2 admin-program-form-integration (2026-09-11): Course 조회 Repository. */
public interface CourseRepository extends JpaRepository<Course, Long> {

  /** 사용자·admin 렌더용 — 활성 강좌만 sortOrder 순. */
  List<Course> findByProgramIdAndIsActiveTrueOrderBySortOrderAscIdAsc(Long programId);

  /** admin upsert 용 — 활성/비활성 전부 (편집 폼에 재활성화 UX 없음이라도 일관성 유지). */
  List<Course> findByProgramIdOrderBySortOrderAscIdAsc(Long programId);
}

package io.github.sihyuuun.youthmoa.program;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplyQuestionRepository extends JpaRepository<ApplyQuestion, Long> {

  /** 사용자 apply 폼 렌더용 — 활성 필드만 sort_order 순. */
  List<ApplyQuestion> findByProgramIdAndIsActiveTrueOrderBySortOrderAsc(Long programId);

  /** admin 목록용 — 활성/비활성 전부. */
  List<ApplyQuestion> findByProgramIdOrderBySortOrderAscIdAsc(Long programId);

  boolean existsByProgramIdAndIsActiveTrue(Long programId);
}

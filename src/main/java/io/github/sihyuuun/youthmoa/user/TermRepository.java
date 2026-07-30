package io.github.sihyuuun.youthmoa.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermRepository extends JpaRepository<Term, Long> {

  /** 회원가입 폼 렌더 · 서비스 검증 진입점 — 활성 약관만 sortOrder 오름차순. */
  List<Term> findByIsActiveTrueOrderBySortOrderAsc();

  Optional<Term> findByCode(String code);
}

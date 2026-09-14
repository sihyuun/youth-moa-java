package io.github.sihyuuun.youthmoa.application;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplyAnswerRepository extends JpaRepository<ApplyAnswer, Long> {

  List<ApplyAnswer> findByApplicationId(Long applicationId);

  /** A4 admin-program-detail: 신청 상세 모달용. question 을 fetch join 해 라벨·타입 렌더. */
  @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"question"})
  List<ApplyAnswer> findByApplicationIdOrderByIdAsc(Long applicationId);

  /** 소프트 삭제 UX 안내용 — 이 질문에 대한 응답이 이미 존재하는지. */
  boolean existsByQuestionId(Long questionId);

  long countByQuestionId(Long questionId);
}

package io.github.sihyuuun.youthmoa.notice;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

  /**
   * 관리자 목록/편집 화면용 — {@code Notice.createdBy} LAZY 프록시가 뷰 렌더 시점에 초기화되지 못하고 {@code
   * LazyInitializationException} 을 발생시키는 회귀 방지 (open-in-view=false 조합). EntityGraph 로 createdBy 를
   * 즉시 로딩.
   */
  @EntityGraph(attributePaths = "createdBy")
  @Query("select n from Notice n")
  Page<Notice> findAllWithCreatedBy(Pageable pageable);

  @EntityGraph(attributePaths = "createdBy")
  @Query("select n from Notice n where n.id = :id")
  Optional<Notice> findByIdWithCreatedBy(Long id);

  List<Notice> findAllByIsPinnedTrueOrderByCreatedAtDesc();

  Page<Notice> findAllByCategory(NoticeCategory category, Pageable pageable);

  /** 홈 대표 공지 1건 (pinned + 최신). */
  Optional<Notice> findTop1ByIsPinnedTrueOrderByCreatedAtDesc();

  /** 홈 서브 공지 3건 (pinned 아닌 것 중 최신). */
  List<Notice> findTop3ByIsPinnedFalseOrderByCreatedAtDesc();

  /** 상세 페이지 "이전글" — 카테고리 무관, id 기준. */
  Optional<Notice> findFirstByIdLessThanOrderByIdDesc(Long id);

  /** 상세 페이지 "다음글" — 카테고리 무관, id 기준. */
  Optional<Notice> findFirstByIdGreaterThanOrderByIdAsc(Long id);

  /**
   * D4 통합 검색 — 제목 + 본문 OR LIKE 대소문자 무시. 260826 chore/content-lob-to-text: content
   * 를 @JdbcTypeCode(LONGVARCHAR) 매핑으로 이관 → PG text · H2 VARCHAR(MAX). Hibernate 6 SQM STRING 확정. 이전
   * CLOB grammar 우회로 도입했던 findByTitleContainingIgnoreCase (title 만) ·
   * findByTitleContainingIgnoreCaseOrSummaryContainingIgnoreCase 두 메서드는 폐기 · 원 메서드 복원.
   */
  Page<Notice> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
      String titleKeyword, String contentKeyword, Pageable pageable);
}

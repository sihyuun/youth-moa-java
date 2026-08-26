package io.github.sihyuuun.youthmoa.notice;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

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
   * D4 통합 검색 — 제목 대소문자 무시 부분 일치.
   *
   * <p>260826 P9 fix: content 필드는 @Lob → CLOB 이라 Hibernate 6 이 UPPER(CLOB) 호출 시
   * BadJpqlGrammarException 발생 → 검색 대상에서 제외. 본문 검색은 별도 fulltext 인덱스 · Elasticsearch · varchar
   * summary 필드 등 향후 트랙에서 도입.
   */
  Page<Notice> findByTitleContainingIgnoreCase(String titleKeyword, Pageable pageable);

  /**
   * 260826 P9 후속: 제목 + 요약(VARCHAR 300) OR LIKE. content 는 여전히 검색 대상 아님(@Lob CLOB 문제). summary 는
   * DataInitializer 에서 content 앞 300자로 자동 파생.
   */
  Page<Notice> findByTitleContainingIgnoreCaseOrSummaryContainingIgnoreCase(
      String titleKeyword, String summaryKeyword, Pageable pageable);
}

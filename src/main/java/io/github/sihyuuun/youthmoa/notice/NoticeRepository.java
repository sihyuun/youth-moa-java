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

  /** D4 통합 검색 — 제목·본문 대소문자 무시 부분 일치. */
  Page<Notice> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
      String titleKeyword, String contentKeyword, Pageable pageable);
}

package io.github.sihyuuun.youthmoa.notice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByIsPinnedTrueOrderByCreatedAtDesc();

    Page<Notice> findAllByTag(String tag, Pageable pageable);

    /** 홈 대표 공지 1건 (pinned + 최신). */
    Optional<Notice> findTop1ByIsPinnedTrueOrderByCreatedAtDesc();

    /** 홈 서브 공지 3건 (pinned 아닌 것 중 최신). */
    List<Notice> findTop3ByIsPinnedFalseOrderByCreatedAtDesc();
}

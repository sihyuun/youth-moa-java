package io.github.sihyuuun.youthmoa.notice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByIsPinnedTrueOrderByCreatedAtDesc();

    Page<Notice> findAllByTag(String tag, Pageable pageable);
}

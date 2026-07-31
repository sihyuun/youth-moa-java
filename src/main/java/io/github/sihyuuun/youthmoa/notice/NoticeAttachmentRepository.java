package io.github.sihyuuun.youthmoa.notice;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {

  List<NoticeAttachment> findByNoticeIdOrderBySortOrderAscIdAsc(Long noticeId);

  /** 다운로드 컨트롤러용 — 첨부파일 id 와 소속 공지 id 를 동시 검증. 다른 공지 소속 첨부파일을 URL 조작으로 접근하는 것을 서비스 계층에서 차단. */
  Optional<NoticeAttachment> findByIdAndNoticeId(Long id, Long noticeId);
}

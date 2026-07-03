package io.github.sihyuuun.youthmoa.notice;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {

  List<NoticeAttachment> findByNoticeIdOrderBySortOrderAscIdAsc(Long noticeId);
}

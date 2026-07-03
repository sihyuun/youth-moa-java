package io.github.sihyuuun.youthmoa.notice;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoticeService {

  public static final int PAGE_SIZE = 10;
  public static final int PAGE_GROUP_SIZE = 5;

  private final NoticeRepository noticeRepository;
  private final NoticeAttachmentRepository noticeAttachmentRepository;

  /**
   * 공지 목록 조회. 정렬은 pinned DESC, id DESC 로 고정 (핀 상단 후 최신 id 순).
   *
   * @param category null 이면 전체
   * @param page 0-based
   */
  @Transactional(readOnly = true)
  public Page<Notice> list(NoticeCategory category, int page) {
    Pageable pageable =
        PageRequest.of(
            Math.max(0, page),
            PAGE_SIZE,
            Sort.by(Sort.Order.desc("isPinned"), Sort.Order.desc("id")));
    if (category == null) {
      return noticeRepository.findAll(pageable);
    }
    return noticeRepository.findAllByCategory(category, pageable);
  }

  /** 상세 조회 + viewCount++ (매 진입). @Lob content 접근을 위해 트랜잭션 필수. */
  @Transactional
  public Notice detailAndIncreaseView(Long id) {
    Notice notice =
        noticeRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지입니다: " + id));
    notice.increaseViewCount();
    return notice;
  }

  @Transactional(readOnly = true)
  public Optional<Notice> findPrev(Long id) {
    return noticeRepository.findFirstByIdLessThanOrderByIdDesc(id);
  }

  @Transactional(readOnly = true)
  public Optional<Notice> findNext(Long id) {
    return noticeRepository.findFirstByIdGreaterThanOrderByIdAsc(id);
  }

  @Transactional(readOnly = true)
  public List<NoticeAttachment> findAttachments(Long noticeId) {
    return noticeAttachmentRepository.findByNoticeIdOrderBySortOrderAscIdAsc(noticeId);
  }
}

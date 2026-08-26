package io.github.sihyuuun.youthmoa.notice;

import java.util.List;
import java.util.Optional;
import java.util.Set;
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

  // F-notice-attachment 정책 상수 (2026-07-31 사용자 확정):
  // - 파일 크기 상한 5MB — DB @Lob 부담 방지 (Supabase Storage 이관 시 재검토)
  // - 허용 확장자 pdf/hwp/docx/xlsx — 관공서 공지 관례
  // 상한 검증은 업로드 시점에 이루어지며 (admin 트랙 별도 PR), 다운로드 서비스는 저장된 무결성만 스트리밍.
  public static final long MAX_ATTACHMENT_SIZE_BYTES = 5L * 1024 * 1024;
  public static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "hwp", "docx", "xlsx");

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

  /**
   * 상세 조회 + viewCount++ (매 진입). 트랜잭션은 viewCount 갱신 · 동시성 위함. 260826 chore/content-lob-to-text:
   * content 는 이제 @JdbcTypeCode(LONGVARCHAR) 매핑이라 @Lob 스트리밍 트랜잭션 요구는 사라졌지만 write 트랜잭션은 유지.
   */
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

  /**
   * F-notice-attachment: 다운로드 컨트롤러 진입점. 소속 공지 검증 후 파일 반환. LAZY {@code data} 필드 접근을 위해 트랜잭션 필수. 다른
   * 공지 소속 첨부이거나 존재하지 않으면 {@link IllegalArgumentException}. 컨트롤러가 404 로 매핑.
   */
  @Transactional(readOnly = true)
  public NoticeAttachment findAttachmentForDownload(Long noticeId, Long attachmentId) {
    NoticeAttachment attachment =
        noticeAttachmentRepository
            .findByIdAndNoticeId(attachmentId, noticeId)
            .orElseThrow(() -> new IllegalArgumentException("첨부파일을 찾을 수 없습니다."));
    if (attachment.getData() == null || attachment.getData().length == 0) {
      // legacy 메타 전용 시드(F0g) — data 미주입 상태. 사용자 관점에서는 파일 없음과 동일.
      throw new IllegalArgumentException("첨부파일 데이터가 없습니다.");
    }
    return attachment;
  }

  /** 파일명에서 확장자 소문자 추출. 확장자 없으면 빈 문자열. 업로드 검증·시드 로직 공용. */
  public static String extensionOf(String fileName) {
    if (fileName == null) return "";
    int dot = fileName.lastIndexOf('.');
    if (dot < 0 || dot == fileName.length() - 1) return "";
    return fileName.substring(dot + 1).toLowerCase();
  }
}

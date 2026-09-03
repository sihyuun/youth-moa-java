package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.common.storage.FileStorage;
import io.github.sihyuuun.youthmoa.common.storage.StoredFile;
import io.github.sihyuuun.youthmoa.notice.Notice;
import io.github.sihyuuun.youthmoa.notice.NoticeAttachment;
import io.github.sihyuuun.youthmoa.notice.NoticeAttachmentRepository;
import io.github.sihyuuun.youthmoa.notice.NoticeCategory;
import io.github.sihyuuun.youthmoa.notice.NoticeRepository;
import io.github.sihyuuun.youthmoa.notice.NoticeService;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * A-admin-notice-attachment (2026-09-03 · Qn-5 A · Qn-8 Custom): 관리자 공지 CRUD + 첨부 관리 서비스.
 *
 * <p>RBAC:
 *
 * <ul>
 *   <li>Create / Read — SYSTEM_ADMIN · CENTER_ADMIN 모두 가능
 *   <li>Update / Delete — SYSTEM_ADMIN 모두, CENTER_ADMIN 은 본인 작성 공지만
 * </ul>
 *
 * <p>업로드 검증: 크기 5MB / 확장자 pdf·hwp·docx·xlsx / Content-Type 헤더 매칭 (Qn-4 A).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNoticeService {

  public static final int ADMIN_PAGE_SIZE = 20;

  /** Qn-4 A: 확장자별 허용 Content-Type 헤더. multipart Content-Type 이 이 집합 안에 있어야 통과. */
  public static final Map<String, Set<String>> ALLOWED_MIME =
      Map.of(
          "pdf", Set.of("application/pdf"),
          "hwp",
              Set.of(
                  "application/x-hwp",
                  "application/haansofthwp",
                  "application/vnd.hancom.hwp",
                  "application/octet-stream"),
          "docx",
              Set.of(
                  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                  "application/octet-stream"),
          "xlsx",
              Set.of(
                  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                  "application/octet-stream"));

  private final NoticeRepository noticeRepository;
  private final NoticeAttachmentRepository noticeAttachmentRepository;
  private final FileStorage fileStorage;

  @Value("${youthmoa.storage.supabase.notice-bucket:notice-attachments}")
  private String noticeBucket;

  // ================= RBAC =================

  /** SYSTEM_ADMIN 또는 본인 작성 공지 여부. Update/Delete/첨부 CRUD 진입 시 필수. */
  public boolean canEdit(Notice notice, User currentUser) {
    if (currentUser == null || notice == null) return false;
    if (currentUser.getRole() == UserRole.SYSTEM_ADMIN) return true;
    if (currentUser.getRole() == UserRole.CENTER_ADMIN) {
      User creator = notice.getCreatedBy();
      return creator != null && creator.getId().equals(currentUser.getId());
    }
    return false;
  }

  private void assertCanEdit(Notice notice, User currentUser) {
    if (!canEdit(notice, currentUser)) {
      throw new AccessDeniedException("이 공지를 수정할 권한이 없어요.");
    }
  }

  // ================= CRUD =================

  @Transactional(readOnly = true)
  public Page<Notice> list(int page) {
    Pageable pageable =
        PageRequest.of(
            Math.max(0, page),
            ADMIN_PAGE_SIZE,
            Sort.by(Sort.Order.desc("isPinned"), Sort.Order.desc("id")));
    return noticeRepository.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public Notice findById(Long id) {
    return noticeRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지에요: " + id));
  }

  @Transactional
  public Notice create(
      String title,
      String content,
      NoticeCategory category,
      boolean isPinned,
      String imageUrl,
      User currentUser) {
    validateTitleContent(title, content);
    Notice notice =
        Notice.builder()
            .title(title.trim())
            .content(content)
            .category(category != null ? category : NoticeCategory.NOTICE)
            .isPinned(isPinned)
            .imageUrl(imageUrl)
            .createdBy(currentUser)
            .build();
    return noticeRepository.save(notice);
  }

  @Transactional
  public Notice update(
      Long noticeId,
      String title,
      String content,
      NoticeCategory category,
      boolean isPinned,
      String imageUrl,
      User currentUser) {
    validateTitleContent(title, content);
    Notice notice = findById(noticeId);
    assertCanEdit(notice, currentUser);
    notice.update(
        title.trim(),
        content,
        category != null ? category : NoticeCategory.NOTICE,
        isPinned,
        imageUrl);
    return notice;
  }

  @Transactional
  public void delete(Long noticeId, User currentUser) {
    Notice notice = findById(noticeId);
    assertCanEdit(notice, currentUser);

    // 첨부 파일 저장소 정리 (best-effort — DB 삭제는 cascade 없으므로 명시적)
    List<NoticeAttachment> attachments =
        noticeAttachmentRepository.findByNoticeIdOrderBySortOrderAscIdAsc(noticeId);
    for (NoticeAttachment att : attachments) {
      if (att.getStoredName() != null && !att.getStoredName().isBlank()) {
        try {
          fileStorage.delete(noticeBucket, buildStoragePath(noticeId, att.getStoredName()));
        } catch (IOException e) {
          log.warn(
              "Failed to delete storage object for notice {} attachment {}: {}",
              noticeId,
              att.getId(),
              e.getMessage());
        }
      }
    }
    noticeAttachmentRepository.deleteAll(attachments);
    noticeRepository.delete(notice);
  }

  private void validateTitleContent(String title, String content) {
    if (title == null || title.trim().isEmpty()) {
      throw new IllegalArgumentException("제목을 입력해주세요.");
    }
    if (title.length() > 255) {
      throw new IllegalArgumentException("제목은 255자 이하여야 합니다.");
    }
    if (content == null || content.trim().isEmpty()) {
      throw new IllegalArgumentException("내용을 입력해주세요.");
    }
  }

  // ================= 첨부 =================

  @Transactional(readOnly = true)
  public List<NoticeAttachment> findAttachments(Long noticeId) {
    return noticeAttachmentRepository.findByNoticeIdOrderBySortOrderAscIdAsc(noticeId);
  }

  /**
   * 첨부 업로드. 크기·확장자·Content-Type 검증 후 FileStorage 로 저장. NoticeAttachment.data 는 legacy 호환용으로 null
   * (F-notice-attachment 다운로드 endpoint 는 data 컬럼을 읽으므로 A-track prod 이관 시 storage 스트리밍 fallback 필요).
   * 이번 티켓은 admin CRUD + storage 인프라까지 완결하고, 다운로드 재라우팅은 후속 티켓.
   *
   * <p>학습 단계 임시 조치: LocalFileStorage 는 파일시스템에 저장 + data 컬럼도 함께 저장하여 기존 다운로드 endpoint 를 무손상 유지. 5MB
   * 이하 정책이라 DB 부담은 이번 스코프에서 감내.
   */
  @Transactional
  public NoticeAttachment uploadAttachment(Long noticeId, MultipartFile file, User currentUser)
      throws IOException {
    Notice notice = findById(noticeId);
    assertCanEdit(notice, currentUser);

    validateUploadedFile(file);

    String originalName = file.getOriginalFilename();
    if (originalName == null || originalName.isBlank()) originalName = "unnamed";
    String ext = NoticeService.extensionOf(originalName);
    String storedName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
    String path = buildStoragePath(noticeId, storedName);

    StoredFile stored = fileStorage.upload(noticeBucket, path, file);
    log.info(
        "Uploaded attachment for notice {}: originalName={}, storedName={}, size={}",
        noticeId,
        originalName,
        storedName,
        stored.size());

    int nextSort =
        noticeAttachmentRepository.findByNoticeIdOrderBySortOrderAscIdAsc(noticeId).stream()
                .mapToInt(NoticeAttachment::getSortOrder)
                .max()
                .orElse(-1)
            + 1;

    // legacy 호환: 기존 사용자 다운로드 endpoint 가 data 컬럼을 읽으므로 함께 저장 (5MB 상한).
    byte[] bytes = file.getBytes();

    NoticeAttachment attachment =
        NoticeAttachment.builder()
            .notice(notice)
            .fileName(originalName)
            .storedName(storedName)
            .fileSize(stored.size())
            .contentType(file.getContentType())
            .sortOrder(nextSort)
            .data(bytes)
            .build();
    return noticeAttachmentRepository.save(attachment);
  }

  @Transactional
  public void deleteAttachment(Long noticeId, Long attachmentId, User currentUser)
      throws IOException {
    Notice notice = findById(noticeId);
    assertCanEdit(notice, currentUser);
    NoticeAttachment attachment =
        noticeAttachmentRepository
            .findByIdAndNoticeId(attachmentId, noticeId)
            .orElseThrow(() -> new IllegalArgumentException("첨부파일을 찾을 수 없어요."));
    if (attachment.getStoredName() != null && !attachment.getStoredName().isBlank()) {
      fileStorage.delete(noticeBucket, buildStoragePath(noticeId, attachment.getStoredName()));
    }
    noticeAttachmentRepository.delete(attachment);
  }

  /** Qn-2 · Qn-3 · Qn-4 검증. */
  void validateUploadedFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("파일이 비어 있어요.");
    }
    if (file.getSize() > NoticeService.MAX_ATTACHMENT_SIZE_BYTES) {
      throw new IllegalArgumentException("파일 크기는 5MB 이하여야 해요.");
    }
    String ext = NoticeService.extensionOf(file.getOriginalFilename());
    if (!NoticeService.ALLOWED_EXTENSIONS.contains(ext)) {
      throw new IllegalArgumentException("허용되지 않는 파일 형식이에요. pdf, hwp, docx, xlsx 만 업로드할 수 있어요.");
    }
    // Qn-4 A: Content-Type 헤더 매칭.
    String contentType = file.getContentType();
    Set<String> allowed = ALLOWED_MIME.getOrDefault(ext, Set.of());
    if (contentType != null && !allowed.isEmpty() && !allowed.contains(contentType.toLowerCase())) {
      throw new IllegalArgumentException(
          "파일 형식과 실제 Content-Type 이 일치하지 않아요. (" + ext + " / " + contentType + ")");
    }
  }

  String buildStoragePath(Long noticeId, String storedName) {
    return noticeId + "/" + storedName;
  }
}

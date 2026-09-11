package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.common.storage.FileStorage;
import io.github.sihyuuun.youthmoa.common.storage.StoredFile;
import io.github.sihyuuun.youthmoa.notice.NoticeService;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramAttachment;
import io.github.sihyuuun.youthmoa.program.ProgramAttachmentRepository;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * A3-2 admin-program-form-integration (2026-09-11 · Qn-C A · NoticeAttachment 승계): 프로그램 첨부 CRUD.
 *
 * <ul>
 *   <li>Qn-C-ext: pdf · hwp · docx · xlsx (NoticeService.ALLOWED_EXTENSIONS 재사용)
 *   <li>Qn-C-size: 5MB (NoticeService.MAX_ATTACHMENT_SIZE_BYTES 재사용)
 *   <li>Qn-C-max : 10개
 *   <li>Qn-C-dup : storedName UUID
 *   <li>Qn-Δ-C-bucket A: {@code program-attachments}
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProgramAttachmentService {

  public static final int MAX_ATTACHMENTS_PER_PROGRAM = 10;

  /** NoticeService 와 동일한 MIME 매핑 (한 곳에 상수 유지가 이상적이나 이번 티켓은 재활용에 그침). */
  public static final Map<String, Set<String>> ALLOWED_MIME = AdminNoticeService.ALLOWED_MIME;

  private final ProgramRepository programRepository;
  private final ProgramAttachmentRepository programAttachmentRepository;
  private final FileStorage fileStorage;

  @Value("${youthmoa.storage.supabase.program-attachment-bucket:program-attachments}")
  private String attachmentBucket;

  @Transactional(readOnly = true)
  public List<ProgramAttachment> findAttachments(Long programId) {
    return programAttachmentRepository.findByProgramIdOrderBySortOrderAscIdAsc(programId);
  }

  /**
   * 첨부 업로드. NoticeAttachment 패턴 승계 — legacy 호환용 data bytea 컬럼 동시 저장. FileStorage 실 저장 성공 후 DB row
   * 생성.
   */
  @Transactional
  public ProgramAttachment uploadAttachment(Long programId, MultipartFile file) throws IOException {
    Program program =
        programRepository
            .findById(programId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로그램이에요: " + programId));
    validateUploadedFile(file);

    List<ProgramAttachment> existing =
        programAttachmentRepository.findByProgramIdOrderBySortOrderAscIdAsc(programId);
    if (existing.size() >= MAX_ATTACHMENTS_PER_PROGRAM) {
      throw new IllegalArgumentException(
          "첨부는 프로그램당 " + MAX_ATTACHMENTS_PER_PROGRAM + "개까지만 등록할 수 있어요.");
    }

    String originalName = file.getOriginalFilename();
    if (originalName == null || originalName.isBlank()) originalName = "unnamed";
    String ext = NoticeService.extensionOf(originalName);
    String storedName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
    String path = programId + "/" + storedName;

    StoredFile stored = fileStorage.upload(attachmentBucket, path, file);
    log.info(
        "[admin-program-attachment] uploaded programId={} originalName={} storedName={} size={}",
        programId,
        originalName,
        storedName,
        stored.size());

    int nextSort = existing.stream().mapToInt(ProgramAttachment::getSortOrder).max().orElse(-1) + 1;
    byte[] bytes = file.getBytes();

    ProgramAttachment attachment =
        ProgramAttachment.builder()
            .program(program)
            .fileName(originalName)
            .storedName(storedName)
            .fileSize(stored.size())
            .contentType(file.getContentType())
            .sortOrder(nextSort)
            .data(bytes)
            .build();
    return programAttachmentRepository.save(attachment);
  }

  @Transactional
  public void deleteAttachment(Long programId, Long attachmentId) throws IOException {
    ProgramAttachment attachment =
        programAttachmentRepository
            .findByIdAndProgramId(attachmentId, programId)
            .orElseThrow(() -> new IllegalArgumentException("첨부파일을 찾을 수 없어요."));
    if (attachment.getStoredName() != null && !attachment.getStoredName().isBlank()) {
      fileStorage.delete(attachmentBucket, programId + "/" + attachment.getStoredName());
    }
    programAttachmentRepository.delete(attachment);
    log.info(
        "[admin-program-attachment] deleted programId={} attachmentId={}", programId, attachmentId);
  }

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
    String contentType = file.getContentType();
    Set<String> allowed = ALLOWED_MIME.getOrDefault(ext, Set.of());
    if (contentType != null && !allowed.isEmpty() && !allowed.contains(contentType.toLowerCase())) {
      throw new IllegalArgumentException(
          "파일 형식과 실제 Content-Type 이 일치하지 않아요. (" + ext + " / " + contentType + ")");
    }
  }
}

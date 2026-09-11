package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.common.storage.FileStorage;
import io.github.sihyuuun.youthmoa.common.storage.StoredFile;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * A3-2 admin-program-form-integration (2026-09-11 · Qn-D A · Qn-Δ-D-*): 프로그램 대표 이미지 업로드.
 *
 * <ul>
 *   <li>Qn-D A: 파일 업로드 시 imageUrl 자동 갱신. 기존 imageUrl (URL 텍스트) 는 파일 미제출 시 유지 (무회귀).
 *   <li>Qn-Δ-D-ext A: jpg / jpeg / png / webp
 *   <li>Qn-Δ-D-size A: 2MB 상한
 *   <li>Qn-Δ-C-bucket A: 전용 bucket {@code program-images}
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProgramImageService {

  public static final long MAX_IMAGE_SIZE_BYTES = 2L * 1024 * 1024;
  public static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
  public static final Set<String> ALLOWED_IMAGE_MIME =
      Set.of("image/jpeg", "image/png", "image/webp");

  private final FileStorage fileStorage;
  private final ProgramRepository programRepository;

  @Value("${youthmoa.storage.supabase.program-image-bucket:program-images}")
  private String imageBucket;

  /**
   * multipart 이미지 업로드. 파일이 null 이거나 비어 있으면 기존 imageUrl 유지 (무동작). 실 저장 성공 시 Program.imageUrl 을
   * bucket/path 로 갱신.
   */
  @Transactional
  public String uploadImageIfPresent(Long programId, MultipartFile image) throws IOException {
    if (image == null || image.isEmpty()) return null;
    validateImage(image);
    Program program =
        programRepository
            .findById(programId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로그램이에요: " + programId));

    String originalName = image.getOriginalFilename();
    if (originalName == null || originalName.isBlank()) originalName = "image";
    String ext = extensionOf(originalName);
    String storedName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
    String path = programId + "/" + storedName;

    StoredFile stored = fileStorage.upload(imageBucket, path, image);
    // FileStorage 는 URL 을 반환하지 않으므로 논리적 참조 문자열을 저장 (bucket/path).
    // 실제 렌더는 컨트롤러 다운로드 endpoint 를 두거나 후행에서 처리. 이번 티켓은 저장 값 자체 유지.
    String reference = "/storage/" + imageBucket + "/" + path;
    program.updateImageUrl(reference);
    log.info(
        "[admin-program-image] uploaded programId={} originalName={} storedName={} size={} ref={}",
        programId,
        originalName,
        storedName,
        stored.size(),
        reference);
    return reference;
  }

  void validateImage(MultipartFile image) {
    if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
      throw new IllegalArgumentException("이미지 크기는 2MB 이하여야 해요.");
    }
    String ext = extensionOf(image.getOriginalFilename());
    if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
      throw new IllegalArgumentException("허용되지 않는 이미지 형식이에요. jpg, jpeg, png, webp 만 업로드할 수 있어요.");
    }
    String contentType = image.getContentType();
    if (contentType != null && !ALLOWED_IMAGE_MIME.contains(contentType.toLowerCase())) {
      throw new IllegalArgumentException("이미지 Content-Type 이 허용되지 않아요: " + contentType);
    }
  }

  static String extensionOf(String fileName) {
    if (fileName == null) return "";
    int dot = fileName.lastIndexOf('.');
    if (dot < 0 || dot == fileName.length() - 1) return "";
    return fileName.substring(dot + 1).toLowerCase();
  }
}

package io.github.sihyuuun.youthmoa.common.storage;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

/**
 * A-admin-notice-attachment (2026-09-03 · Qn-1 C): 파일 저장소 추상화.
 *
 * <p>구현체:
 *
 * <ul>
 *   <li>{@link LocalFileStorage} — {@code @Profile("!prod")} 로컬 파일시스템 저장 (dev/e2e/local/test-guard)
 *   <li>{@link SupabaseFileStorage} — {@code @Profile("prod")} Supabase Storage REST + OkHttp
 * </ul>
 *
 * <p>{@code bucket} 은 논리 이름 (예: "notice-attachments"). {@code path} 는 bucket 내부 상대 경로.
 */
public interface FileStorage {

  /** multipart 업로드. */
  StoredFile upload(String bucket, String path, MultipartFile file) throws IOException;

  /** 다운로드 스트림. 호출측이 닫아야 함. */
  InputStream download(String bucket, String path) throws IOException;

  /** 삭제. 존재하지 않는 파일 삭제도 no-op 로 처리 (idempotent). */
  void delete(String bucket, String path) throws IOException;

  /** 존재 여부 확인. */
  boolean exists(String bucket, String path);
}

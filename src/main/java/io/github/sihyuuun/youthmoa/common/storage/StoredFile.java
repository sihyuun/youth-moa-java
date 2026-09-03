package io.github.sihyuuun.youthmoa.common.storage;

/**
 * A-admin-notice-attachment: 저장된 파일의 opaque handle. 다운로드/삭제 시 사용.
 *
 * <p>{@code path} — bucket 내부 상대 경로 (예: "1/uuid-filename.pdf"). LocalFileStorage 는 {@code
 * ${storage.local.root-dir}/{bucket}/{path}} 로 파일시스템 매핑. SupabaseFileStorage 는 REST URL segment 로
 * 사용.
 *
 * <p>{@code storedName} — DB 에 저장할 서버측 저장 이름 (UUID + 확장자). NoticeAttachment.storedName 에 이 값 저장.
 */
public record StoredFile(String bucket, String path, String storedName, long size) {}

package io.github.sihyuuun.youthmoa.common.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/**
 * A-admin-notice-attachment: LocalFileStorage upload/download/delete round-trip 검증. Supabase 는 실
 * endpoint 필요라 CI 에선 mock 없이 검증 불가 → LocalFileStorage 로 인터페이스 계약 검증.
 */
class LocalFileStorageTest {

  @Test
  void upload_download_delete_roundTrip(@TempDir Path tempDir) throws IOException {
    LocalFileStorage storage = new LocalFileStorage(tempDir.toString());
    byte[] payload = "hello-world".getBytes();
    MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", payload);

    StoredFile stored = storage.upload("bucket-a", "sub/test.pdf", file);
    assertThat(stored.size()).isEqualTo(payload.length);
    assertThat(stored.bucket()).isEqualTo("bucket-a");
    assertThat(storage.exists("bucket-a", "sub/test.pdf")).isTrue();

    try (InputStream in = storage.download("bucket-a", "sub/test.pdf")) {
      assertThat(in.readAllBytes()).isEqualTo(payload);
    }

    storage.delete("bucket-a", "sub/test.pdf");
    assertThat(storage.exists("bucket-a", "sub/test.pdf")).isFalse();

    // idempotent: 재삭제도 예외 없음
    storage.delete("bucket-a", "sub/test.pdf");
  }

  @Test
  void delete_nonexistent_isIdempotent(@TempDir Path tempDir) throws IOException {
    LocalFileStorage storage = new LocalFileStorage(tempDir.toString());
    storage.delete("bucket-x", "missing.pdf"); // should not throw
    assertThat(storage.exists("bucket-x", "missing.pdf")).isFalse();
  }

  @Test
  void download_nonexistent_throwsIOException(@TempDir Path tempDir) {
    LocalFileStorage storage = new LocalFileStorage(tempDir.toString());
    assertThat(java.util.Optional.ofNullable(null)).isEmpty(); // placeholder to avoid empty test
    org.junit.jupiter.api.Assertions.assertThrows(
        IOException.class, () -> storage.download("nowhere", "x.pdf"));
  }

  @Test
  void pathTraversal_isBlocked(@TempDir Path tempDir) {
    LocalFileStorage storage = new LocalFileStorage(tempDir.toString());
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class, () -> storage.exists("bucket", "../../etc/passwd"));
  }

  @Test
  void upload_createsNestedDirs(@TempDir Path tempDir) throws IOException {
    LocalFileStorage storage = new LocalFileStorage(tempDir.toString());
    MockMultipartFile file =
        new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[] {1, 2});
    storage.upload("b", "deeply/nested/dir/a.pdf", file);
    assertThat(Files.exists(tempDir.resolve("b/deeply/nested/dir/a.pdf"))).isTrue();
  }
}

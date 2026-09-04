package io.github.sihyuuun.youthmoa.common.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * A-admin-notice-attachment (Qn-1 C): 로컬 파일시스템 저장 구현. dev/e2e/local/test-guard 프로파일에서 활성.
 *
 * <p>저장 경로: {@code ${youthmoa.storage.local.root-dir}/{bucket}/{path}}. bucket · path 는 controller
 * 계층에서 결정 (예: bucket=notice-attachments, path=1/uuid.pdf).
 */
@Slf4j
@Component
@Profile("!prod")
public class LocalFileStorage implements FileStorage {

  private final Path rootDir;

  public LocalFileStorage(@Value("${youthmoa.storage.local.root-dir:./storage}") String rootDir) {
    this.rootDir = Paths.get(rootDir).toAbsolutePath().normalize();
    try {
      Files.createDirectories(this.rootDir);
      log.info("LocalFileStorage rootDir = {}", this.rootDir);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create storage root dir: " + this.rootDir, e);
    }
  }

  @Override
  public StoredFile upload(String bucket, String path, MultipartFile file) throws IOException {
    Path target = resolvePath(bucket, path);
    Files.createDirectories(target.getParent());
    try (InputStream in = file.getInputStream()) {
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }
    long size = Files.size(target);
    log.debug("LocalFileStorage upload bucket={}, path={}, size={}", bucket, path, size);
    return new StoredFile(bucket, path, extractStoredName(path), size);
  }

  @Override
  public InputStream download(String bucket, String path) throws IOException {
    Path source = resolvePath(bucket, path);
    if (!Files.exists(source)) {
      throw new IOException("File not found: " + bucket + "/" + path);
    }
    return Files.newInputStream(source);
  }

  @Override
  public void delete(String bucket, String path) throws IOException {
    Path target = resolvePath(bucket, path);
    Files.deleteIfExists(target);
    log.debug("LocalFileStorage delete bucket={}, path={}", bucket, path);
  }

  @Override
  public boolean exists(String bucket, String path) {
    return Files.exists(resolvePath(bucket, path));
  }

  /** path traversal 방어: rootDir 밖 접근 차단. */
  private Path resolvePath(String bucket, String path) {
    Path resolved = rootDir.resolve(bucket).resolve(path).normalize();
    if (!resolved.startsWith(rootDir)) {
      throw new IllegalArgumentException("Invalid storage path (traversal detected): " + path);
    }
    return resolved;
  }

  private String extractStoredName(String path) {
    int slash = path.lastIndexOf('/');
    return slash < 0 ? path : path.substring(slash + 1);
  }
}

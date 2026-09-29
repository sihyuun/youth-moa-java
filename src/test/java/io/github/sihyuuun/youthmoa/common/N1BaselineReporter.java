package io.github.sihyuuun.youthmoa.common;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * A9-c: N+1 baseline 측정값을 JSON artifact 로 export.
 *
 * <p>{@link AdminEagerFetchN1Test} / UserEagerFetchN1Test 등 여러 테스트 클래스에서 실측한 쿼리 수와 상한을 {@code
 * build/reports/n1-baseline.json} 에 append 한다. CI 는 이 파일을 artifact 로 upload 하여 트렌드 분석에 활용한다.
 *
 * <p>사용법:
 *
 * <pre>{@code
 * N1BaselineReporter.record(
 *     getClass().getName(), "adminApplicationService_list", 3L, 6L);
 * }</pre>
 *
 * <p>동시성: JVM lock + FileChannel lock 이중 보호로 병렬 test class 실행 시 append race 방지.
 *
 * <p>스키마:
 *
 * <pre>{@code
 * {
 *   "generatedAt": "2026-09-29T12:34:56Z",
 *   "commit": "<sha or 'unknown'>",
 *   "entries": [
 *     { "class": "...", "method": "...", "queries": 3, "limit": 6, "recordedAt": "..." }
 *   ]
 * }
 * }</pre>
 */
public final class N1BaselineReporter {

  private static final Object LOCK = new Object();
  private static final Path REPORT_PATH = Path.of("build/reports/n1-baseline.json");
  private static final ObjectMapper MAPPER =
      JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

  private N1BaselineReporter() {}

  /**
   * 한 TC 의 baseline 결과를 파일에 append.
   *
   * @param className 테스트 클래스 FQN
   * @param methodName 테스트 메서드명
   * @param queries 실측 쿼리 수
   * @param limit assert 상한
   */
  public static void record(String className, String methodName, long queries, long limit) {
    synchronized (LOCK) {
      try {
        Files.createDirectories(REPORT_PATH.getParent());
        try (FileChannel channel =
                FileChannel.open(
                    REPORT_PATH,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE);
            FileLock ignored = channel.lock()) {

          ObjectNode root = readOrInit(channel);
          ArrayNode entries = (ArrayNode) root.get("entries");

          ObjectNode entry = MAPPER.createObjectNode();
          entry.put("class", className);
          entry.put("method", methodName);
          entry.put("queries", queries);
          entry.put("limit", limit);
          entry.put("recordedAt", Instant.now().toString());
          entries.add(entry);

          // 갱신된 generatedAt (마지막 기록 시각)
          root.put("generatedAt", Instant.now().toString());

          byte[] bytes = MAPPER.writeValueAsBytes(root);
          channel.truncate(0);
          channel.position(0);
          channel.write(java.nio.ByteBuffer.wrap(bytes));
        }
      } catch (IOException e) {
        // baseline artifact 실패가 test 를 fail 시키지는 않도록 stderr 로만 노출.
        System.err.println("[N1BaselineReporter] failed to append: " + e.getMessage());
      }
    }
  }

  private static ObjectNode readOrInit(FileChannel channel) throws IOException {
    long size = channel.size();
    if (size == 0) {
      ObjectNode root = MAPPER.createObjectNode();
      root.put("generatedAt", Instant.now().toString());
      root.put("commit", resolveCommit());
      root.set("entries", MAPPER.createArrayNode());
      return root;
    }
    java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate((int) size);
    channel.position(0);
    channel.read(buf);
    String json = new String(buf.array(), StandardCharsets.UTF_8);
    try {
      return (ObjectNode) MAPPER.readTree(json);
    } catch (RuntimeException e) {
      // 파손된 파일은 새로 시작 (Jackson 3 는 unchecked JacksonException 을 던진다)
      ObjectNode root = MAPPER.createObjectNode();
      root.put("generatedAt", Instant.now().toString());
      root.put("commit", resolveCommit());
      root.set("entries", MAPPER.createArrayNode());
      return root;
    }
  }

  private static String resolveCommit() {
    String sha = System.getenv("GITHUB_SHA");
    if (sha != null && !sha.isBlank()) return sha;
    try {
      Process p = new ProcessBuilder("git", "rev-parse", "HEAD").redirectErrorStream(true).start();
      byte[] out = p.getInputStream().readAllBytes();
      p.waitFor();
      String s = new String(out, StandardCharsets.UTF_8).trim();
      return s.isEmpty() ? "unknown" : s;
    } catch (Exception e) {
      return "unknown";
    }
  }
}

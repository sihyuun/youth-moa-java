package io.github.sihyuuun.youthmoa.common.storage;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * A-admin-notice-attachment (Qn-1 C, Qn-6 파생 B): Supabase Storage 구현체. prod 프로파일에서만 활성.
 *
 * <p>REST 사양:
 *
 * <ul>
 *   <li>Upload: {@code POST ${SUPABASE_URL}/storage/v1/object/{bucket}/{path}}
 *   <li>Download: {@code GET ${SUPABASE_URL}/storage/v1/object/{bucket}/{path}}
 *   <li>Delete: {@code DELETE ${SUPABASE_URL}/storage/v1/object/{bucket}/{path}}
 *   <li>Bucket 생성: {@code POST ${SUPABASE_URL}/storage/v1/bucket}
 * </ul>
 *
 * <p>Auth: {@code Authorization: Bearer ${SUPABASE_SERVICE_ROLE_KEY}} — 서버측 secret. bucket 은
 * private 유지 + 서버 프록시 다운로드 (기존 {@code /notices/{nid}/attachments/{aid}/download} 재사용).
 *
 * <p>부팅 시 bucket idempotent 생성 시도.
 */
@Slf4j
@Component
@Profile("prod")
public class SupabaseFileStorage implements FileStorage {

  private final String supabaseUrl;
  private final String serviceRoleKey;
  private final String noticeBucket;
  private final OkHttpClient client;

  public SupabaseFileStorage(
      @Value("${youthmoa.storage.supabase.url:}") String supabaseUrl,
      @Value("${youthmoa.storage.supabase.service-role-key:}") String serviceRoleKey,
      @Value("${youthmoa.storage.supabase.notice-bucket:notice-attachments}") String noticeBucket) {
    this.supabaseUrl = supabaseUrl.replaceAll("/$", "");
    this.serviceRoleKey = serviceRoleKey;
    this.noticeBucket = noticeBucket;
    this.client =
        new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
  }

  @PostConstruct
  void initBucket() {
    if (supabaseUrl.isBlank() || serviceRoleKey.isBlank()) {
      log.warn("SupabaseFileStorage: SUPABASE_URL or SERVICE_ROLE_KEY is empty, skip bucket init");
      return;
    }
    ensureBucket(noticeBucket);
  }

  /** bucket idempotent 생성. 존재하면 409 반환 → 무시. */
  private void ensureBucket(String bucket) {
    String body = "{\"id\":\"" + bucket + "\",\"name\":\"" + bucket + "\",\"public\":false}";
    Request request =
        authed(new Request.Builder().url(supabaseUrl + "/storage/v1/bucket"))
            .post(RequestBody.create(body, MediaType.parse("application/json")))
            .build();
    try (Response resp = client.newCall(request).execute()) {
      if (resp.isSuccessful()) {
        log.info("Supabase bucket '{}' created", bucket);
      } else if (resp.code() == 409) {
        log.debug("Supabase bucket '{}' already exists", bucket);
      } else {
        log.warn(
            "Supabase bucket '{}' create failed: HTTP {} {}", bucket, resp.code(), resp.message());
      }
    } catch (IOException e) {
      log.warn("Supabase bucket '{}' create failed: {}", bucket, e.getMessage());
    }
  }

  @Override
  public StoredFile upload(String bucket, String path, MultipartFile file) throws IOException {
    MediaType mediaType =
        file.getContentType() != null
            ? MediaType.parse(file.getContentType())
            : MediaType.parse("application/octet-stream");
    RequestBody body = RequestBody.create(file.getBytes(), mediaType);
    Request request = authed(new Request.Builder().url(objectUrl(bucket, path))).post(body).build();
    try (Response resp = client.newCall(request).execute()) {
      if (!resp.isSuccessful()) {
        throw new IOException(
            "Supabase upload failed: HTTP " + resp.code() + " " + safeBodyString(resp));
      }
    }
    return new StoredFile(bucket, path, extractStoredName(path), file.getSize());
  }

  @Override
  public InputStream download(String bucket, String path) throws IOException {
    Request request = authed(new Request.Builder().url(objectUrl(bucket, path))).get().build();
    Response resp = client.newCall(request).execute();
    if (!resp.isSuccessful()) {
      try (resp) {
        throw new IOException(
            "Supabase download failed: HTTP " + resp.code() + " " + safeBodyString(resp));
      }
    }
    ResponseBody body = resp.body();
    if (body == null) {
      resp.close();
      throw new IOException("Supabase download: empty body");
    }
    return body.byteStream();
  }

  @Override
  public void delete(String bucket, String path) throws IOException {
    Request request = authed(new Request.Builder().url(objectUrl(bucket, path))).delete().build();
    try (Response resp = client.newCall(request).execute()) {
      if (!resp.isSuccessful() && resp.code() != 404) {
        throw new IOException(
            "Supabase delete failed: HTTP " + resp.code() + " " + safeBodyString(resp));
      }
    }
  }

  @Override
  public boolean exists(String bucket, String path) {
    Request request = authed(new Request.Builder().url(objectUrl(bucket, path))).head().build();
    try (Response resp = client.newCall(request).execute()) {
      return resp.isSuccessful();
    } catch (IOException e) {
      return false;
    }
  }

  private String objectUrl(String bucket, String path) {
    return supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;
  }

  private Request.Builder authed(Request.Builder b) {
    return b.header("Authorization", "Bearer " + serviceRoleKey).header("apikey", serviceRoleKey);
  }

  private String safeBodyString(Response resp) {
    try {
      ResponseBody body = resp.body();
      return body != null ? body.string() : "";
    } catch (IOException e) {
      return "";
    }
  }

  private String extractStoredName(String path) {
    int slash = path.lastIndexOf('/');
    return slash < 0 ? path : path.substring(slash + 1);
  }
}

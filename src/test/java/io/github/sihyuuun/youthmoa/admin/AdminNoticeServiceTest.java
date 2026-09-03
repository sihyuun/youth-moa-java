package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.common.storage.FileStorage;
import io.github.sihyuuun.youthmoa.common.storage.StoredFile;
import io.github.sihyuuun.youthmoa.notice.Notice;
import io.github.sihyuuun.youthmoa.notice.NoticeAttachmentRepository;
import io.github.sihyuuun.youthmoa.notice.NoticeRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.io.InputStream;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * A-admin-notice-attachment: AdminNoticeService 단위 검증. RBAC · 업로드 검증(크기·확장자·MIME) 를 mock 없이 최소 stub
 * 로 검증. Service 자체 로직만 대상 (repository 실 조회는 별도 통합 테스트).
 */
class AdminNoticeServiceTest {

  private AdminNoticeService service;
  private User sysadmin;
  private User centerAdmin1;
  private User centerAdmin2;

  @BeforeEach
  void setup() throws Exception {
    // FileStorage stub (upload 시 파일 크기 반영, delete no-op)
    FileStorage storage =
        new FileStorage() {
          @Override
          public StoredFile upload(String bucket, String path, MultipartFile file) {
            return new StoredFile(bucket, path, "x", file.getSize());
          }

          @Override
          public InputStream download(String bucket, String path) {
            return InputStream.nullInputStream();
          }

          @Override
          public void delete(String bucket, String path) {}

          @Override
          public boolean exists(String bucket, String path) {
            return false;
          }
        };
    service = new AdminNoticeService(noOpNoticeRepo(), noOpAttachmentRepo(), storage);

    sysadmin = buildUser(1L, "sys@t", UserRole.SYSTEM_ADMIN);
    centerAdmin1 = buildUser(2L, "c1@t", UserRole.CENTER_ADMIN);
    centerAdmin2 = buildUser(3L, "c2@t", UserRole.CENTER_ADMIN);
  }

  // ================= RBAC =================

  @Test
  void canEdit_sysadmin_always() {
    Notice n = buildNotice(centerAdmin1);
    assertThat(service.canEdit(n, sysadmin)).isTrue();
  }

  @Test
  void canEdit_centerAdmin_ownNotice() {
    Notice n = buildNotice(centerAdmin1);
    assertThat(service.canEdit(n, centerAdmin1)).isTrue();
  }

  @Test
  void canEdit_centerAdmin_othersNotice_false() {
    Notice n = buildNotice(centerAdmin1);
    assertThat(service.canEdit(n, centerAdmin2)).isFalse();
  }

  @Test
  void canEdit_nullUser_false() {
    Notice n = buildNotice(sysadmin);
    assertThat(service.canEdit(n, null)).isFalse();
  }

  @Test
  void canEdit_regularUser_false() throws Exception {
    User regular = buildUser(4L, "u@t", UserRole.USER);
    Notice n = buildNotice(sysadmin);
    assertThat(service.canEdit(n, regular)).isFalse();
  }

  // ================= 업로드 검증 (Qn-2 · Qn-3 · Qn-4) =================

  @Test
  void validate_okPdf() {
    MockMultipartFile f =
        new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[] {1, 2, 3});
    service.validateUploadedFile(f);
  }

  @Test
  void validate_okHwp() {
    MockMultipartFile f =
        new MockMultipartFile("file", "doc.hwp", "application/x-hwp", new byte[] {1});
    service.validateUploadedFile(f);
  }

  @Test
  void validate_emptyFile_rejected() {
    MockMultipartFile f = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[0]);
    assertThatThrownBy(() -> service.validateUploadedFile(f))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("비어");
  }

  @Test
  void validate_sizeOver5MB_rejected() {
    byte[] big = new byte[5 * 1024 * 1024 + 1];
    MockMultipartFile f = new MockMultipartFile("file", "big.pdf", "application/pdf", big);
    assertThatThrownBy(() -> service.validateUploadedFile(f))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("5MB");
  }

  @Test
  void validate_disallowedExtension_rejected() {
    MockMultipartFile f =
        new MockMultipartFile("file", "malware.exe", "application/octet-stream", new byte[] {1});
    assertThatThrownBy(() -> service.validateUploadedFile(f))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("허용되지 않는");
  }

  @Test
  void validate_extensionOk_butMimeMismatch_rejected() {
    // 확장자 pdf 이지만 Content-Type 이 image/jpeg → 매칭 실패
    MockMultipartFile f =
        new MockMultipartFile("file", "fake.pdf", "image/jpeg", new byte[] {1, 2});
    assertThatThrownBy(() -> service.validateUploadedFile(f))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Content-Type");
  }

  @Test
  void validate_noExtension_rejected() {
    MockMultipartFile f = new MockMultipartFile("file", "noext", "application/pdf", new byte[] {1});
    assertThatThrownBy(() -> service.validateUploadedFile(f))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ================= helpers =================

  private static User buildUser(Long id, String email, UserRole role) {
    User u = User.builder().email(email).password("x").name(email).role(role).build();
    setId(u, id);
    return u;
  }

  private static Notice buildNotice(User creator) {
    Notice n =
        Notice.builder()
            .title("t")
            .content("c")
            .category(io.github.sihyuuun.youthmoa.notice.NoticeCategory.NOTICE)
            .createdBy(creator)
            .build();
    setId(n, 100L);
    return n;
  }

  private static void setId(Object entity, Long id) {
    try {
      Field f = entity.getClass().getDeclaredField("id");
      f.setAccessible(true);
      f.set(entity, id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** 최소 no-op Repository (실 사용은 없음, ctor 요구사항만 충족). */
  @SuppressWarnings("unchecked")
  private static NoticeRepository noOpNoticeRepo() {
    return (NoticeRepository)
        java.lang.reflect.Proxy.newProxyInstance(
            AdminNoticeServiceTest.class.getClassLoader(),
            new Class<?>[] {NoticeRepository.class},
            (proxy, method, args) -> {
              if (method.getReturnType().equals(java.util.Optional.class))
                return java.util.Optional.empty();
              if (method.getReturnType().equals(java.util.List.class)) return java.util.List.of();
              if (method.getReturnType().equals(long.class)) return 0L;
              if (method.getReturnType().equals(boolean.class)) return false;
              return null;
            });
  }

  @SuppressWarnings("unchecked")
  private static NoticeAttachmentRepository noOpAttachmentRepo() {
    return (NoticeAttachmentRepository)
        java.lang.reflect.Proxy.newProxyInstance(
            AdminNoticeServiceTest.class.getClassLoader(),
            new Class<?>[] {NoticeAttachmentRepository.class},
            (proxy, method, args) -> {
              if (method.getReturnType().equals(java.util.Optional.class))
                return java.util.Optional.empty();
              if (method.getReturnType().equals(java.util.List.class)) return java.util.List.of();
              return null;
            });
  }
}

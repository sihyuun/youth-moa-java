package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.program.ApprovalMode;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * A3-2 admin-program-form-integration verify fix (2026-09-11): {@code AdminProgramImageService}
 * 업로드/검증 회귀. spec §5-4 · Qn-D · Qn-Δ-D-ext (jpg/jpeg/png/webp) · Qn-Δ-D-size (2MB) 커버.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@Transactional
class AdminProgramImageServiceTest {

  @Autowired AdminProgramService adminProgramService;
  @Autowired AdminProgramImageService adminProgramImageService;
  @Autowired ProgramRepository programRepository;

  @AfterEach
  void clearAuth() {
    SecurityContextHolder.clearContext();
  }

  private Program newProgram(String title) {
    ProgramFormRequest r = new ProgramFormRequest();
    r.setTitle(title);
    r.setOrganization("e2e 센터");
    r.setContent("본문");
    r.setStartDate(LocalDate.of(2026, 10, 1));
    r.setEndDate(LocalDate.of(2026, 10, 31));
    r.setApplyStartDate(LocalDate.of(2026, 9, 1));
    r.setApplyEndDate(LocalDate.of(2026, 9, 30));
    r.setApprovalMode(ApprovalMode.MANUAL);
    r.setActive(true);
    return adminProgramService.create(r);
  }

  private MockMultipartFile jpeg(String name, byte[] bytes) {
    return new MockMultipartFile("image", name, "image/jpeg", bytes);
  }

  @Test
  void 정상_업로드_imageUrl_갱신() throws Exception {
    Program p = newProgram("img-ok");
    String ref =
        adminProgramImageService.uploadImageIfPresent(
            p.getId(), jpeg("thumb.jpg", "fake-jpeg".getBytes()));

    assertThat(ref).startsWith("/storage/");
    Program reloaded = programRepository.findById(p.getId()).orElseThrow();
    assertThat(reloaded.getImageUrl()).isEqualTo(ref);
  }

  @Test
  void null_파일_no_op() throws Exception {
    Program p = newProgram("img-null");
    String beforeUrl = p.getImageUrl();
    String ref = adminProgramImageService.uploadImageIfPresent(p.getId(), null);

    assertThat(ref).isNull();
    Program reloaded = programRepository.findById(p.getId()).orElseThrow();
    assertThat(reloaded.getImageUrl()).isEqualTo(beforeUrl);
  }

  @Test
  void 빈_파일_no_op() throws Exception {
    Program p = newProgram("img-empty");
    MockMultipartFile empty =
        new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]);
    String ref = adminProgramImageService.uploadImageIfPresent(p.getId(), empty);
    assertThat(ref).isNull();
  }

  @Test
  void 크기_2MB_초과_400() {
    Program p = newProgram("img-big");
    byte[] big = new byte[(int) (2L * 1024 * 1024 + 1)];
    MockMultipartFile huge = new MockMultipartFile("image", "big.jpg", "image/jpeg", big);
    assertThatThrownBy(() -> adminProgramImageService.uploadImageIfPresent(p.getId(), huge))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("2MB");
  }

  @Test
  void 확장자_허용_외_400() {
    Program p = newProgram("img-ext");
    MockMultipartFile gif =
        new MockMultipartFile("image", "anim.gif", "image/gif", new byte[] {1, 2, 3});
    assertThatThrownBy(() -> adminProgramImageService.uploadImageIfPresent(p.getId(), gif))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("허용되지 않는 이미지 형식");
  }

  @Test
  void ContentType_불일치_400() {
    Program p = newProgram("img-ct");
    // 파일명은 .jpg 이지만 Content-Type 이 image/gif → MIME 불일치
    MockMultipartFile mismatched =
        new MockMultipartFile("image", "photo.jpg", "image/gif", new byte[] {1, 2, 3});
    assertThatThrownBy(() -> adminProgramImageService.uploadImageIfPresent(p.getId(), mismatched))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Content-Type");
  }

  @Test
  void 없는_programId_400() {
    MockMultipartFile ok = jpeg("ok.jpg", "abc".getBytes());
    assertThatThrownBy(() -> adminProgramImageService.uploadImageIfPresent(9_999_999L, ok))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("존재하지 않는 프로그램");
  }

  @Test
  void png_webp_확장자_허용() throws Exception {
    Program p = newProgram("img-multi");
    MockMultipartFile png =
        new MockMultipartFile("image", "shot.png", "image/png", "png".getBytes());
    String ref1 = adminProgramImageService.uploadImageIfPresent(p.getId(), png);
    assertThat(ref1).isNotNull();

    MockMultipartFile webp =
        new MockMultipartFile("image", "next.webp", "image/webp", "wp".getBytes());
    String ref2 = adminProgramImageService.uploadImageIfPresent(p.getId(), webp);
    assertThat(ref2).isNotNull();
  }
}

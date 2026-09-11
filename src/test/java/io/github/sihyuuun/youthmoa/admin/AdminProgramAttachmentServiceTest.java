package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.program.ApprovalMode;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramAttachment;
import io.github.sihyuuun.youthmoa.program.ProgramAttachmentRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * A3-2 admin-program-form-integration verify fix (2026-09-11): {@code
 * AdminProgramAttachmentService} 업로드/삭제/검증 회귀. spec §5-2 · Qn-C 상수 (pdf/hwp/docx/xlsx, 5MB, 10개)
 * 커버.
 *
 * <p>실 파일 저장은 e2e 프로파일의 LocalFileStorage 로 임시 디렉토리에 기록됨.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@Transactional
class AdminProgramAttachmentServiceTest {

  @Autowired AdminProgramService adminProgramService;
  @Autowired AdminProgramAttachmentService adminProgramAttachmentService;
  @Autowired ProgramAttachmentRepository programAttachmentRepository;

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

  private MockMultipartFile pdf(String name, byte[] bytes) {
    return new MockMultipartFile("file", name, "application/pdf", bytes);
  }

  @Test
  void 정상_업로드_row_생성_되고_storedName_UUID_확장자() throws Exception {
    Program p = newProgram("att-ok");
    byte[] payload = "hello".getBytes();
    ProgramAttachment saved =
        adminProgramAttachmentService.uploadAttachment(p.getId(), pdf("guide.pdf", payload));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getFileName()).isEqualTo("guide.pdf");
    assertThat(saved.getStoredName()).endsWith(".pdf");
    assertThat(saved.getFileSize()).isEqualTo(payload.length);
    assertThat(saved.getSortOrder()).isEqualTo(0);
  }

  @Test
  void 여러_개_업로드_시_sortOrder_증가() throws Exception {
    Program p = newProgram("att-sort");
    adminProgramAttachmentService.uploadAttachment(p.getId(), pdf("a.pdf", new byte[] {1}));
    adminProgramAttachmentService.uploadAttachment(p.getId(), pdf("b.pdf", new byte[] {2}));
    adminProgramAttachmentService.uploadAttachment(p.getId(), pdf("c.pdf", new byte[] {3}));

    List<ProgramAttachment> list =
        programAttachmentRepository.findByProgramIdOrderBySortOrderAscIdAsc(p.getId());
    assertThat(list).extracting(ProgramAttachment::getSortOrder).containsExactly(0, 1, 2);
  }

  @Test
  void 상한_10개_초과_400() throws Exception {
    Program p = newProgram("att-max");
    for (int i = 0; i < 10; i++) {
      adminProgramAttachmentService.uploadAttachment(
          p.getId(), pdf("file-" + i + ".pdf", new byte[] {(byte) i}));
    }
    assertThatThrownBy(
            () ->
                adminProgramAttachmentService.uploadAttachment(
                    p.getId(), pdf("over.pdf", new byte[] {99})))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("10");
  }

  @Test
  void 확장자_허용_외_400() {
    Program p = newProgram("att-ext");
    MockMultipartFile exe =
        new MockMultipartFile("file", "malware.exe", "application/octet-stream", new byte[] {1});
    assertThatThrownBy(() -> adminProgramAttachmentService.uploadAttachment(p.getId(), exe))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("허용되지 않는 파일 형식");
  }

  @Test
  void 크기_5MB_초과_400() {
    Program p = newProgram("att-size");
    byte[] big = new byte[(int) (5L * 1024 * 1024 + 1)];
    MockMultipartFile huge = new MockMultipartFile("file", "big.pdf", "application/pdf", big);
    assertThatThrownBy(() -> adminProgramAttachmentService.uploadAttachment(p.getId(), huge))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("5MB");
  }

  @Test
  void 빈_파일_400() {
    Program p = newProgram("att-empty");
    MockMultipartFile empty =
        new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
    assertThatThrownBy(() -> adminProgramAttachmentService.uploadAttachment(p.getId(), empty))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("비어");
  }

  @Test
  void 삭제_row_사라짐() throws Exception {
    Program p = newProgram("att-del");
    ProgramAttachment saved =
        adminProgramAttachmentService.uploadAttachment(
            p.getId(), pdf("bye.pdf", new byte[] {1, 2}));

    adminProgramAttachmentService.deleteAttachment(p.getId(), saved.getId());

    assertThat(programAttachmentRepository.findById(saved.getId())).isEmpty();
  }

  @Test
  void 없는_id_삭제_400() {
    Program p = newProgram("att-del-miss");
    assertThatThrownBy(() -> adminProgramAttachmentService.deleteAttachment(p.getId(), 9_999_999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("첨부파일");
  }
}

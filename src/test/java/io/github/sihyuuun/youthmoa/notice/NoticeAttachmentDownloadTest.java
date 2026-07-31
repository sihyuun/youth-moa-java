package io.github.sihyuuun.youthmoa.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * F-notice-attachment 정책 강제 테스트 (spec §5).
 *
 * <p>DataInitializer 가 시드한 4건 첨부파일 중 첫 번째만 실 바이트 (sample.pdf 로드) 를 보유. 나머지 3건은 legacy 메타 (data
 * null) 로 다운로드 시 404 를 반환해야 함.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class NoticeAttachmentDownloadTest {

  @Autowired MockMvc mockMvc;
  @Autowired NoticeRepository noticeRepository;
  @Autowired NoticeAttachmentRepository noticeAttachmentRepository;

  private Long noticeIdWithRealFile;
  private Long attachmentIdWithData;
  private Long attachmentIdLegacyNull;
  private Long noticeIdOther;

  @BeforeEach
  void findSeededIds() {
    // 시드 순서상 첫 번째 첨부파일이 실 바이트 보유.
    List<NoticeAttachment> all = noticeAttachmentRepository.findAll();
    NoticeAttachment withData =
        all.stream()
            .filter(a -> a.getData() != null && a.getData().length > 0)
            .findFirst()
            .orElseThrow();
    NoticeAttachment legacy =
        all.stream()
            .filter(a -> a.getData() == null || a.getData().length == 0)
            .findFirst()
            .orElseThrow();
    attachmentIdWithData = withData.getId();
    attachmentIdLegacyNull = legacy.getId();
    noticeIdWithRealFile = withData.getNotice().getId();
    // 다른 공지 하나 (첫 번째 공지가 아닌)
    noticeIdOther =
        noticeRepository.findAll().stream()
            .map(Notice::getId)
            .filter(id -> !id.equals(noticeIdWithRealFile))
            .findFirst()
            .orElseThrow();
  }

  @Test
  @DisplayName("실 바이트 있는 첨부파일 → 200 + Content-Disposition + Content-Type + 바이트 무결성")
  void download_success() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get(
                    "/notices/{noticeId}/attachments/{attachmentId}/download",
                    noticeIdWithRealFile,
                    attachmentIdWithData))
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string("Content-Type", org.hamcrest.Matchers.startsWith("application/pdf")))
            .andExpect(header().exists("Content-Disposition"))
            .andReturn();

    byte[] body = result.getResponse().getContentAsByteArray();
    NoticeAttachment saved =
        noticeAttachmentRepository.findById(attachmentIdWithData).orElseThrow();
    assertThat(body).isEqualTo(saved.getData());

    String disposition = result.getResponse().getHeader("Content-Disposition");
    assertThat(disposition).startsWith("attachment;");
    assertThat(disposition).contains("filename*=UTF-8''");
    // 한글 파일명 URL 인코딩 결과 (% 로 시작하는 인코딩 시퀀스 포함)
    assertThat(disposition).containsPattern("%[0-9A-F]{2}");
  }

  @Test
  @DisplayName("Legacy 첨부 (data null) → 404")
  void download_legacyNull_returns404() throws Exception {
    NoticeAttachment legacy =
        noticeAttachmentRepository.findById(attachmentIdLegacyNull).orElseThrow();
    mockMvc
        .perform(
            get(
                "/notices/{noticeId}/attachments/{attachmentId}/download",
                legacy.getNotice().getId(),
                attachmentIdLegacyNull))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("다른 공지 소속으로 접근 시 404 (URL 조작 방어)")
  void download_wrongNotice_returns404() throws Exception {
    mockMvc
        .perform(
            get(
                "/notices/{noticeId}/attachments/{attachmentId}/download",
                noticeIdOther,
                attachmentIdWithData))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("존재하지 않는 attachmentId → 404")
  void download_missingAttachment_returns404() throws Exception {
    mockMvc
        .perform(
            get(
                "/notices/{noticeId}/attachments/{attachmentId}/download",
                noticeIdWithRealFile,
                999_999_999L))
        .andExpect(status().isNotFound());
  }
}

package io.github.sihyuuun.youthmoa.notice;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * F-notice-attachment: NoticeAttachmentRepository 매핑 + findByIdAndNoticeId 경로 검증.
 *
 * <p>실 바이트 저장·조회 round-trip 을 확인해 @Lob byte[] 매핑이 유효한지 방어한다.
 *
 * <p>A-admin-notice-attachment (2026-09-03): Notice.createdBy 필수화 이후 시드 User 필요.
 */
@DataJpaTest
@AutoConfigureTestDatabase
@Import(JpaConfig.class)
class NoticeAttachmentRepositoryTest {

  @Autowired NoticeAttachmentRepository noticeAttachmentRepository;
  @Autowired NoticeRepository noticeRepository;
  @Autowired UserRepository userRepository;

  private User author;

  @BeforeEach
  void setup() {
    author =
        userRepository.save(
            User.builder()
                .email("author-att@test.local")
                .password("x")
                .name("작성자")
                .role(UserRole.SYSTEM_ADMIN)
                .build());
  }

  @Test
  void save_and_load_dataBytes_roundTrip() {
    Notice notice =
        noticeRepository.save(
            Notice.builder()
                .title("t")
                .content("c")
                .category(NoticeCategory.NOTICE)
                .createdBy(author)
                .build());
    byte[] payload = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
    NoticeAttachment saved =
        noticeAttachmentRepository.save(
            NoticeAttachment.builder()
                .notice(notice)
                .fileName("test.pdf")
                .fileSize(payload.length)
                .contentType("application/pdf")
                .sortOrder(0)
                .data(payload)
                .build());

    NoticeAttachment loaded = noticeAttachmentRepository.findById(saved.getId()).orElseThrow();
    assertThat(loaded.getData()).isEqualTo(payload);
    assertThat(loaded.getFileName()).isEqualTo("test.pdf");
  }

  @Test
  void findByIdAndNoticeId_matchesOnlyOwning() {
    Notice a =
        noticeRepository.save(
            Notice.builder()
                .title("A")
                .content("A")
                .category(NoticeCategory.NOTICE)
                .createdBy(author)
                .build());
    Notice b =
        noticeRepository.save(
            Notice.builder()
                .title("B")
                .content("B")
                .category(NoticeCategory.NOTICE)
                .createdBy(author)
                .build());
    NoticeAttachment aa =
        noticeAttachmentRepository.save(
            NoticeAttachment.builder()
                .notice(a)
                .fileName("a.pdf")
                .fileSize(1)
                .contentType("application/pdf")
                .sortOrder(0)
                .data(new byte[] {9})
                .build());

    // 소속 공지로 조회 시 present
    assertThat(noticeAttachmentRepository.findByIdAndNoticeId(aa.getId(), a.getId())).isPresent();
    // 다른 공지 id 로는 empty (URL 조작 방어)
    assertThat(noticeAttachmentRepository.findByIdAndNoticeId(aa.getId(), b.getId())).isEmpty();
  }
}

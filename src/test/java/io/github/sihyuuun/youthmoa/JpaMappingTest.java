package io.github.sihyuuun.youthmoa;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.bookmark.Bookmark;
import io.github.sihyuuun.youthmoa.bookmark.BookmarkRepository;
import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import io.github.sihyuuun.youthmoa.notice.Notice;
import io.github.sihyuuun.youthmoa.notice.NoticeRepository;
import io.github.sihyuuun.youthmoa.notification.Notification;
import io.github.sihyuuun.youthmoa.notification.NotificationRepository;
import io.github.sihyuuun.youthmoa.notification.NotificationType;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.region.Region;
import io.github.sihyuuun.youthmoa.region.RegionRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
@Import(JpaConfig.class)
class JpaMappingTest {

    @Autowired UserRepository userRepository;
    @Autowired CenterRepository centerRepository;
    @Autowired ProgramRepository programRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired BookmarkRepository bookmarkRepository;
    @Autowired NoticeRepository noticeRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired RegionRepository regionRepository;

    @Test
    void allEntitiesPersistAndAuditingWorks() {
        Center center = centerRepository.save(Center.builder()
                .name("강남 청년센터").region("서울").address("서울시 강남구").phone("02-0000-0000")
                .isFeatured(true)
                .build());

        Region region = regionRepository.save(Region.builder()
                .name("서울").isFeatured(true).build());

        User user = userRepository.save(User.builder()
                .email("user@test.com").password("hashed").name("홍길동")
                .interests(Set.of("취업", "주거"))
                .role(UserRole.USER)
                .birthDate(LocalDate.of(1995, 1, 1))
                .build());

        User admin = userRepository.save(User.builder()
                .email("admin@test.com").password("hashed").name("관리자")
                .role(UserRole.CENTER_ADMIN).center(center)
                .build());

        Program program = programRepository.save(Program.builder()
                .title("취업 부트캠프").organization("청년재단").category("취업").region("서울")
                .content("내용").requirements("자격").capacity(20).build());

        Application application = applicationRepository.save(Application.builder()
                .user(user).program(program).build());
        application.approve(admin);
        applicationRepository.flush();

        bookmarkRepository.save(Bookmark.builder().user(user).program(program).build());

        Notice notice = noticeRepository.save(Notice.builder()
                .title("공지 제목").content("본문").tag("공지").isPinned(true).build());

        notificationRepository.save(Notification.builder()
                .user(user).type(NotificationType.APPLICATION_APPROVED)
                .title("승인 알림").message("신청이 승인되었습니다.").link("/mypage/history").build());

        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(centerRepository.count()).isEqualTo(1);
        assertThat(regionRepository.count()).isEqualTo(1);
        assertThat(regionRepository.findAllByIsFeaturedTrueOrderByNameAsc()).hasSize(1);
        assertThat(centerRepository.findAllByIsFeaturedTrueOrderByNameAsc()).hasSize(1);
        assertThat(region.getName()).isEqualTo("서울");
        assertThat(programRepository.count()).isEqualTo(1);
        assertThat(applicationRepository.count()).isEqualTo(1);
        assertThat(bookmarkRepository.count()).isEqualTo(1);
        assertThat(noticeRepository.count()).isEqualTo(1);
        assertThat(notificationRepository.count()).isEqualTo(1);

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(notice.getCreatedAt()).isNotNull();

        Application saved = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(saved.getProcessedBy().getEmail()).isEqualTo("admin@test.com");
        assertThat(saved.getAppliedAt()).isNotNull();
        assertThat(saved.getProcessedAt()).isNotNull();

        long unread = notificationRepository.countByUserAndIsReadFalse(user);
        assertThat(unread).isEqualTo(1);
    }
}

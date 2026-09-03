package io.github.sihyuuun.youthmoa;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.bookmark.Bookmark;
import io.github.sihyuuun.youthmoa.bookmark.BookmarkRepository;
import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.center.CenterContent;
import io.github.sihyuuun.youthmoa.center.CenterContentRepository;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import io.github.sihyuuun.youthmoa.notice.Notice;
import io.github.sihyuuun.youthmoa.notice.NoticeRepository;
import io.github.sihyuuun.youthmoa.notification.Notification;
import io.github.sihyuuun.youthmoa.notification.NotificationRepository;
import io.github.sihyuuun.youthmoa.notification.NotificationType;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramEligibility;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.region.Region;
import io.github.sihyuuun.youthmoa.region.RegionRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase
@Import(JpaConfig.class)
class JpaMappingTest {

  @Autowired UserRepository userRepository;
  @Autowired CenterRepository centerRepository;
  @Autowired CenterContentRepository centerContentRepository;
  @Autowired ProgramRepository programRepository;
  @Autowired ApplicationRepository applicationRepository;
  @Autowired BookmarkRepository bookmarkRepository;
  @Autowired NoticeRepository noticeRepository;
  @Autowired NotificationRepository notificationRepository;
  @Autowired RegionRepository regionRepository;

  @Test
  void allEntitiesPersistAndAuditingWorks() {
    Center center =
        centerRepository.save(
            Center.builder()
                .name("강남 청년센터")
                .region("서울")
                .address("서울시 강남구")
                .phone("02-0000-0000")
                .isFeatured(true)
                .build());

    Region region = regionRepository.save(Region.builder().name("서울").isFeatured(true).build());

    User user =
        userRepository.save(
            User.builder()
                .email("user@test.com")
                .password("hashed")
                .name("홍길동")
                .interestCategories(Set.of("취업·역량", "주거"))
                .role(UserRole.USER)
                .birthDate(LocalDate.of(1995, 1, 1))
                .build());

    User admin =
        userRepository.save(
            User.builder()
                .email("admin@test.com")
                .password("hashed")
                .name("관리자")
                .role(UserRole.CENTER_ADMIN)
                .center(center)
                .build());

    Program program =
        programRepository.save(
            Program.builder()
                .title("취업 부트캠프")
                .organization("청년재단")
                .category("취업")
                .region("서울")
                .content("내용")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 39세 청년")
                        .region("서울")
                        .etc("전 회차 참석 가능자")
                        .build())
                .capacity(20)
                .build());

    Application application =
        applicationRepository.save(Application.builder().user(user).program(program).build());
    application.approve(admin);
    applicationRepository.flush();

    bookmarkRepository.save(Bookmark.builder().user(user).program(program).build());

    Notice notice =
        noticeRepository.save(
            Notice.builder()
                .title("공지 제목")
                .content("본문")
                .category(io.github.sihyuuun.youthmoa.notice.NoticeCategory.NOTICE)
                .isPinned(true)
                .createdBy(admin)
                .build());

    notificationRepository.save(
        Notification.builder()
            .user(user)
            .type(NotificationType.APPLICATION_APPROVED)
            .title("승인 알림")
            .message("신청이 승인되었습니다.")
            .link("/mypage/history")
            .build());

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

    // F4: ProgramEligibility @Embedded round-trip
    Program reloaded = programRepository.findById(program.getId()).orElseThrow();
    assertThat(reloaded.getEligibility()).isNotNull();
    assertThat(reloaded.getEligibility().getAge()).isEqualTo("만 19세 ~ 39세 청년");
    assertThat(reloaded.getEligibility().getRegion()).isEqualTo("서울");
    assertThat(reloaded.getEligibility().getEtc()).isEqualTo("전 회차 참석 가능자");
  }

  /**
   * F0h-center-desc-image (spec §9-1): description·imageUrl 은 {@link CenterContent} 로 분리됨. Center 는
   * 팩트(operatingHours 포함) 만 유지. CenterContent @OneToOne 매핑·round-trip 검증.
   */
  @Test
  void centerContentFieldsPersist() {
    Center c =
        centerRepository.save(
            Center.builder().name("테스트센터").region("수원시").operatingHours("평일 09~18").build());
    centerRepository.flush();
    Center loaded = centerRepository.findById(c.getId()).orElseThrow();
    assertThat(loaded.getOperatingHours()).isEqualTo("평일 09~18");

    // updateOperatingHours 도메인 메서드 반영 검증
    loaded.updateOperatingHours("주말 10~17");
    centerRepository.flush();
    assertThat(centerRepository.findById(c.getId()).orElseThrow().getOperatingHours())
        .isEqualTo("주말 10~17");

    // CenterContent 저장·조회·업데이트 round-trip
    CenterContent content =
        centerContentRepository.save(
            CenterContent.builder()
                .center(loaded)
                .description("설명 텍스트")
                .imageUrl("/images/centers/x.png")
                .build());
    centerContentRepository.flush();
    CenterContent foundByCenter = centerContentRepository.findByCenterId(c.getId()).orElseThrow();
    assertThat(foundByCenter.getDescription()).isEqualTo("설명 텍스트");
    assertThat(foundByCenter.getImageUrl()).isEqualTo("/images/centers/x.png");
    assertThat(foundByCenter.getCenter().getId()).isEqualTo(c.getId());

    foundByCenter.update("변경된 설명", "/images/centers/y.png");
    centerContentRepository.flush();
    CenterContent reloadedContent = centerContentRepository.findById(content.getId()).orElseThrow();
    assertThat(reloadedContent.getDescription()).isEqualTo("변경된 설명");
    assertThat(reloadedContent.getImageUrl()).isEqualTo("/images/centers/y.png");
  }

  @Autowired io.github.sihyuuun.youthmoa.common.SiteImageRepository siteImageRepository;

  /** F0e-2: SiteImage slot 은 unique 아니어야 함 (HERO_BANNER 다건 시드). */
  @Test
  void siteImage_allowsMultiplePerSlot() {
    siteImageRepository.saveAll(
        java.util.List.of(
            io.github.sihyuuun.youthmoa.common.SiteImage.builder()
                .slot("HERO_BANNER")
                .imageUrl("a.jpg")
                .sortOrder(0)
                .isActive(true)
                .build(),
            io.github.sihyuuun.youthmoa.common.SiteImage.builder()
                .slot("HERO_BANNER")
                .imageUrl("b.jpg")
                .sortOrder(1)
                .isActive(true)
                .build()));
    assertThat(siteImageRepository.findAllBySlotAndIsActiveTrueOrderBySortOrderAsc("HERO_BANNER"))
        .hasSize(2);
  }
}

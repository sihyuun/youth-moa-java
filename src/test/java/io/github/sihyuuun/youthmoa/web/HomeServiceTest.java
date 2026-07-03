package io.github.sihyuuun.youthmoa.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import io.github.sihyuuun.youthmoa.common.SiteImage;
import io.github.sihyuuun.youthmoa.common.SiteImageRepository;
import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import io.github.sihyuuun.youthmoa.notice.Notice;
import io.github.sihyuuun.youthmoa.notice.NoticeRepository;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * F0e — HomeService 통합 검증 (@DataJpaTest H2 기반).
 *
 * <p>DataInitializer 는 !test 프로파일 전용이므로 여기서 시드하지 않는다. 대신 각 테스트에서 필요한 최소 데이터만 준비.
 */
@DataJpaTest
@AutoConfigureTestDatabase
@Import({JpaConfig.class, HomeService.class})
class HomeServiceTest {

  @Autowired HomeService homeService;
  @Autowired ProgramRepository programRepository;
  @Autowired CenterRepository centerRepository;
  @Autowired ApplicationRepository applicationRepository;
  @Autowired NoticeRepository noticeRepository;
  @Autowired SiteImageRepository siteImageRepository;
  @Autowired UserRepository userRepository;

  @BeforeEach
  void setUp() {
    // 사이트 이미지 4 slot
    siteImageRepository.saveAll(
        List.of(
            SiteImage.builder()
                .slot("HERO_BANNER")
                .imageUrl("hero.jpg")
                .sortOrder(0)
                .isActive(true)
                .build(),
            SiteImage.builder()
                .slot("HOME_SPACE_1")
                .imageUrl("s1.jpg")
                .sortOrder(1)
                .isActive(true)
                .build(),
            SiteImage.builder()
                .slot("HOME_SPACE_2")
                .imageUrl("s2.jpg")
                .sortOrder(2)
                .isActive(true)
                .build(),
            SiteImage.builder()
                .slot("HOME_SPACE_3")
                .imageUrl("s3.jpg")
                .sortOrder(3)
                .isActive(true)
                .build()));
    // Notice
    noticeRepository.saveAll(
        List.of(
            Notice.builder().title("대표 공지").content("본문").tag("행사").isPinned(true).build(),
            Notice.builder().title("서브 1").content("본문").tag("공지").isPinned(false).build(),
            Notice.builder().title("서브 2").content("본문").tag("운영").isPinned(false).build(),
            Notice.builder().title("서브 3").content("본문").tag("기타").isPinned(false).build()));
    // Center
    centerRepository.save(
        Center.builder().name("C1").region("서울").address("A").phone("0").isFeatured(true).build());
    // Program
    LocalDate today = LocalDate.now();
    programRepository.saveAll(
        List.of(
            Program.builder()
                .title("P1")
                .organization("O")
                .category("취업")
                .region("서울")
                .content("c")
                .requirements("r")
                .endDate(today.plusDays(1))
                .isActive(true)
                .build(),
            Program.builder()
                .title("P2")
                .organization("O")
                .category("창업")
                .region("경기")
                .content("c")
                .requirements("r")
                .endDate(today.plusDays(10))
                .isActive(true)
                .build(),
            Program.builder()
                .title("P3")
                .organization("O")
                .category("힐링")
                .region("서울")
                .content("c")
                .requirements("r")
                .endDate(today.plusDays(20))
                .isActive(true)
                .build(),
            Program.builder()
                .title("P4-inactive")
                .organization("O")
                .category("교육")
                .region("서울")
                .content("c")
                .requirements("r")
                .endDate(today.plusDays(3))
                .isActive(false)
                .build()));
  }

  @Test
  void heroImageUrl_fetchesFromSiteImage() {
    assertThat(homeService.getHeroImageUrl()).isEqualTo("hero.jpg");
  }

  @Test
  void heroImageUrl_fallbackWhenSlotMissing() {
    siteImageRepository.deleteAll();
    assertThat(homeService.getHeroImageUrl()).isEqualTo("/images/banner_01.png");
  }

  @Test
  void quickStats_counts() {
    assertThat(homeService.countActivePrograms()).isEqualTo(3L); // P4 제외
    assertThat(homeService.countCenters()).isEqualTo(1L);
    assertThat(homeService.countTotalApplicants()).isEqualTo(0L);
  }

  @Test
  void topPrograms_isActiveAndEndDateAsc() {
    List<Program> top = homeService.findTopPrograms();
    assertThat(top).hasSize(3);
    assertThat(top.get(0).getTitle()).isEqualTo("P1");
    assertThat(top.get(1).getTitle()).isEqualTo("P2");
    assertThat(top).noneMatch(p -> "P4-inactive".equals(p.getTitle()));
  }

  @Test
  void mainNotice_returnsPinned() {
    Notice main = homeService.findMainNotice();
    assertThat(main).isNotNull();
    assertThat(main.getTitle()).isEqualTo("대표 공지");
  }

  @Test
  void subNotices_returnsTop3NonPinned() {
    List<Notice> sub = homeService.findSubNotices();
    assertThat(sub).hasSize(3);
    assertThat(sub).noneMatch(Notice::isPinned);
  }

  @Test
  void spaceImages_onlyHomeSpaceSlots() {
    List<SiteImage> spaces = homeService.findSpaceImages();
    assertThat(spaces).hasSize(3);
    assertThat(spaces).allMatch(si -> si.getSlot().startsWith("HOME_SPACE_"));
  }

  @Test
  void recommendedPrograms_interestsMatch() {
    User user =
        userRepository.save(
            User.builder()
                .email("u@test.com")
                .password("p")
                .name("테스터")
                .interests(Set.of("힐링"))
                .role(UserRole.USER)
                .build());

    List<Program> rec = homeService.findRecommendedPrograms(user.getId());
    assertThat(rec).isNotEmpty();
    // 관심분야가 "힐링" 이므로 P3 (힐링) 가 상위로 올라와야 함
    assertThat(rec.get(0).getCategory()).isEqualTo("힐링");
  }
}

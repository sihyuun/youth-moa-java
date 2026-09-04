package io.github.sihyuuun.youthmoa.common;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.bookmark.Bookmark;
import io.github.sihyuuun.youthmoa.bookmark.BookmarkRepository;
import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.center.CenterContent;
import io.github.sihyuuun.youthmoa.center.CenterContentRepository;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import io.github.sihyuuun.youthmoa.notice.Notice;
import io.github.sihyuuun.youthmoa.notice.NoticeAttachment;
import io.github.sihyuuun.youthmoa.notice.NoticeAttachmentRepository;
import io.github.sihyuuun.youthmoa.notice.NoticeCategory;
import io.github.sihyuuun.youthmoa.notice.NoticeRepository;
import io.github.sihyuuun.youthmoa.notification.Notification;
import io.github.sihyuuun.youthmoa.notification.NotificationRepository;
import io.github.sihyuuun.youthmoa.notification.NotificationType;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramEligibility;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.region.Region;
import io.github.sihyuuun.youthmoa.region.RegionRepository;
import io.github.sihyuuun.youthmoa.user.Term;
import io.github.sihyuuun.youthmoa.user.TermRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

  // P0-2: 관리자 시드 비밀번호. 환경변수로 override 가능. dev/e2e 기본값 = "Admin!234".
  //   - 운영 배포 시 반드시 환경변수로 강력한 값 주입.
  //   - BCrypt encode 후 저장되므로 재기동 시 매번 같은 값으로 유지되지 않음 (encode 는 salt 로 매번 다른 해시).
  @Value("${admin.seed.password.system:Admin!234}")
  private String adminSeedPasswordSystem;

  @Value("${admin.seed.password.center1:Admin!234}")
  private String adminSeedPasswordCenter1;

  @Value("${admin.seed.password.center2:Admin!234}")
  private String adminSeedPasswordCenter2;

  /**
   * seedNotices() 가 생성하는 공지 개수. TestFixtureController#resetNotices() 가 id > SEED_NOTICE_COUNT 인 row
   * 만 삭제하기 위해 참조. seedNotices() 를 수정하면 이 상수도 동기화한다. (E2E: sysadmin 세션으로 생성된 오염 notice 도 id 기준으로 잡기
   * 위한 정책.)
   */
  public static final int SEED_NOTICE_COUNT = 12;

  private final ProgramRepository programRepository;
  private final RegionRepository regionRepository;
  private final CenterRepository centerRepository;
  private final NoticeRepository noticeRepository;
  private final NoticeAttachmentRepository noticeAttachmentRepository;
  private final SiteImageRepository siteImageRepository;
  private final UserRepository userRepository;
  private final ApplicationRepository applicationRepository;
  private final BookmarkRepository bookmarkRepository;
  private final NotificationRepository notificationRepository;
  private final PasswordEncoder passwordEncoder;
  private final CenterCsvLoader centerCsvLoader;
  private final CenterContentCsvLoader centerContentCsvLoader;
  private final CenterContentRepository centerContentRepository;
  private final TermRepository termRepository;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    seedRegionsAndCenters();
    seedPrograms();
    seedSiteImages();
    // A-admin-notice-attachment (2026-09-03 · Qn-8 Custom): Notice.createdBy 가 NOT NULL 이므로
    // sysadmin 시드가 반드시 seedNotices 보다 먼저 실행되어야 한다. seedAdmins 를 seedNotices 앞으로 이동.
    seedAdmins();
    seedNotices();
    seedApplications();
    seedBookmarks();
    seedNotifications();
    seedTerms();
  }

  /**
   * F-signup-terms-agreement: 약관 정의 시드. 재기동 시 멱등 (count 체크). Spec §3-E 표를 그대로 반영 — PRIVACY 는
   * version=2 상태로 시작 (개정 이력 반영).
   */
  private void seedTerms() {
    if (termRepository.count() > 0) {
      log.info("Terms already seeded (count={}), skip", termRepository.count());
      return;
    }
    termRepository.save(
        Term.builder()
            .code("SERVICE")
            .title("회원가입약관")
            .contentPath("/terms")
            .required(true)
            .version(1)
            .sortOrder(1)
            .isActive(true)
            .build());
    termRepository.save(
        Term.builder()
            .code("PRIVACY")
            .title("개인정보처리방침 안내")
            .contentPath("/privacy")
            .required(true)
            .version(2)
            .sortOrder(2)
            .isActive(true)
            .build());
    log.info("Seeded 2 terms (SERVICE, PRIVACY)");
  }

  /**
   * P0-2: 관리자 계정 시드. 재기동 시 멱등 (existsByEmail 체크). 시스템관리자 1명 + 센터관리자 2명 (centers[0], centers[1] 매칭).
   * center 시드 이후 실행되므로 centerRepository 조회 안전.
   */
  private void seedAdmins() {
    // 시스템 관리자
    if (!userRepository.existsByEmail("sysadmin@youth-moa.test")) {
      userRepository.save(
          User.builder()
              .email("sysadmin@youth-moa.test")
              .password(passwordEncoder.encode(adminSeedPasswordSystem))
              .name("시스템관리자")
              .role(UserRole.SYSTEM_ADMIN)
              .build());
      log.info("Seeded SYSTEM_ADMIN: sysadmin@youth-moa.test");
    } else {
      log.info("SYSTEM_ADMIN already seeded, skip");
    }

    // 센터 관리자 2명 — centers[0], centers[1] 매칭
    List<Center> centers = centerRepository.findAll();
    if (centers.size() < 2) {
      log.warn("Not enough centers to seed CENTER_ADMIN (need >= 2, got {})", centers.size());
      return;
    }
    seedCenterAdmin("center1@youth-moa.test", "센터1관리자", centers.get(0), adminSeedPasswordCenter1);
    seedCenterAdmin("center2@youth-moa.test", "센터2관리자", centers.get(1), adminSeedPasswordCenter2);
  }

  private void seedCenterAdmin(String email, String name, Center center, String rawPassword) {
    if (userRepository.existsByEmail(email)) {
      log.info("CENTER_ADMIN already seeded: {} , skip", email);
      return;
    }
    User admin =
        User.builder()
            .email(email)
            .password(passwordEncoder.encode(rawPassword))
            .name(name)
            .role(UserRole.USER) // 임시 세팅 후 assignRole() 로 CENTER_ADMIN + center + scope 부여
            .build();
    admin.assignRole(UserRole.CENTER_ADMIN, center, center.getName());
    userRepository.save(admin);
    log.info("Seeded CENTER_ADMIN: {} (center={})", email, center.getName());
  }

  /** F2 헤더 종 UX 검증용 — seed1, seed30 에 알림 각 4건 시드. */
  /**
   * seed1 에 즐겨찾기 3건 시드 (2026-08-18 · P1-4). 재기동 시 멱등 (Bookmark count 체크). mypage favorites 탭 시각 계약
   * 통과 조건.
   */
  private void seedBookmarks() {
    if (bookmarkRepository.count() > 0) {
      log.info("Bookmarks already seeded, skip");
      return;
    }
    List<Program> programs = programRepository.findAll();
    if (programs.size() < 3) {
      log.warn("Not enough programs to seed bookmarks");
      return;
    }
    userRepository
        .findByEmail("seed1@youth-moa.test")
        .ifPresent(
            u -> {
              List<Bookmark> bs = new ArrayList<>();
              for (int i = 0; i < 3 && i < programs.size(); i++) {
                bs.add(Bookmark.builder().user(u).program(programs.get(i)).build());
              }
              bookmarkRepository.saveAll(bs);
              log.info("Seeded {} bookmarks for seed1", bs.size());
            });
  }

  private void seedNotifications() {
    if (notificationRepository.count() > 0) {
      log.info("Notifications already seeded, skip");
      return;
    }
    List<String> targetEmails = List.of("seed1@youth-moa.test", "seed30@youth-moa.test");
    for (String email : targetEmails) {
      userRepository
          .findByEmail(email)
          .ifPresent(
              u ->
                  notificationRepository.saveAll(
                      List.of(
                          Notification.builder()
                              .user(u)
                              .type(NotificationType.APPLICATION_APPROVED)
                              .title("프로그램 신청 승인")
                              .message("[취업역량 강화 워크숍] 신청이 승인되었습니다.")
                              .link("/mypage")
                              .build(),
                          Notification.builder()
                              .user(u)
                              .type(NotificationType.PROGRAM_DEADLINE_NEAR)
                              .title("마감 임박")
                              .message("[청년 창업 아카데미] 마감이 임박했어요. (D-1)")
                              .link("/programs")
                              .build(),
                          Notification.builder()
                              .user(u)
                              .type(NotificationType.WELCOME)
                              .title("공지사항")
                              .message("새 공지사항 — 7월 휴관 일정 안내")
                              .link("/notices")
                              .build(),
                          Notification.builder()
                              .user(u)
                              .type(NotificationType.APPLICATION_CANCELLED)
                              .title("신청 취소 처리")
                              .message("[마음건강 힐링 캠프] 취소가 처리되었습니다.")
                              .link("/mypage")
                              .build())));
    }
    log.info("Seeded notifications for {} users", targetEmails.size());
  }

  private void seedSiteImages() {
    if (siteImageRepository.count() > 0) {
      log.info("SiteImages already seeded (count={}), skip", siteImageRepository.count());
      return;
    }
    // F0e-2: HERO_BANNER 6건 (A/C/E/F/G/H) — 8초 크로스페이드 로테이션 대상
    List<String> heroIds =
        List.of(
            "1531482615713-2afd69097998", // A
            "1543269865-cbf427effbad", // C
            "1523580494863-6f3031224c94", // E
            "1511632765486-a01980e01a18", // F
            "1528605248644-14dd04022da1", // G
            "1540575467063-178a50c2df87"); // H
    List<SiteImage> images = new ArrayList<>();
    for (int i = 0; i < heroIds.size(); i++) {
      images.add(
          SiteImage.builder()
              .slot("HERO_BANNER")
              .imageUrl(
                  "https://images.unsplash.com/photo-" + heroIds.get(i) + "?w=1440&h=560&fit=crop")
              .sortOrder(i)
              .isActive(true)
              .build());
    }
    images.addAll(
        List.of(
            SiteImage.builder()
                .slot("HOME_SPACE_1")
                .imageUrl(
                    "https://images.unsplash.com/photo-1497366216548-37526070297c?w=460&h=340&fit=crop")
                .sortOrder(1)
                .isActive(true)
                .caption("상상대로")
                .build(),
            SiteImage.builder()
                .slot("HOME_SPACE_2")
                .imageUrl(
                    "https://images.unsplash.com/photo-1497366811353-6870744d04b2?w=460&h=340&fit=crop")
                .sortOrder(2)
                .isActive(true)
                .caption("내일스퀘어")
                .build(),
            SiteImage.builder()
                .slot("HOME_SPACE_3")
                .imageUrl(
                    "https://images.unsplash.com/photo-1524758631624-e2822e304c36?w=460&h=340&fit=crop")
                .sortOrder(3)
                .isActive(true)
                .caption("비행지구")
                .build()));
    siteImageRepository.saveAll(images);
    log.info("Seeded {} site images", images.size());
  }

  private void seedNotices() {
    if (noticeRepository.count() > 0) {
      log.info("Notices already seeded (count={}), skip", noticeRepository.count());
      return;
    }

    // A-admin-notice-attachment: 작성자 기반 RBAC 를 위해 sysadmin 을 createdBy 로 부착.
    // seedAdmins 가 seedNotices 앞에서 실행되므로 여기서 조회 가능.
    User sysadmin =
        userRepository
            .findByEmail("sysadmin@youth-moa.test")
            .orElseThrow(
                () -> new IllegalStateException("sysadmin seed must exist before seedNotices"));

    // 본문은 HTML 저장 (F0g Q3: 상세 이미지 인라인). prototype §5.14 참조 마크업 재구성.
    String bodyEvent =
        "<p>안녕하세요, 경기도 청년모아입니다.</p>"
            + "<p>「제1회 청년의 날 축제」 관련하여 아래와 같이 안내드립니다.</p>"
            + "<p><img src=\"https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=860&h=280&fit=crop\""
            + " alt=\"청년의 날 배너\" style=\"width:100%;border-radius:8px;margin:8px 0 20px\"/></p>"
            + "<p>· 대상: 경기도 거주 만 19~39세 청년</p>"
            + "<p>· 신청: 청년모아 홈페이지 및 방문 접수</p>"
            + "<p>· 문의: 청년센터 대표번호 031-000-0000</p>"
            + "<p>많은 관심과 참여 부탁드립니다. 감사합니다.</p>";
    String bodyGeneric =
        "<p>안녕하세요, 경기도 청년모아입니다.</p>"
            + "<p>자세한 내용은 아래를 확인해 주시기 바랍니다.</p>"
            + "<p>· 문의: 청년센터 대표번호 031-000-0000</p>"
            + "<p>많은 관심 부탁드립니다.</p>";

    List<Notice> notices =
        List.of(
            Notice.builder()
                .title("제1회 청년의 날 축제 안내")
                .content(bodyEvent)
                .category(NoticeCategory.EVENT)
                .isPinned(true)
                .imageUrl(
                    "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=360&h=220&fit=crop")
                .createdBy(sysadmin)
                .build(),
            Notice.builder()
                .title("2026년 상반기 청년센터 운영 방침")
                .content(bodyGeneric)
                .category(NoticeCategory.NOTICE)
                .isPinned(true)
                .imageUrl(
                    "https://images.unsplash.com/photo-1521737604893-d14cc237f11d?w=360&h=220&fit=crop")
                .createdBy(sysadmin)
                .build(),
            Notice.builder()
                .title("7월 청년센터 프로그램 일정 안내")
                .content(bodyGeneric)
                .category(NoticeCategory.NOTICE)
                .createdBy(sysadmin)
                .build(),
            Notice.builder()
                .title("7월 휴관 일정 안내")
                .content(bodyGeneric)
                .category(NoticeCategory.OPERATION)
                .createdBy(sysadmin)
                .build(),
            Notice.builder()
                .title("[경기도] 2026 경기 사회적 경제 박람회")
                .content(bodyEvent)
                .category(NoticeCategory.EVENT)
                .createdBy(sysadmin)
                .build(),
            Notice.builder()
                .title("청년 창업 아카데미 오픈 안내")
                .content(bodyGeneric)
                .category(NoticeCategory.EVENT)
                .createdBy(sysadmin)
                .build(),
            Notice.builder()
                .title("시설 점검에 따른 임시 휴관 안내")
                .content(bodyGeneric)
                .category(NoticeCategory.OPERATION)
                .createdBy(sysadmin)
                .build(),
            Notice.builder()
                .title("청년모아 회원가입 이벤트 진행")
                .content(bodyGeneric)
                .category(NoticeCategory.ETC)
                .createdBy(sysadmin)
                .build(),
            Notice.builder()
                .title("2026 경기청년 정책 설문조사 참여 요청")
                .content(bodyGeneric)
                .category(NoticeCategory.NOTICE)
                .createdBy(sysadmin)
                .build(),
            Notice.builder()
                .title("청년센터 이용 규정 개정 안내")
                .content(bodyGeneric)
                .category(NoticeCategory.OPERATION)
                .createdBy(sysadmin)
                .build(),
            Notice.builder()
                .title("AI 활용 특강 참가자 모집")
                .content(bodyEvent)
                .category(NoticeCategory.EVENT)
                .createdBy(sysadmin)
                .build(),
            Notice.builder()
                .title("개인정보 처리방침 개정 안내")
                .content(bodyGeneric)
                .category(NoticeCategory.ETC)
                .createdBy(sysadmin)
                .build());
    noticeRepository.saveAll(notices);
    log.info(
        "Seeded {} notices ({} pinned)",
        notices.size(),
        notices.stream().filter(Notice::isPinned).count());

    // 첨부파일 시드 — 앞 3건에 각 1~2개씩.
    // F-notice-attachment (2026-07-31): 첫 번째 첨부파일은 실 바이트 (classpath resource sample.pdf) 를 주입해
    // 사용자 관점 다운로드가 진짜 파일을 반환하도록 한다. 나머지 3건은 메타만 (legacy, 다운로드 시 404).
    byte[] samplePdf = loadSampleAttachmentBytes();
    List<NoticeAttachment> attachments = new ArrayList<>();
    attachments.add(
        NoticeAttachment.builder()
            .notice(notices.get(0))
            .fileName("청년의날_축제_안내문.pdf")
            .storedName("dummy-1.pdf")
            .fileSize(samplePdf != null ? samplePdf.length : 1_258_291L)
            .contentType("application/pdf")
            .sortOrder(0)
            .data(samplePdf)
            .build());
    attachments.add(
        NoticeAttachment.builder()
            .notice(notices.get(0))
            .fileName("축제_참가신청서.hwp")
            .storedName("dummy-2.hwp")
            .fileSize(102_400L)
            .contentType("application/x-hwp")
            .sortOrder(1)
            .build());
    attachments.add(
        NoticeAttachment.builder()
            .notice(notices.get(1))
            .fileName("2026_상반기_운영방침.pdf")
            .storedName("dummy-3.pdf")
            .fileSize(524_288L)
            .contentType("application/pdf")
            .sortOrder(0)
            .build());
    attachments.add(
        NoticeAttachment.builder()
            .notice(notices.get(2))
            .fileName("7월_프로그램_일정표.xlsx")
            .storedName("dummy-4.xlsx")
            .fileSize(204_800L)
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .sortOrder(0)
            .build());
    noticeAttachmentRepository.saveAll(attachments);
    log.info("Seeded {} notice attachments (1 with real PDF bytes)", attachments.size());
  }

  /** F-notice-attachment: classpath 의 sample.pdf 를 byte[] 로 로드. 리소스가 없으면 null 반환 (테스트 격리 환경 대응). */
  private byte[] loadSampleAttachmentBytes() {
    try {
      org.springframework.core.io.Resource resource =
          new org.springframework.core.io.ClassPathResource("data/notice-attachments/sample.pdf");
      if (!resource.exists()) return null;
      try (java.io.InputStream in = resource.getInputStream()) {
        return in.readAllBytes();
      }
    } catch (java.io.IOException e) {
      log.warn("Failed to load sample.pdf for notice attachment seed: {}", e.getMessage());
      return null;
    }
  }

  private void seedRegionsAndCenters() {
    if (regionRepository.count() > 0) {
      log.info("Regions already seeded (count={}), skip", regionRepository.count());
    } else {
      // 30 시·군 가나다순. F-signup-03 (spec §A-Q8) + HANDOFF §5.21: WelcomeScreen 관심 지역 옵션은
      // 상위 10개 (isFeatured=true) + 더보기 20개 (isFeatured=false). 필터 사이드바도 동일한 10개 노출.
      // TOP 10: 수원·성남·고양·용인·부천·안양·안산·화성·남양주·평택 (prototype.html WELCOME_REGIONS_TOP)
      List<String> regionNames =
          List.of(
              "가평군", "고양시", "과천시", "광명시", "광주시", "구리시", "군포시", "김포시", "남양주시", "동두천시", "부천시", "성남시",
              "수원시", "시흥시", "안산시", "안성시", "안양시", "양주시", "양평군", "여주시", "오산시", "용인시", "의왕시", "의정부시",
              "이천시", "파주시", "평택시", "포천시", "하남시", "화성시");
      java.util.Set<String> featuredNames =
          java.util.Set.of("수원시", "성남시", "고양시", "용인시", "부천시", "안양시", "안산시", "화성시", "남양주시", "평택시");
      List<Region> regions = new ArrayList<>();
      for (String name : regionNames) {
        regions.add(Region.builder().name(name).isFeatured(featuredNames.contains(name)).build());
      }
      regionRepository.saveAll(regions);
      log.info("Seeded {} regions ({} featured)", regions.size(), featuredNames.size());
    }

    if (centerRepository.count() > 0) {
      log.info("Centers already seeded (count={}), skip", centerRepository.count());
      return;
    }

    // F0h-real-coords: CSV(classpath:/data/centers.csv) 로 실좌표 + 전화 + 운영시간 + isActive 로드.
    // 시·군 대표 좌표 파생 시드 완전 제거 — CLAUDE.md §확장성 원칙 §파생 시드 금지 준수 (2026-07-09 F0h 사고 회고).
    List<CenterCsvRow> csvRows = centerCsvLoader.load();
    if (csvRows.size() != 48) {
      log.warn("centers.csv 시드 예상 48행과 다름 (실제 {}행)", csvRows.size());
    }

    // Region 테이블 존재 여부 사전 조회 — 없으면 warn (spec §9-2 유연성 원칙, 저장은 진행)
    java.util.Set<String> knownRegions =
        regionRepository.findAll().stream()
            .map(Region::getName)
            .collect(java.util.stream.Collectors.toSet());

    List<Center> centers = new ArrayList<>();
    for (CenterCsvRow row : csvRows) {
      if (!knownRegions.contains(row.region())) {
        log.warn(
            "centers.csv: region '{}' 이 Region 테이블에 없음 ({} 는 저장은 진행)", row.region(), row.name());
      }
      centers.add(
          Center.builder()
              .name(row.name())
              .region(row.region())
              .address(row.address())
              .phone(row.phone())
              .operatingHours(row.operatingHours())
              .latitude(row.latitude())
              .longitude(row.longitude())
              .isActive(row.isActive())
              // F0h-operating-hours-badge (spec §9-1): CSV 파싱한 구조화 운영시간. 파싱 불가 3행은 null.
              .schedule(row.schedule())
              .build());
    }

    // F0h-center-desc-image (spec §9-1): description·imageUrl 파생 시드 제거.
    // 기존 unsplash 6장 로테이션·이름 키워드 5종 desc·featured 8건 override 로직 완전 삭제 → centers-content.csv 로 이관.
    // CenterContent 저장은 centerRepository.saveAll(centers) 이후 실행 (아래).

    // name 가나다순으로 상위 5개 featured 표시
    Set<String> featuredNames =
        centers.stream()
            .map(Center::getName)
            .sorted()
            .limit(5)
            .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    for (Center c : centers) {
      if (featuredNames.contains(c.getName())) {
        c.markFeatured();
      }
    }

    centerRepository.saveAll(centers);
    log.info("Seeded {} centers ({} featured)", centers.size(), featuredNames.size());

    // F0h-center-desc-image (spec §9-1): CenterContent 시드 — CSV 로부터 로드.
    Map<String, Center> centerByName = new HashMap<>();
    for (Center c : centers) centerByName.put(c.getName(), c);
    List<CenterContentCsvRow> contentRows = centerContentCsvLoader.load();
    List<CenterContent> contents = new ArrayList<>();
    for (CenterContentCsvRow row : contentRows) {
      Center matched = centerByName.get(row.name());
      if (matched == null) {
        log.warn("centers-content.csv: name '{}' 이 centers.csv 와 매칭되지 않음 (skip)", row.name());
        continue;
      }
      contents.add(
          CenterContent.builder()
              .center(matched)
              .description(row.description())
              .imageUrl(row.imageUrl())
              .build());
    }
    centerContentRepository.saveAll(contents);
    log.info("Seeded {} center contents", contents.size());
  }

  private void seedPrograms() {
    if (programRepository.count() > 0) {
      log.info("Programs already seeded (count={}), skip", programRepository.count());
      return;
    }

    LocalDate today = LocalDate.now();
    List<Program> seeds =
        List.of(
            Program.builder()
                .title("취업역량 강화 워크숍")
                .organization("내일스퀘어 양평")
                .region("양평군")
                .imageUrl(
                    "https://images.unsplash.com/photo-1524178232363-1fb2b075b655?w=400&h=280&fit=crop")
                .content("이력서 작성, 면접 트레이닝, 자기소개 워크숍을 한 번에 진행합니다.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 39세 청년")
                        .region("경기도 거주 또는 활동 중인 청년")
                        .etc("전 회차 참석 가능자 우대")
                        .build())
                .startDate(today.minusDays(10))
                .endDate(today.plusDays(3))
                .capacity(30)
                .build(),
            Program.builder()
                .title("청년 창업 아카데미")
                .organization("안산시 청년센터 상상대로")
                .region("안산시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=400&h=280&fit=crop")
                .content("창업 아이디어 발굴부터 사업 모델 검증까지 8주 과정.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 39세 청년")
                        .region("안산시 거주 또는 활동")
                        .etc("창업 아이디어 보유자 우대")
                        .build())
                .startDate(today.minusDays(5))
                .endDate(today.plusDays(12))
                .capacity(25)
                .build(),
            Program.builder()
                .title("마음건강 힐링 캠프")
                .organization("범계역 청년출구")
                .region("안양시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=400&h=280&fit=crop")
                .content("청년의 번아웃을 회복하는 1박 2일 자연 힐링 캠프.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 34세 청년")
                        .region("안양시 거주 또는 활동")
                        .etc("전 회차 참여 가능자")
                        .build())
                .startDate(today.minusDays(2))
                .endDate(today.plusDays(6))
                .capacity(20)
                .build(),
            Program.builder()
                .title("디지털 마케팅 실전반")
                .organization("원미청(년)정(점)구역")
                .region("부천시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=400&h=280&fit=crop")
                .content("SNS·검색 광고 실전 캠페인 운영 실습.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 39세 청년")
                        .region("부천시 거주 또는 활동")
                        .etc("디지털 마케팅 입문자 대상")
                        .build())
                .startDate(today.minusDays(30))
                .endDate(today.minusDays(5))
                .capacity(15)
                .build(),
            Program.builder()
                .title("AI 활용 실무 교육")
                .organization("과천시 청년공간 비행지구")
                .region("과천시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1531482615713-2afd69097998?w=400&h=280&fit=crop")
                .content("ChatGPT·Claude 등 LLM 활용 실무 워크숍 (전 6강).")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 39세 청년")
                        .region("과천시 거주 또는 활동")
                        .etc("개발자 또는 기획자 우대")
                        .build())
                .startDate(today.plusDays(14))
                .endDate(today.plusDays(45))
                .capacity(30)
                .build(),
            Program.builder()
                .title("소셜벤처 인큐베이팅")
                .organization("양평청년공간 오름")
                .region("양평군")
                .imageUrl(
                    "https://images.unsplash.com/photo-1552664730-d307ca884978?w=400&h=280&fit=crop")
                .content("사회 문제 해결형 비즈니스 모델 인큐베이팅 6개월 과정.")
                // 의도적으로 age null — 화면 fallback ("연령 제한 없음") 검증용 시드
                .eligibility(
                    ProgramEligibility.builder()
                        .region("양평군 거주 또는 활동")
                        .etc("소셜벤처 관심 예비/초기 창업팀")
                        .build())
                .startDate(today.plusDays(21))
                .endDate(today.plusDays(180))
                .capacity(20)
                .build(),
            Program.builder()
                .title("청년 문화예술 스쿨")
                .organization("의왕청년발전소")
                .region("의왕시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1497366216548-37526070297c?w=460&h=340&fit=crop")
                .content("연극·사진·뮤지컬 등 6개 트랙 문화예술 입문 강좌.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 39세 청년")
                        .region("의왕시 거주 또는 활동")
                        .etc("문화예술 입문자 대상")
                        .build())
                .startDate(today.minusDays(7))
                .endDate(today.plusDays(30))
                .capacity(40)
                .build(),
            Program.builder()
                .title("청년 네트워킹 데이")
                .organization("오산청년일자리지원센터 이루잡")
                .region("오산시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1497366811353-6870744d04b2?w=460&h=340&fit=crop")
                .content("창업·취업 동료와 함께하는 월간 네트워킹 밋업.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 39세 청년")
                        .region("오산시 거주 또는 활동")
                        .etc("창업·취업 관심자 누구나")
                        .build())
                .startDate(today.minusDays(1))
                .endDate(today.plusDays(2))
                .capacity(60)
                .build(),
            // F0f-fix-1: SUSPENDED (운영 중단, 관리자 조치) 시나리오 시드
            Program.builder()
                .title("청년 목공 클래스")
                .organization("청년이봄")
                .region("성남시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1503387762-592deb58ef4e?w=460&h=340&fit=crop")
                .content("운영 사정으로 잠시 중단된 프로그램입니다.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 39세 청년")
                        .region("성남시 거주 또는 활동")
                        .etc("추후 재개 시 재공지")
                        .build())
                .startDate(today.plusDays(3))
                .endDate(today.plusDays(30))
                .capacity(16)
                .isActive(false)
                .build(),
            // F0f-fix-3: ENDED (기간 만료, 자연 종료) 시나리오 시드 — 종료 탭·그레이스케일 시각 검증용
            Program.builder()
                .title("자소서 첨삭 카페")
                .organization("광명시 청년동")
                .region("광명시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1455390582262-044cdead277a?w=460&h=340&fit=crop")
                .content("전문 컨설턴트와 함께하는 자기소개서 첨삭 프로그램입니다. (종료된 프로그램)")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 34세 청년")
                        .region("광명시 거주 또는 활동")
                        .etc("취업 준비생 우선")
                        .build())
                .startDate(today.minusDays(90))
                .endDate(today.minusDays(30))
                .capacity(20)
                .build(),
            // ── gap-batch2 (2026-07-27): pagination 데모용 추가 시드 15건 ──
            Program.builder()
                .title("포트폴리오 웹사이트 만들기")
                .organization("청누리")
                .region("수원시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=400&h=280&fit=crop")
                .content("HTML/CSS/JS 기초부터 배포까지 4주 실습.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 34세 청년")
                        .region("수원시")
                        .etc("입문자")
                        .build())
                .startDate(today.minusDays(3))
                .endDate(today.plusDays(28))
                .capacity(25)
                .build(),
            Program.builder()
                .title("영상 편집 실전반")
                .organization("내일꿈제작소")
                .region("고양시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?w=400&h=280&fit=crop")
                .content("Premiere Pro 로 YouTube·Shorts 편집 실습.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 39세 청년")
                        .region("고양시")
                        .etc("영상 취미자")
                        .build())
                .startDate(today.minusDays(1))
                .endDate(today.plusDays(35))
                .capacity(20)
                .build(),
            Program.builder()
                .title("바리스타 자격증 취득 과정")
                .organization("용인청년LAB 기흥")
                .region("용인시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1442512595331-e89e73853f31?w=400&h=280&fit=crop")
                .content("2급 바리스타 자격증 필기·실기 8주.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 34세 청년")
                        .region("용인시")
                        .etc("전 회차 참여")
                        .build())
                .startDate(today.minusDays(2))
                .endDate(today.plusDays(56))
                .capacity(16)
                .build(),
            Program.builder()
                .title("청년 부트캠프 백엔드 트랙")
                .organization("청년이봄 정자")
                .region("성남시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=400&h=280&fit=crop")
                .content("Spring Boot + JPA 실무 프로젝트 12주 부트캠프.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 34세 청년")
                        .region("성남시")
                        .etc("Java 경험자")
                        .build())
                .startDate(today.plusDays(7))
                .endDate(today.plusDays(90))
                .capacity(24)
                .build(),
            Program.builder()
                .title("도예 원데이 클래스")
                .organization("청년일자리카페 '청년e-room'")
                .region("이천시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1565193566173-7a0ee3dbe261?w=400&h=280&fit=crop")
                .content("도자기 물레 체험 원데이 클래스.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 39세 청년")
                        .region("이천시")
                        .etc("초보 환영")
                        .build())
                // seed-fix (2026-09-01): startDate=today 로 앵커 → 현재 월 view 에 OPEN 상태로 나타남
                // (ProgramListRenderTest.OPEN_/programs-calendar.spec.ts:129 OPEN chip 회귀 방지)
                .startDate(today.plusDays(0))
                .endDate(today.plusDays(10))
                .capacity(12)
                .build(),
            Program.builder()
                .title("청년 재테크 세미나")
                .organization("김포시청년지원센터")
                .region("김포시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1554224155-6726b3ff858f?w=400&h=280&fit=crop")
                .content("월급 관리·투자 기초 4강 세미나.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 39세 청년")
                        .region("김포시")
                        .etc("사회초년생 대상")
                        .build())
                .startDate(today.minusDays(4))
                .endDate(today.plusDays(24))
                .capacity(40)
                .build(),
            Program.builder()
                .title("스피치·발표 트레이닝")
                .organization("청춘곳간")
                .region("광명시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1475721027785-f74eccf877e2?w=400&h=280&fit=crop")
                .content("취업·비즈니스 발표 스피치 6주.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 39세 청년")
                        .region("광명시")
                        .etc("취업 준비생")
                        .build())
                .startDate(today.minusDays(6))
                .endDate(today.plusDays(15))
                .capacity(18)
                .build(),
            Program.builder()
                .title("데이터 분석 입문 캠프")
                .organization("화성시청년지원센터 H.E.Y")
                .region("화성시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=400&h=280&fit=crop")
                .content("Python·SQL·시각화 기초 5주.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 34세 청년")
                        .region("화성시")
                        .etc("데이터 관심자")
                        .build())
                .startDate(today.plusDays(14))
                .endDate(today.plusDays(50))
                .capacity(20)
                .build(),
            Program.builder()
                .title("UX·UI 디자인 워크숍")
                .organization("파주시청년공간 GP1939")
                .region("파주시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1541462608143-67571c6738dd?w=400&h=280&fit=crop")
                .content("Figma 로 배우는 실무 UX 리서치·UI 프로토타이핑.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 34세 청년")
                        .region("파주시")
                        .etc("디자인 관심자")
                        .build())
                .startDate(today.minusDays(2))
                .endDate(today.plusDays(19))
                .capacity(22)
                .build(),
            Program.builder()
                .title("청년 요가·명상 클래스")
                .organization("의정부시 청년공감터")
                .region("의정부시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=400&h=280&fit=crop")
                .content("주 2회 요가·명상 8주 프로그램.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 39세 청년")
                        .region("의정부시")
                        .etc("전 레벨 환영")
                        .build())
                .startDate(today.minusDays(3))
                .endDate(today.plusDays(53))
                .capacity(30)
                .build(),
            Program.builder()
                .title("청년 사진 워크숍")
                .organization("남양주시 청년창업센터 / 청년꽃간")
                .region("남양주시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=400&h=280&fit=crop")
                .content("스마트폰 사진에서 DSLR 까지 6주.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 39세 청년")
                        .region("남양주시")
                        .etc("사진 취미자")
                        .build())
                .startDate(today.plusDays(10))
                .endDate(today.plusDays(52))
                .capacity(15)
                .build(),
            Program.builder()
                .title("독서 모임 · 인문학 살롱")
                .organization("청년협업마을")
                .region("시흥시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=400&h=280&fit=crop")
                .content("월간 도서 함께 읽고 토론.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 39세 청년")
                        .region("시흥시")
                        .etc("책 좋아하는 청년")
                        .build())
                .startDate(today.minusDays(30))
                .endDate(today.minusDays(2))
                .capacity(20)
                .build(),
            Program.builder()
                .title("보컬 트레이닝 심화반")
                .organization("청년쉼표")
                .region("평택시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=400&h=280&fit=crop")
                .content("실전 무대 준비를 위한 보컬 트레이닝 10주.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 34세 청년")
                        .region("평택시")
                        .etc("음악 활동자")
                        .build())
                .startDate(today.minusDays(60))
                .endDate(today.minusDays(10))
                .capacity(12)
                .build(),
            Program.builder()
                .title("친환경 도시농부 프로젝트")
                .organization("하남시청년지원센터")
                .region("하남시")
                .imageUrl(
                    "https://images.unsplash.com/photo-1416879595882-3373a0480b5b?w=400&h=280&fit=crop")
                .content("옥상 텃밭 재배·유기농 요리 12주.")
                .eligibility(
                    ProgramEligibility.builder()
                        .age("만 19세 ~ 39세 청년")
                        .region("하남시")
                        .etc("환경 관심자")
                        .build())
                // seed-fix (2026-09-01): startDate=today+35 로 앵커 → 다음 월 view 에 UPCOMING 상태로 나타남
                // (ProgramListRenderTest.UPCOMING_ 회귀 방지 · Oct 이후 view 커버리지)
                .startDate(today.plusDays(35))
                .endDate(today.plusDays(105))
                .capacity(18)
                .build());

    programRepository.saveAll(seeds);
    log.info("Seeded {} programs", seeds.size());
  }

  /**
   * CapacityBar 시각 검증용 Application 시드. 시드 유저를 먼저 생성하고 세 가지 비율 케이스 데이터를 추가. ddl-auto: create-drop
   * 이므로 매 기동 시 초기화됨.
   */
  private void seedApplications() {
    if (applicationRepository.count() > 0) {
      log.info("Applications already seeded, skip");
      return;
    }
    List<Program> programs = programRepository.findAll();
    if (programs.size() < 3) {
      log.warn("Not enough programs to seed applications");
      return;
    }

    // 더미 유저 50명 생성 (UniqueConstraint: user+program 쌍)
    // seed1 은 계약 검사(interest chip · 맞춤 추천)의 대표 시나리오라 관심 지역·분야를 설정한다.
    // seed29~48 은 apply-complete 계약이 로컬 반복 실행 시 로테이션 풀로 사용 (2026-08-12 확장).
    List<User> seedUsers = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      String email = "seed" + i + "@youth-moa.test";
      if (!userRepository.existsByEmail(email)) {
        // F0i: 아이디/비밀번호 찾기 매칭용 phone 시드 (하이픈 없이 저장 — signup 정책과 동일)
        String phone = String.format("0100000%04d", i);
        java.util.Set<String> regions =
            (i == 1)
                ? new java.util.HashSet<>(java.util.List.of("부천시"))
                : new java.util.HashSet<>();
        java.util.Set<String> cats =
            (i == 1)
                ? new java.util.HashSet<>(java.util.List.of("취업", "창업"))
                : new java.util.HashSet<>();
        seedUsers.add(
            userRepository.save(
                User.builder()
                    .email(email)
                    .password(passwordEncoder.encode("Test1234!"))
                    .name("시드유저" + i)
                    .phone(phone)
                    .role(UserRole.USER)
                    .interestRegions(regions)
                    .interestCategories(cats)
                    .build()));
      } else {
        userRepository.findByEmail(email).ifPresent(seedUsers::add);
      }
    }

    // programs[0] = 취업역량 강화 워크숍 capacity=30 → 마감임박(90%+): 28명 신청
    // programs[1] = 청년 창업 아카데미 capacity=25 → 서두르세요(70~89%): 19명 신청
    // programs[2] = 마음건강 힐링 캠프 capacity=20 → 모집중(50% 이하): 6명 신청
    List<Application> applications = new ArrayList<>();

    // 마감임박 케이스: 28/30
    for (int i = 0; i < 28 && i < seedUsers.size(); i++) {
      applications.add(
          Application.builder()
              .user(seedUsers.get(i))
              .program(programs.get(0))
              .status(ApplicationStatus.APPROVED)
              .build());
    }

    // 서두르세요 케이스: 19/25
    for (int i = 0; i < 19 && i < seedUsers.size(); i++) {
      applications.add(
          Application.builder()
              .user(seedUsers.get(i))
              .program(programs.get(1))
              .status(ApplicationStatus.PENDING)
              .build());
    }

    // 모집중 케이스: 6/20
    for (int i = 0; i < 6 && i < seedUsers.size(); i++) {
      applications.add(
          Application.builder()
              .user(seedUsers.get(i))
              .program(programs.get(2))
              .status(ApplicationStatus.PENDING)
              .build());
    }

    applicationRepository.saveAll(applications);
    log.info("Seeded {} applications (capacity bar test data)", applications.size());
  }
}

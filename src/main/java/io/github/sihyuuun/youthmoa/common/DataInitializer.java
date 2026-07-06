package io.github.sihyuuun.youthmoa.common;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.center.Center;
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
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.region.Region;
import io.github.sihyuuun.youthmoa.region.RegionRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  private final ProgramRepository programRepository;
  private final RegionRepository regionRepository;
  private final CenterRepository centerRepository;
  private final NoticeRepository noticeRepository;
  private final NoticeAttachmentRepository noticeAttachmentRepository;
  private final SiteImageRepository siteImageRepository;
  private final UserRepository userRepository;
  private final ApplicationRepository applicationRepository;
  private final NotificationRepository notificationRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    seedRegionsAndCenters();
    seedPrograms();
    seedSiteImages();
    seedNotices();
    seedApplications();
    seedNotifications();
  }

  /** F2 헤더 종 UX 검증용 — seed1, seed30 에 알림 각 4건 시드. */
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
                .build(),
            Notice.builder()
                .title("2026년 상반기 청년센터 운영 방침")
                .content(bodyGeneric)
                .category(NoticeCategory.NOTICE)
                .isPinned(true)
                .build(),
            Notice.builder()
                .title("7월 청년센터 프로그램 일정 안내")
                .content(bodyGeneric)
                .category(NoticeCategory.NOTICE)
                .build(),
            Notice.builder()
                .title("7월 휴관 일정 안내")
                .content(bodyGeneric)
                .category(NoticeCategory.OPERATION)
                .build(),
            Notice.builder()
                .title("[경기도] 2026 경기 사회적 경제 박람회")
                .content(bodyEvent)
                .category(NoticeCategory.EVENT)
                .build(),
            Notice.builder()
                .title("청년 창업 아카데미 오픈 안내")
                .content(bodyGeneric)
                .category(NoticeCategory.EVENT)
                .build(),
            Notice.builder()
                .title("시설 점검에 따른 임시 휴관 안내")
                .content(bodyGeneric)
                .category(NoticeCategory.OPERATION)
                .build(),
            Notice.builder()
                .title("청년모아 회원가입 이벤트 진행")
                .content(bodyGeneric)
                .category(NoticeCategory.ETC)
                .build(),
            Notice.builder()
                .title("2026 경기청년 정책 설문조사 참여 요청")
                .content(bodyGeneric)
                .category(NoticeCategory.NOTICE)
                .build(),
            Notice.builder()
                .title("청년센터 이용 규정 개정 안내")
                .content(bodyGeneric)
                .category(NoticeCategory.OPERATION)
                .build(),
            Notice.builder()
                .title("AI 활용 특강 참가자 모집")
                .content(bodyEvent)
                .category(NoticeCategory.EVENT)
                .build(),
            Notice.builder()
                .title("개인정보 처리방침 개정 안내")
                .content(bodyGeneric)
                .category(NoticeCategory.ETC)
                .build());
    noticeRepository.saveAll(notices);
    log.info(
        "Seeded {} notices ({} pinned)",
        notices.size(),
        notices.stream().filter(Notice::isPinned).count());

    // 첨부파일 시드 — 앞 3건에 각 1~2개씩 (다운로드는 alert stub)
    List<NoticeAttachment> attachments = new ArrayList<>();
    attachments.add(
        NoticeAttachment.builder()
            .notice(notices.get(0))
            .fileName("청년의날_축제_안내문.pdf")
            .storedName("dummy-1.pdf")
            .fileSize(1_258_291L)
            .contentType("application/pdf")
            .sortOrder(0)
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
    log.info("Seeded {} notice attachments", attachments.size());
  }

  private void seedRegionsAndCenters() {
    if (regionRepository.count() > 0) {
      log.info("Regions already seeded (count={}), skip", regionRepository.count());
    } else {
      // 30 시·군 가나다순. 상위 5개 isFeatured=true (사이드바 노출 기본)
      List<String> regionNames =
          List.of(
              "고양시", "과천시", "광명시", "광주시", "구리시", "군포시", "김포시", "남양주시", "동두천시", "부천시", "성남시", "수원시",
              "시흥시", "안산시", "안성시", "안양시", "양주시", "양평군", "여주시", "연천군", "오산시", "용인시", "의왕시", "의정부시",
              "이천시", "파주시", "평택시", "포천시", "하남시", "화성시");
      List<Region> regions = new ArrayList<>();
      for (int i = 0; i < regionNames.size(); i++) {
        regions.add(Region.builder().name(regionNames.get(i)).isFeatured(i < 5).build());
      }
      regionRepository.saveAll(regions);
      log.info("Seeded {} regions ({} featured)", regions.size(), 5);
    }

    if (centerRepository.count() > 0) {
      log.info("Centers already seeded (count={}), skip", centerRepository.count());
      return;
    }

    // region_center_list.md — 48 청년공간 / 30 시·군
    Map<String, List<String>> centersByRegion = new LinkedHashMap<>();
    centersByRegion.put("고양시", List.of("내일꿈제작소", "28청춘창업소"));
    centersByRegion.put("과천시", List.of("과천시 청년공간 비행지구"));
    centersByRegion.put("광명시", List.of("청춘곳간", "광명시 청년동"));
    centersByRegion.put("광주시", List.of("광주시 청년지원센터"));
    centersByRegion.put("구리시", List.of("청년내일센터"));
    centersByRegion.put("군포시", List.of("군포시 청년공간 플라잉"));
    centersByRegion.put("김포시", List.of("김포시청년지원센터"));
    centersByRegion.put("남양주시", List.of("남양주시 청년창업센터 / 청년꽃간"));
    centersByRegion.put("동두천시", List.of("동두천시 청년창업지원센터"));
    centersByRegion.put("부천시", List.of("소사청년공간 소사로움", "원미청(년)정(점)구역", "오정청년공간"));
    centersByRegion.put("성남시", List.of("청년이봄", "청년이봄 야탑", "청년이봄 정자"));
    centersByRegion.put("수원시", List.of("청누리", "청년바람지대"));
    centersByRegion.put("시흥시", List.of("청년협업마을", "청년스테이션"));
    centersByRegion.put("안산시", List.of("안산시 청년센터 상상대로", "안산시 청년센터 상상스테이션"));
    centersByRegion.put("안성시", List.of("안성시청년문화공간 '청년톡톡'"));
    centersByRegion.put("안양시", List.of("범계역 청년출구", "동안 청년오피스", "만안 청년오피스", "안양청년1번가"));
    centersByRegion.put("양주시", List.of("양주시청년센터"));
    centersByRegion.put("양평군", List.of("양평청년공간 딴딴회관", "내일스퀘어 양평", "양평청년공간 오름"));
    centersByRegion.put("여주시", List.of("여주시청년활동지원센터 푸릇"));
    centersByRegion.put("연천군", List.of("연천군일자리통합지원센터"));
    centersByRegion.put("오산시", List.of("오산청년일자리지원센터 이루잡"));
    centersByRegion.put("용인시", List.of("용인청년LAB 기흥", "용인청년LAB 수지", "용인청년LAB 처인"));
    centersByRegion.put("의왕시", List.of("의왕청년발전소"));
    centersByRegion.put("의정부시", List.of("의정부시 청년공감터", "의정부시 청년다락방"));
    centersByRegion.put("이천시", List.of("청년일자리카페 '청년e-room'"));
    centersByRegion.put("파주시", List.of("파주시청년공간 GP1939"));
    centersByRegion.put("평택시", List.of("청년쉼표"));
    centersByRegion.put("포천시", List.of("포천시 청년센터"));
    centersByRegion.put("하남시", List.of("하남시청년지원센터"));
    centersByRegion.put("화성시", List.of("화성시 청년취업끝까지 지원센터", "화성시청년지원센터 H.E.Y"));

    // 전체 (region, name) 펼친 뒤 name 가나다순으로 상위 5개 isFeatured=true
    List<Center> centers = new ArrayList<>();
    centersByRegion.forEach(
        (region, names) ->
            names.forEach(n -> centers.add(Center.builder().name(n).region(region).build())));

    // F0h — 대표 8개 센터에 실좌표 (경기도 각 시 청년센터 대략 위치). 나머지는 null → 마커 skip.
    Map<String, java.math.BigDecimal[]> seedCoords = new LinkedHashMap<>();
    seedCoords.put(
        "청년바람지대",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.2636000"), new java.math.BigDecimal("127.0286000")
        });
    seedCoords.put(
        "청년이봄",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.4200000"), new java.math.BigDecimal("127.1265000")
        });
    seedCoords.put(
        "안양청년1번가",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.3943000"), new java.math.BigDecimal("126.9569000")
        });
    seedCoords.put(
        "소사청년공간 소사로움",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.5035000"), new java.math.BigDecimal("126.7660000")
        });
    seedCoords.put(
        "화성시청년지원센터 H.E.Y",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.1996000"), new java.math.BigDecimal("126.8311000")
        });
    seedCoords.put(
        "광명시 청년동",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.4772000"), new java.math.BigDecimal("126.8664000")
        });
    seedCoords.put(
        "양평청년공간 오름",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.4917000"), new java.math.BigDecimal("127.4874000")
        });
    seedCoords.put(
        "의왕청년발전소",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.3448000"), new java.math.BigDecimal("126.9682000")
        });
    for (Center c : centers) {
      java.math.BigDecimal[] latlng = seedCoords.get(c.getName());
      if (latlng != null) {
        c.updateCoordinates(latlng[0], latlng[1]);
      }
    }

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
                .requirements("만 19~39세 경기도 거주 청년")
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
                .requirements("창업 관심 청년 누구나")
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
                .requirements("만 19~34세 청년")
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
                .requirements("디지털 마케팅 입문 청년")
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
                .requirements("개발자 또는 기획자 청년")
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
                .requirements("소셜벤처 관심 예비/초기 창업팀")
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
                .requirements("문화예술 입문 청년")
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
                .requirements("청년 누구나")
                .startDate(today.minusDays(1))
                .endDate(today.plusDays(2))
                .capacity(60)
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

    // 더미 유저 30명 생성 (UniqueConstraint: user+program 쌍)
    List<User> seedUsers = new ArrayList<>();
    for (int i = 1; i <= 30; i++) {
      String email = "seed" + i + "@youth-moa.test";
      if (!userRepository.existsByEmail(email)) {
        // F0i: 아이디/비밀번호 찾기 매칭용 phone 시드 (하이픈 없이 저장 — signup 정책과 동일)
        String phone = String.format("0100000%04d", i);
        seedUsers.add(
            userRepository.save(
                User.builder()
                    .email(email)
                    .password(passwordEncoder.encode("Test1234!"))
                    .name("시드유저" + i)
                    .phone(phone)
                    .role(UserRole.USER)
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

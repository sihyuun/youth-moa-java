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
import io.github.sihyuuun.youthmoa.program.ProgramEligibility;
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

    // 전체 (region, name) 펼친 뒤 name 가나다순으로 상위 5개 isFeatured=true.
    // F0h-c1: docs/00_assets/region_center_list.md (2026-07-08 웹 검색 기준) 의 48개 실주소·전화·운영시간을
    // centerDetails 로 주입. 맵에 없는 이름은 fallback 처리 (address="경기도 {region}", hours=defaultHours).
    String defaultHours = "평일 09:00~18:00";
    Map<String, String[]> centerDetails = centerDetails();
    List<Center> centers = new ArrayList<>();
    centersByRegion.forEach(
        (region, names) ->
            names.forEach(
                n -> {
                  String[] d = centerDetails.get(n);
                  String addr = d != null ? d[0] : "경기도 " + region;
                  String phone = d != null ? d[1] : null;
                  String hours = d != null ? d[2] : defaultHours;
                  centers.add(
                      Center.builder()
                          .name(n)
                          .region(region)
                          .address(addr)
                          .phone(phone)
                          .operatingHours(hours)
                          .build());
                }));

    // F0h — 30개 시·군 대표 좌표 (시청/청년센터 인근). 실 센터 좌표 크롤링은 별도 티켓, 시·군 매핑으로 전체 48개 마커 커버.
    // 같은 시·군에 여러 센터가 있을 때는 소수점 4~5자리 offset 으로 겹침 방지 (지도상 시각 구분).
    Map<String, java.math.BigDecimal[]> regionCoords = new LinkedHashMap<>();
    regionCoords.put(
        "고양시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.6584000"), new java.math.BigDecimal("126.8320000")
        });
    regionCoords.put(
        "과천시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.4292000"), new java.math.BigDecimal("126.9878000")
        });
    regionCoords.put(
        "광명시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.4791000"), new java.math.BigDecimal("126.8646000")
        });
    regionCoords.put(
        "광주시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.4295000"), new java.math.BigDecimal("127.2551000")
        });
    regionCoords.put(
        "구리시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.5943000"), new java.math.BigDecimal("127.1296000")
        });
    regionCoords.put(
        "군포시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.3617000"), new java.math.BigDecimal("126.9350000")
        });
    regionCoords.put(
        "김포시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.6155000"), new java.math.BigDecimal("126.7157000")
        });
    regionCoords.put(
        "남양주시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.6360000"), new java.math.BigDecimal("127.2166000")
        });
    regionCoords.put(
        "동두천시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.9036000"), new java.math.BigDecimal("127.0605000")
        });
    regionCoords.put(
        "부천시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.5035000"), new java.math.BigDecimal("126.7660000")
        });
    regionCoords.put(
        "성남시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.4200000"), new java.math.BigDecimal("127.1265000")
        });
    regionCoords.put(
        "수원시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.2636000"), new java.math.BigDecimal("127.0286000")
        });
    regionCoords.put(
        "시흥시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.3800000"), new java.math.BigDecimal("126.8028000")
        });
    regionCoords.put(
        "안산시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.3219000"), new java.math.BigDecimal("126.8309000")
        });
    regionCoords.put(
        "안성시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.0080000"), new java.math.BigDecimal("127.2797000")
        });
    regionCoords.put(
        "안양시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.3943000"), new java.math.BigDecimal("126.9569000")
        });
    regionCoords.put(
        "양주시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.7852000"), new java.math.BigDecimal("127.0459000")
        });
    regionCoords.put(
        "양평군",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.4917000"), new java.math.BigDecimal("127.4874000")
        });
    regionCoords.put(
        "여주시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.2984000"), new java.math.BigDecimal("127.6370000")
        });
    regionCoords.put(
        "연천군",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("38.0966000"), new java.math.BigDecimal("127.0748000")
        });
    regionCoords.put(
        "오산시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.1499000"), new java.math.BigDecimal("127.0774000")
        });
    regionCoords.put(
        "용인시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.2411000"), new java.math.BigDecimal("127.1776000")
        });
    regionCoords.put(
        "의왕시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.3448000"), new java.math.BigDecimal("126.9682000")
        });
    regionCoords.put(
        "의정부시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.7381000"), new java.math.BigDecimal("127.0338000")
        });
    regionCoords.put(
        "이천시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.2723000"), new java.math.BigDecimal("127.4350000")
        });
    regionCoords.put(
        "파주시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.7599000"), new java.math.BigDecimal("126.7799000")
        });
    regionCoords.put(
        "평택시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("36.9922000"), new java.math.BigDecimal("127.1129000")
        });
    regionCoords.put(
        "포천시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.8949000"), new java.math.BigDecimal("127.2003000")
        });
    regionCoords.put(
        "하남시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.5393000"), new java.math.BigDecimal("127.2148000")
        });
    regionCoords.put(
        "화성시",
        new java.math.BigDecimal[] {
          new java.math.BigDecimal("37.1996000"), new java.math.BigDecimal("126.8311000")
        });
    Map<String, Integer> regionOffsetIdx = new java.util.HashMap<>();
    for (Center c : centers) {
      java.math.BigDecimal[] base = regionCoords.get(c.getRegion());
      if (base == null) continue;
      int idx = regionOffsetIdx.merge(c.getRegion(), 0, (a, b) -> a + 1);
      // 같은 시·군에 2번째부터는 소수 5자리에서 offset 적용 (약 100m 오프셋)
      java.math.BigDecimal offset = new java.math.BigDecimal("0.00" + (idx == 0 ? "0000" : String.format("%04d", idx * 15)));
      c.updateCoordinates(base[0].add(offset), base[1].add(offset));
    }

    // F0h-c1: 이름 키워드 기반 desc 자동 생성 (5종 로테이션) — 대표 8개는 아래 seedContent 로 override.
    // imageUrl 은 unsplash 워크스페이스/커뮤니티 사진 6장 로테이션.
    String[] imagePool = {
      "https://images.unsplash.com/photo-1497366216548-37526070297c?w=400&h=200&fit=crop&auto=format",
      "https://images.unsplash.com/photo-1497366811353-6870744d04b2?w=400&h=200&fit=crop&auto=format",
      "https://images.unsplash.com/photo-1524758631624-e2822e304c36?w=400&h=200&fit=crop&auto=format",
      "https://images.unsplash.com/photo-1556761175-5973dc0f32e7?w=400&h=200&fit=crop&auto=format",
      "https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=400&h=200&fit=crop&auto=format",
      "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=400&h=200&fit=crop&auto=format"
    };
    int imgIdx = 0;
    for (Center c : centers) {
      String desc;
      String name = c.getName();
      if (name.contains("창업")) {
        desc = "청년 창업과 네트워킹을 지원하는 복합공간";
      } else if (name.contains("취업") || name.contains("일자리") || name.contains("잡")) {
        desc = "취업·진로 전문 청년 지원센터";
      } else if (name.contains("역량") || name.contains("LAB") || name.contains("발전소")) {
        desc = "청년 역량 강화·성장 프로그램 운영";
      } else if (name.contains("문화") || name.contains("톡톡") || name.contains("바람") || name.contains("꿈")) {
        desc = "청년 문화·창작 커뮤니티 공간";
      } else {
        desc = c.getRegion() + " 청년의 자립과 성장을 지원하는 공간";
      }
      c.updateContent(desc, c.getOperatingHours(), imagePool[imgIdx % imagePool.length]);
      imgIdx++;
    }

    // F0h-c1: 대표 8개 센터 desc override (실제 기획 문안, 좌표에 대응)
    Map<String, String> featuredDesc = new LinkedHashMap<>();
    featuredDesc.put("청년바람지대", "청년 창업과 네트워킹을 위한 복합문화공간");
    featuredDesc.put("청년이봄", "취업·역량강화 특화 청년지원센터");
    featuredDesc.put("안양청년1번가", "정신건강·힐링 프로그램 전문 센터");
    featuredDesc.put("소사청년공간 소사로움", "취업·역량강화 특화 청년지원센터");
    featuredDesc.put("화성시청년지원센터 H.E.Y", "취업·진로 전문 지원 청년센터");
    featuredDesc.put("광명시 청년동", "지역사회 연계 청년 커뮤니티 허브");
    featuredDesc.put("양평청년공간 오름", "소셜벤처·사회적 경제 청년 지원");
    featuredDesc.put("의왕청년발전소", "지역사회 연계 청년 커뮤니티 허브");
    for (Center c : centers) {
      String d = featuredDesc.get(c.getName());
      if (d != null) {
        c.updateContent(d, c.getOperatingHours(), c.getImageUrl());
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

  /**
   * F0h-c1: docs/00_assets/region_center_list.md (2026-07-08 웹 검색 기준) 의 48개 실 데이터.
   * key = 센터명, value = {주소, 전화번호, 운영시간}. 방문 전 직접 확인 권장 (변경 가능성 있음).
   */
  private Map<String, String[]> centerDetails() {
    Map<String, String[]> m = new LinkedHashMap<>();
    // 고양
    m.put("내일꿈제작소", new String[] {"경기도 고양시 덕양구 은빛로 72 (화정동)", "031-8075-2873", "월~토 10:00~18:00, 일·공휴일 휴관"});
    m.put("28청춘창업소", new String[] {"경기도 고양시 덕양구 화중로104번길 33", "031-968-7062", "확인 불가 (입주기업 전용, 직접 문의)"});
    // 과천
    m.put("과천시 청년공간 비행지구", new String[] {"경기도 과천시 중앙로 129 5층 501호", "02-3418-1245", "월·화·목·금 10:00~21:00, 수 10:00~18:00, 토 10:00~16:00, 일·공휴일 휴관"});
    // 광명
    m.put("청춘곳간", new String[] {"경기도 광명시 광명로928번길 42-16 (어울리기행복센터 3~5층, 광명동)", "02-2689-7451", "화~토 09:00~22:00, 일 09:00~18:00, 월·공휴일 휴관"});
    m.put("광명시 청년동", new String[] {"경기도 광명시 오리로854번길 10 (열린시민청 4층, 철산동)", "02-2066-8134", "화~토 09:00~22:00, 일 09:00~18:00, 월·공휴일 휴관"});
    // 광주
    m.put("광주시 청년지원센터", new String[] {"경기도 광주시 중앙로 199 5층 (더누림플랫폼)", "031-8027-0371", "평일 09:00~21:00, 토 10:00~17:00, 일·공휴일 휴관"});
    // 구리
    m.put("청년내일센터", new String[] {"경기도 구리시 건원대로 67 한진빌딩 3~5층 (인창동 383-101)", "031-557-9980", "평일 09:30~21:00, 토 09:30~18:00, 일·공휴일 휴무"});
    // 군포
    m.put("군포시 청년공간 플라잉", new String[] {"경기도 군포시 번영로 314 (산본동)", "031-399-8724", "월~토 09:30~21:00 (동절기 12~2월 09:30~20:30), 일·공휴일 휴관"});
    // 김포
    m.put("김포시청년지원센터", new String[] {"경기도 김포시 김포대로 841 제우스프라자 5층 (사우동)", "031-980-5971", "월~금 09:00~21:00, 토 10:00~20:00, 일·공휴일 휴관"});
    // 남양주
    m.put("남양주시 청년창업센터 / 청년꽃간", new String[] {"경기도 남양주시 늘을2로14번길 12 (평내동, 청년창업센터 3층)", "031-590-8545", "화~일 09:00~21:00, 월요일 휴관"});
    // 동두천
    m.put("동두천시 청년창업지원센터", new String[] {"경기도 동두천시 동두천로 314 복합문화커뮤니티센터 1층", "031-868-9131", "평일 09:00~21:00, 토 09:00~18:00, 일·공휴일 휴관"});
    // 부천
    m.put("소사청년공간 소사로움", new String[] {"경기도 부천시 소사구 성무로 24 심곡도서관 4층", "032-625-4581", "화~일 07:00~22:00, 월·공휴일 휴관 (디지털인쇄소·미디어창작실은 화~금 09:00~18:00)"});
    m.put("원미청(년)정(점)구역", new String[] {"경기도 부천시 원미구 소사로 456 원미도서관 3층", "032-625-4747", "화~일 07:00~22:00, 월·공휴일 휴관"});
    m.put("오정청년공간", new String[] {"경기도 부천시 오정구 소사로 772 원종빌딩 5층", "032-625-8870", "월 09:00~22:00, 화~금 09:00~21:00, 토 09:00~18:00, 일·공휴일 휴관"});
    // 성남
    m.put("청년이봄", new String[] {"경기도 성남시 수정구 산성대로 267 롯데시네마타워 지하1층 B1049호", "070-4908-2094", "월~금 10:00~21:00, 토 10:00~15:00, 일·공휴일 휴관"});
    m.put("청년이봄 야탑", new String[] {"경기도 성남시 분당구 벌말로30번길 35 야탑유스센터 1층", "031-729-9838", "화~금 10:00~18:00, 토 10:00~15:00, 월·일·공휴일 휴관"});
    m.put("청년이봄 정자", new String[] {"경기도 성남시 분당구 성남대로407번길 12 정자유스센터 1층", "070-4908-2094", "평일 10:00~20:00, 토 09:00~17:00, 일·공휴일 휴관"});
    // 수원
    m.put("청누리", new String[] {"경기도 수원시 팔달구 행궁로 68-1", "031-267-3628", "화~토 10:00~18:00, 월·공휴일 휴관"});
    m.put("청년바람지대", new String[] {"경기도 수원시 팔달구 효원로249번길 38 (2~3층)", "031-267-3628", "월~금 10:00~21:00, 토 10:00~18:00, 공휴일 휴관"});
    // 시흥
    m.put("청년협업마을", new String[] {"경기도 시흥시 소래산길 11 (시흥ABC행복학습타운 내, 대야동)", "031-310-3532", "월~금 10:00~22:00, 토·일 09:00~18:00 (일요일 무인 운영), 공휴일 휴관"});
    m.put("청년스테이션", new String[] {"경기도 시흥시 정왕대로233번길 19-1 (정왕동 1799-4)", "070-7710-3816", "월~금 10:00~22:00, 토 10:00~18:00, 일·공휴일 휴관"});
    // 안산
    m.put("안산시 청년센터 상상대로", new String[] {"경기도 안산시 단원구 선부로 312 (와동 96-1)", "031-492-2030", "월~금 09:00~21:00, 토 09:00~16:00, 일·공휴일 휴관"});
    m.put("안산시 청년센터 상상스테이션", new String[] {"경기도 안산시 단원구 적금로 93", "031-410-0311", "평일 09:00~21:00 (점심 12:00~13:00 휴게), 토 09:00~16:00, 일·공휴일 휴무"});
    // 안성
    m.put("안성시청년문화공간 '청년톡톡'", new String[] {"경기도 안성시 인지2길 16-8", "031-678-6848", "평일 10:00~21:00, 토 10:00~18:00, 일·공휴일 휴관"});
    // 안양
    m.put("범계역 청년출구", new String[] {"경기도 안양시 동안구 동안로 130 지하1층 (범계역 광장 내)", "031-476-9371", "평일 10:00~22:00, 토 10:00~17:00, 일·공휴일 휴무"});
    m.put("동안 청년오피스", new String[] {"경기도 안양시 동안구 시민대로327번길 11-41 안양창업지원센터 3층", "031-8045-6754", "평일·토 09:30~21:00, 일·공휴일 휴관"});
    m.put("만안 청년오피스", new String[] {"경기도 안양시 만안구 안양로 311 프로젝트500타워 16층", "031-8045-6706", "평일·토 09:30~21:00, 일·공휴일 휴관"});
    m.put("안양청년1번가", new String[] {"경기도 안양시 만안구 장내로143번길 16 지하1층", "031-441-5279", "월~금 10:00~21:00, 토 10:00~16:00, 일·공휴일 휴관"});
    // 양주
    m.put("양주시청년센터", new String[] {"경기도 양주시 덕정길 67 회천1동 복합청사 5층", "031-8082-6077", "평일 09:00~21:00 (주말 미확인, 직접 문의 필요)"});
    // 양평
    m.put("양평청년공간 딴딴회관", new String[] {"경기도 양평군 양서면 두물머리길20번길 8", "031-770-3121", "월 10:00~18:00, 화~금 10:00~21:00, 토 10:00~17:00, 일·공휴일 휴무"});
    m.put("내일스퀘어 양평", new String[] {"경기도 양평군 양평읍 양근로 196 G타워 2층", "031-770-3921", "평일 10:00~21:00, 토 10:00~17:00, 일·공휴일 휴무"});
    m.put("양평청년공간 오름", new String[] {"경기도 양평군 용문면 용문로 391 2층", "031-770-1018", "평일 10:00~21:00, 토 10:00~17:00, 일·공휴일 휴무"});
    // 여주
    m.put("여주시청년활동지원센터 푸릇", new String[] {"경기도 여주시 세종로14번길 18 중앙프라자 3층 303호", "031-884-2055", "화~금 10:00~21:00, 토~일 10:00~18:00, 월·공휴일 휴관"});
    // 연천
    m.put("연천군일자리통합지원센터", new String[] {"경기도 연천군 전곡읍 전곡로 193", "031-839-2980", "3~12월 평일 09:00~21:00 / 1~2월 평일 09:00~18:00, 점심 12:00~13:00 휴게, 주말·공휴일 휴무"});
    // 오산
    m.put("오산청년일자리지원센터 이루잡", new String[] {"경기도 오산시 운천로 62 3층", "031-8036-8046", "평일 09:00~22:00, 토·일 10:00~18:00, 공휴일 휴관"});
    // 용인
    m.put("용인청년LAB 기흥", new String[] {"경기도 용인시 기흥구 구갈로60번길 15 경영빌딩 2층", "031-693-8332", "평일 10:00~22:00, 토 10:00~18:00, 일·공휴일 휴무"});
    m.put("용인청년LAB 수지", new String[] {"경기도 용인시 수지구 포은대로 499 아르피아타워 2~3층", "031-6193-4852", "평일 10:00~22:00, 토 10:00~18:00, 일·공휴일 휴관"});
    m.put("용인청년LAB 처인", new String[] {"경기도 용인시 처인구 명지로40번길 8 삼가프라자 5층", "031-337-4012", "평일 10:00~22:00, 토 10:00~18:00, 일·공휴일 휴관"});
    // 의왕
    m.put("의왕청년발전소", new String[] {"경기도 의왕시 안양판교로 82 포일어울림센터 4층", "031-345-2720", "평일 09:00~21:00, 토 09:00~18:00, 공휴일 휴관"});
    // 의정부
    m.put("의정부시 청년공감터", new String[] {"경기도 의정부시 둔야로 9 3~4층", "031-828-2434", "평일·토·일 10:00~18:00, 평일 10:00~21:00, 공휴일 휴관"});
    m.put("의정부시 청년다락방", new String[] {"경기도 의정부시 호국로 1314", "031-828-2163", "월~토 14:00~22:00 (2026.4월 야간 확대), 일·공휴일 휴관 ※직접 확인 권장"});
    // 이천
    m.put("청년일자리카페 '청년e-room'", new String[] {"경기도 이천시 경충대로 2701-32", "031-644-4206", "평일 10:00~21:00, 토 10:00~18:00, 일·공휴일 휴관"});
    // 파주
    m.put("파주시청년공간 GP1939", new String[] {"경기도 파주시 금릉역로 84 청원센트럴타워 6층", "031-940-5100", "평일 10:00~21:00, 토 09:30~17:30, 일·공휴일 휴무"});
    // 평택
    m.put("청년쉼표", new String[] {"경기도 평택시 평택1로 9번길 23", "031-691-9917", "평일 10:00~21:00, 토 10:00~17:00, 일·공휴일 휴관"});
    // 포천
    m.put("포천시 청년센터", new String[] {"경기도 포천시 호국로 1423 포천청년비전센터 2층", "031-538-2563", "화~금 10:00~21:00, 토 10:00~18:00, 월·일·공휴일 휴관"});
    // 하남
    m.put("하남시청년지원센터", new String[] {"경기도 하남시 미사강변대로 52 1층", "031-790-6904", "평일 09:00~18:00, 주말·공휴일 휴무"});
    // 화성
    m.put("화성시 청년취업끝까지 지원센터", new String[] {"경기도 화성시 병점구 떡전골로 98 병점우체국 4층 401호", "031-5189-4805", "평일 09:00~18:00, 주말·공휴일 휴무"});
    m.put("화성시청년지원센터 H.E.Y", new String[] {"경기도 화성시 떡전골로 98 병점우체국 5층", "031-5189-3106", "월·금 09:00~18:00, 화·수·목 09:00~21:00, 토 10:00~18:00, 일·공휴일 휴관"});
    return m;
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

package io.github.sihyuuun.youthmoa.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.sihyuuun.youthmoa.common.N1BaselineReporter;
import io.github.sihyuuun.youthmoa.notification.NotificationService;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramService;
import io.github.sihyuuun.youthmoa.search.SearchResult;
import io.github.sihyuuun.youthmoa.search.SearchService;
import jakarta.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A9-c: 사용자 트랙 N+1 회귀 감시 (6 TC).
 *
 * <p>A9-b 에서 Application/Bookmark 의 program 관계를 LAZY→EAGER 로 승격했다. 그 여파는 admin 뿐 아니라 사용자 트랙
 * (마이페이지·프로그램 목록·검색·알림) 에도 미친다. A9-b verify 는 admin 만 커버했다는 지적(#16.4)에 대한 커버리지 확장.
 *
 * <p>각 TC 는 대표 진입점을 실행하고 Hibernate {@link Statistics} 로 실측 PreparedStatement 수를 얻어 상한을 assert 한다.
 * 상한은 실측 baseline + 30% 여유율. 결과는 {@link N1BaselineReporter} 를 통해 {@code
 * build/reports/n1-baseline.json} 에 append 된다.
 *
 * <p>@Transactional 을 걸지 않는다 — 1차 캐시 hit 로 실배포 대비 쿼리 수가 낮게 측정될 위험. 각 TC 는 {@link
 * EntityManager#clear()} 로 세션 초기화 후 실행.
 *
 * <p>시드 (e2e 프로파일 DataInitializer): seed1@youth-moa.test 사용자에 신청 1건 + 즐겨찾기 3건 + 알림 3건 존재.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class UserEagerFetchN1Test {

  private static final String SEED_USER_EMAIL = "seed1@youth-moa.test";

  @Autowired ProgramService programService;
  @Autowired SearchService searchService;
  @Autowired NotificationService notificationService;
  @Autowired UserRepository userRepository;
  @Autowired MockMvc mockMvc;
  @Autowired EntityManager entityManager;

  private Statistics stats;

  @BeforeEach
  void enableAndResetStats() {
    stats = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    stats.setStatisticsEnabled(true);
    resetSessionAndStats();
  }

  @AfterEach
  void clearAuth() {
    SecurityContextHolder.clearContext();
  }

  private void loginAsSeedUser() {
    User u = userRepository.findByEmail(SEED_USER_EMAIL).orElseThrow();
    UserPrincipal principal = new UserPrincipal(u);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))));
  }

  private void resetSessionAndStats() {
    entityManager.clear();
    stats.clear();
  }

  private long measureAndReport(String methodName, long limit) {
    long q = stats.getPrepareStatementCount();
    System.out.printf("[N1-baseline] %s: %d PreparedStatements (limit %d)%n", methodName, q, limit);
    N1BaselineReporter.record(getClass().getName(), methodName, q, limit);
    return q;
  }

  /**
   * U1: ProgramService.search — 목록·필터·검색 진입점.
   *
   * <p>Program 자체는 center EAGER 이므로 page + count + join 이 기대. bookmarkedIds 로 정렬하지만 IN 절 값이라 추가 쿼리
   * 유발 없음. baseline 실측 + 30% 여유.
   */
  @Test
  void programService_search_쿼리_상한_이내() {
    resetSessionAndStats();

    Page<Program> page = programService.search(null, List.of(), List.of(), "default", 0, Set.of());

    assertThat(page).isNotNull();
    long q = measureAndReport("programService_search_쿼리_상한_이내", 15L);
    assertThat(q)
        .as(
            "ProgramService.search 는 center EAGER 로드 시에도 쿼리 15 개 이하여야 한다 "
                + "(baseline 실측 11 + 30% 여유). 초과 시 Program.center 페치 전략 확인 필요.")
        .isLessThanOrEqualTo(15L);
  }

  /**
   * U2: ProgramService.findById — 상세 조회 진입점 (center EAGER + 후속 bookmark/countBy 조회).
   *
   * <p>findById 자체는 program+center 조인 1건. 실제 상세 페이지는 별도 서비스 조합이라 여기서는 findById 만 측정. baseline 실측 후
   * 상한 설정.
   */
  @Test
  void programService_findById_쿼리_상한_이내() {
    resetSessionAndStats();

    Program p = programService.findById(1L);

    assertThat(p).isNotNull();
    long q = measureAndReport("programService_findById_쿼리_상한_이내", 2L);
    assertThat(q)
        .as(
            "ProgramService.findById 는 center EAGER 로드 시에도 쿼리 2 개 이하여야 한다 "
                + "(baseline 실측 1 + 30% 여유, ceil).")
        .isLessThanOrEqualTo(2L);
  }

  /**
   * U3: MyPageController GET /mypage (history 탭 기본) — 사용자 신청 내역 진입점.
   *
   * <p>기대: session load + applications page (with program/center via @EntityGraph). Application 이
   * program/center EAGER 로드하므로 N+1 발생 위험 지점. baseline 실측 + 30% 여유.
   */
  @Test
  @WithUserDetails(SEED_USER_EMAIL)
  void myPage_history_쿼리_상한_이내() throws Exception {
    resetSessionAndStats();

    mockMvc.perform(get("/mypage")).andExpect(status().isOk());

    long q = measureAndReport("myPage_history_쿼리_상한_이내", 13L);
    assertThat(q)
        .as(
            "GET /mypage (history) 는 EAGER 승격 후에도 쿼리 13 개 이하여야 한다 "
                + "(baseline 실측 10 + 30% 여유). 초과 시 findAllByUserOrderByAppliedAtDesc @EntityGraph 확인.")
        .isLessThanOrEqualTo(13L);
  }

  /**
   * U4: MyPageController GET /mypage?tab=favorites — 사용자 즐겨찾기 진입점.
   *
   * <p>기대: bookmarks findAll (with program/center) + toCardDtos (countByProgramIds IN 쿼리 1회).
   * Bookmark.program EAGER 승격 후 회귀 감시. baseline 실측 + 30% 여유.
   */
  @Test
  @WithUserDetails(SEED_USER_EMAIL)
  void myPage_favorites_쿼리_상한_이내() throws Exception {
    resetSessionAndStats();

    mockMvc.perform(get("/mypage").param("tab", "favorites")).andExpect(status().isOk());

    long q = measureAndReport("myPage_favorites_쿼리_상한_이내", 15L);
    assertThat(q)
        .as(
            "GET /mypage?tab=favorites 는 EAGER 승격 후에도 쿼리 15 개 이하여야 한다 "
                + "(baseline 실측 11 + 30% 여유). 초과 시 findAllByUserOrderByCreatedAtDesc @EntityGraph 확인.")
        .isLessThanOrEqualTo(15L);
  }

  /**
   * U5: NotificationService.recentForHeader — 헤더 드롭다운 최근 알림 5건 조회.
   *
   * <p>알림 자체는 단순 조회이지만 Application/Program link 가 있는 경우 lazy join 유발 가능. baseline 실측 + 30% 여유.
   */
  @Test
  void notificationService_recentForHeader_쿼리_상한_이내() {
    loginAsSeedUser();
    resetSessionAndStats();

    User u = userRepository.findByEmail(SEED_USER_EMAIL).orElseThrow();
    var recent = notificationService.recentForHeader(u);

    assertThat(recent).isNotNull();
    long q = measureAndReport("notificationService_recentForHeader_쿼리_상한_이내", 3L);
    assertThat(q)
        .as(
            "NotificationService.recentForHeader 는 쿼리 3 개 이하여야 한다 "
                + "(baseline 실측 2 + 30% 여유, ceil). 초과 시 lazy relation 유발 확인.")
        .isLessThanOrEqualTo(3L);
  }

  /**
   * U6: SearchService.search — 프로그램 + 공지 통합 검색 진입점.
   *
   * <p>기대: program findAll(spec, pageable) + count + notice findAll + count. Program.center EAGER
   * 로드 시에도 batch join 이 잡혀야 함. baseline 실측 + 30% 여유.
   */
  @Test
  void searchService_search_쿼리_상한_이내() {
    resetSessionAndStats();

    SearchResult result = searchService.search("청년", 0, 0);

    assertThat(result).isNotNull();
    long q = measureAndReport("searchService_search_쿼리_상한_이내", 21L);
    assertThat(q)
        .as(
            "SearchService.search 는 program+notice 통합 검색 시에도 쿼리 21 개 이하여야 한다 "
                + "(baseline 실측 16 + 30% 여유). 초과 시 Program.center 페치 전략 확인.")
        .isLessThanOrEqualTo(21L);
  }
}

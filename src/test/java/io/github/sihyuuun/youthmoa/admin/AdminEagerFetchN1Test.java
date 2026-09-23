package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.bookmark.Bookmark;
import io.github.sihyuuun.youthmoa.bookmark.BookmarkRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * A9-b verify UNVERIFIED #16 해소: admin 계층 findAll 진입점 N+1 회귀 감시.
 *
 * <p>A9-b 에서 Application.program · Bookmark.program 을 LAZY→EAGER 로 승격했다 (open-in-view=false 하
 * MyPageRenderTest 실패로 인해 필수 조치). Repository 의 findAllByUser* 메서드는 {@code @EntityGraph({"program",
 * "program.center"})} 로 명시적 fetch join 을 걸었으나, JpaRepository 기본 findAll() /
 * JpaSpecificationExecutor findAll(spec, pageable) 은 @EntityGraph 없어 Application 마다 program 별도
 * SELECT 위험 (N+1).
 *
 * <p>본 테스트는 대표 진입점에서 실제 실행 쿼리 수를 Hibernate {@link Statistics} 로 측정하고, 상한을 assert 한다. 상한 초과 시 N+1 발생
 * 신호 → Repository 커스텀 메서드에 @EntityGraph 추가로 방어.
 *
 * <p>e2e 프로파일 시드 (DataInitializer): program 1 (내일스퀘어 양평) 에 28건 APPROVED, program 2 에 19건 PENDING,
 * program 3 에 6건 PENDING. 총 53건 이상의 Application 시드 존재.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@Transactional
class AdminEagerFetchN1Test {

  @Autowired AdminApplicationService adminApplicationService;
  @Autowired AdminDashboardService adminDashboardService;
  @Autowired AdminStatsService adminStatsService;
  @Autowired ApplicationRepository applicationRepository;
  @Autowired BookmarkRepository bookmarkRepository;
  @Autowired UserRepository userRepository;
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

  private void loginAsSysadmin() {
    User u = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(u);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
  }

  /**
   * AdminApplicationService.list — Page<Application> 10 건 로드 시 쿼리 수가 상한 이내인지 검증.
   *
   * <p>기대 쿼리: (1) applications page + (2) count + (3) program 배치/join + (4) center 배치/join + (5)
   * user 배치/join. N+1 발생 시 10 개 row 마다 program/center 각각 SELECT → 20+ 쿼리로 초과.
   *
   * <p>verify #14 대응: baseline 실측 3 · 상한 6 (실측 + 100% 여유). 소폭 회귀도 즉시 감지.
   */
  /** 실측 쿼리 수를 System.out 으로 노출해 baseline 을 문서화. verify #16.3 대응. */
  private long measureAndReport(String label) {
    long q = stats.getPrepareStatementCount();
    System.out.printf("[N1-baseline] %s: %d PreparedStatements%n", label, q);
    return q;
  }

  /**
   * 배포 환경 (요청당 신규 Session) 재현. verify #16.5 대응.
   *
   * <p>{@code @Transactional} 테스트는 단일 Session 유지로 1차 캐시 hit 하여 실 배포 대비 쿼리 수가 낮게 측정될 수 있다. 각 시나리오 실행
   * 전 {@link EntityManager#clear()} 로 1차 캐시를 비워 재현성 확보.
   */
  private void resetSessionAndStats() {
    entityManager.clear();
    stats.clear();
  }

  @Test
  void adminApplicationService_list_쿼리_상한_12_이내() {
    loginAsSysadmin();
    resetSessionAndStats();

    // 시드 program 1 (내일스퀘어 양평) 에 28 건 APPROVED 존재 · 페이지당 10 건 노출
    Page<Application> page = adminApplicationService.list(1L, null, null, 0);

    assertThat(page.getContent()).isNotEmpty();
    long executedQueries = measureAndReport("AdminApplicationService.list(programId=1)");
    assertThat(executedQueries)
        .as(
            "AdminApplicationService.list 는 program·center EAGER 로드 시에도 쿼리 6 개 이하여야 한다 "
                + "(baseline 3 + 100% 여유). 초과 시 N+1 신호 → "
                + "ApplicationRepository.findAll(Specification, Pageable) 에 @EntityGraph 추가 필요.")
        .isLessThanOrEqualTo(6L);
  }

  /**
   * ApplicationRepository.findAll() (Dashboard/Stats 진입점) — 전체 로드 시 쿼리 수 상한.
   *
   * <p>{@link AdminDashboardService}, {@link AdminStatsService} 가 findAll() 을 호출해 List<Application>
   * 을 받는다. 시드 데이터 50+ 건에서 N+1 발생 시 100 쿼리 이상 발화. 상한 20 은 여유 있게 잡아 EAGER JOIN 최적화만 확인.
   */
  @Test
  void applicationRepository_findAll_전체_쿼리_상한_20_이내() {
    resetSessionAndStats();

    List<Application> all = applicationRepository.findAll();

    assertThat(all).isNotEmpty();
    long executedQueries = measureAndReport("ApplicationRepository.findAll()");
    assertThat(executedQueries)
        .as(
            "ApplicationRepository.findAll() 은 program·center EAGER 로드 시에도 쿼리 8 개 이하여야 한다 "
                + "(baseline 4 + 100% 여유). 초과 시 N+1 신호 → default_batch_fetch_size 또는 "
                + "@EntityGraph 도입 검토.")
        .isLessThanOrEqualTo(8L);
  }

  /**
   * BookmarkRepository.findAll() — Bookmark 도 A9-b 에서 program LAZY→EAGER 승격. N+1 회귀 감시.
   *
   * <p>verify #16.4 커버리지 확장 — Bookmark 진입점 미커버 지적 대응.
   */
  @Test
  void bookmarkRepository_findAll_전체_쿼리_상한_10_이내() {
    resetSessionAndStats();

    List<Bookmark> all = bookmarkRepository.findAll();

    long executedQueries = measureAndReport("BookmarkRepository.findAll()");
    assertThat(executedQueries)
        .as(
            "BookmarkRepository.findAll() 은 program·center EAGER 로드 시에도 쿼리 8 개 이하여야 한다 "
                + "(baseline 4 + 100% 여유). 초과 시 N+1 신호 → @EntityGraph 도입 검토.")
        .isLessThanOrEqualTo(8L);
  }

  /**
   * AdminDashboardService.load — 대시보드 진입 시 Application/Program 다중 조회 통합 쿼리 수 상한.
   *
   * <p>verify #16.4 커버리지 확장 — Dashboard 진입점 미커버 지적 대응. Dashboard 는 여러 findAll · countBy 조합이므로 상한을
   * 여유 있게 (50) 잡아 EAGER 승격으로 인한 폭발적 증가만 감지.
   */
  @Test
  void adminDashboardService_load_쿼리_상한_50_이내() {
    resetSessionAndStats();

    AdminDashboardService.DashboardModel model = adminDashboardService.load(null);

    assertThat(model).isNotNull();
    long executedQueries = measureAndReport("AdminDashboardService.load(scopeCenterId=null)");
    assertThat(executedQueries)
        .as(
            "AdminDashboardService.load 는 EAGER 승격 후에도 쿼리 35 개 이하여야 한다 "
                + "(baseline 27 + 30% 여유).")
        .isLessThanOrEqualTo(35L);
  }

  /**
   * AdminStatsService.load — 통계 페이지 진입 시 통합 쿼리 수 상한.
   *
   * <p>verify #16.4 커버리지 확장 — Stats 진입점 미커버 지적 대응. Stats 는 Application 전체 조회 + 집계 로직이라 상한을 60 으로 여유
   * 있게 잡음.
   */
  @Test
  void adminStatsService_load_쿼리_상한_60_이내() {
    resetSessionAndStats();

    AdminStatsService.StatsModel model = adminStatsService.load(null, "daily");

    assertThat(model).isNotNull();
    long executedQueries =
        measureAndReport("AdminStatsService.load(scopeCenterId=null, mode=daily)");
    assertThat(executedQueries)
        .as("AdminStatsService.load 는 EAGER 승격 후에도 쿼리 48 개 이하여야 한다 " + "(baseline 37 + 30% 여유).")
        .isLessThanOrEqualTo(48L);
  }
}

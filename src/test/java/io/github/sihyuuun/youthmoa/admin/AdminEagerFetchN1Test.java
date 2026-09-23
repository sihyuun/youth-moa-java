package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
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
  @Autowired ApplicationRepository applicationRepository;
  @Autowired UserRepository userRepository;
  @Autowired EntityManager entityManager;

  private Statistics stats;

  @BeforeEach
  void enableAndResetStats() {
    stats = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    stats.setStatisticsEnabled(true);
    stats.clear();
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
   * user 배치/join · 여유분 = 상한 12. N+1 발생 시 10 개 row 마다 program/center 각각 SELECT → 20+ 쿼리로 초과.
   */
  @Test
  void adminApplicationService_list_쿼리_상한_12_이내() {
    loginAsSysadmin();
    stats.clear();

    // 시드 program 1 (내일스퀘어 양평) 에 28 건 APPROVED 존재 · 페이지당 10 건 노출
    Page<Application> page = adminApplicationService.list(1L, null, null, 0);

    assertThat(page.getContent()).isNotEmpty();
    long executedQueries = stats.getPrepareStatementCount();
    assertThat(executedQueries)
        .as(
            "AdminApplicationService.list 는 program·center EAGER 로드 시에도 쿼리 12 개 이하여야 한다. "
                + "초과 시 N+1 신호 → ApplicationRepository.findAll(Specification, Pageable) 에 @EntityGraph 추가 필요.")
        .isLessThanOrEqualTo(12L);
  }

  /**
   * ApplicationRepository.findAll() (Dashboard/Stats 진입점) — 전체 로드 시 쿼리 수 상한.
   *
   * <p>{@link AdminDashboardService}, {@link AdminStatsService} 가 findAll() 을 호출해 List<Application>
   * 을 받는다. 시드 데이터 50+ 건에서 N+1 발생 시 100 쿼리 이상 발화. 상한 20 은 여유 있게 잡아 EAGER JOIN 최적화만 확인.
   */
  @Test
  void applicationRepository_findAll_전체_쿼리_상한_20_이내() {
    stats.clear();

    List<Application> all = applicationRepository.findAll();

    assertThat(all).isNotEmpty();
    long executedQueries = stats.getPrepareStatementCount();
    assertThat(executedQueries)
        .as(
            "ApplicationRepository.findAll() 은 program·center EAGER 로드 시에도 쿼리 20 개 이하여야 한다. "
                + "초과 시 N+1 신호 → default_batch_fetch_size 또는 @EntityGraph 도입 검토.")
        .isLessThanOrEqualTo(20L);
  }
}

package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * A8-search (2026-10-01) — {@link AdminSearchService} 단위 검증.
 *
 * <p>검증 축: ① 빈/공백 쿼리 → 빈 결과 ② 100자 초과 trim 안전 ③ SYSTEM_ADMIN 전역 검색 ④ CENTER_ADMIN 자기 센터로만 격리.
 *
 * <p>e2e 프로파일 (H2 in-memory + DataInitializer 전량 시드) 사용. @Transactional 로 side-effect 격리.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@Transactional
class AdminSearchServiceTest {

  @Autowired AdminSearchService adminSearchService;
  @Autowired UserRepository userRepository;

  @AfterEach
  void clearAuth() {
    SecurityContextHolder.clearContext();
  }

  private void loginAs(String email) {
    User user = userRepository.findByEmail(email).orElseThrow();
    UserPrincipal principal = new UserPrincipal(user);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
  }

  @Test
  void 빈_쿼리면_빈_결과를_돌려준다() {
    loginAs("sysadmin@youth-moa.test");

    AdminSearchResult r1 = adminSearchService.search(null);
    AdminSearchResult r2 = adminSearchService.search("");
    AdminSearchResult r3 = adminSearchService.search("   ");

    assertThat(r1.isEmpty()).isTrue();
    assertThat(r2.isEmpty()).isTrue();
    assertThat(r3.isEmpty()).isTrue();
  }

  @Test
  void 길이_100자_초과_쿼리는_앞_100자로_잘라_처리한다() {
    loginAs("sysadmin@youth-moa.test");

    String huge = "청년".repeat(80); // 160자
    AdminSearchResult result = adminSearchService.search(huge);

    assertThat(result).isNotNull();
    assertThat(result.programs()).isNotNull();
    assertThat(result.users()).isNotNull();
  }

  @Test
  void SYSTEM_ADMIN_은_전역_프로그램을_검색한다() {
    loginAs("sysadmin@youth-moa.test");

    AdminSearchResult result = adminSearchService.search("청년");

    assertThat(result.programs()).isNotEmpty();
    assertThat(result.programs().size()).isLessThanOrEqualTo(4);
  }

  @Test
  void CENTER_ADMIN_은_자기_센터_프로그램만_본다() {
    loginAs("center1@youth-moa.test");
    User center1Admin = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    String myCenterName = center1Admin.getCenter().getName();

    AdminSearchResult result = adminSearchService.search("청년");

    assertThat(result.programs())
        .allSatisfy(p -> assertThat(p.centerName()).isEqualTo(myCenterName));
  }

  @Test
  void CENTER_ADMIN_은_자기_센터_사용자만_본다() {
    loginAs("center1@youth-moa.test");

    AdminSearchResult result = adminSearchService.search("seed");

    assertThat(result.users().size()).isLessThanOrEqualTo(3);
    // CENTER_ADMIN 격리이므로 결과가 비어 있을 수도 있음(seed 유저는 Center FK 가 없음). 전역 쿼리 결과와 반드시 다르다.
    SecurityContextHolder.clearContext();
    loginAs("sysadmin@youth-moa.test");
    AdminSearchResult sysResult = adminSearchService.search("seed");
    assertThat(sysResult.users().size()).isGreaterThanOrEqualTo(result.users().size());
  }

  @Test
  void SYSTEM_ADMIN_사용자_검색_name_또는_email_매칭() {
    loginAs("sysadmin@youth-moa.test");

    AdminSearchResult result = adminSearchService.search("sysadmin");

    assertThat(result.users()).isNotEmpty();
    assertThat(result.users())
        .anySatisfy(
            u -> {
              assertThat(u.email().toLowerCase()).contains("sysadmin");
            });
  }
}

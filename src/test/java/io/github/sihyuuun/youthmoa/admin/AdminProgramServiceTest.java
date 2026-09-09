package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
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
 * A2 (2026-09-09) — AdminProgramService 통합 검증.
 *
 * <p>Specification 조합 (scope · keyword · status) 을 실제 JPA 쿼리로 실행해 필터 정확성 검증. e2e 프로파일 H2 + 시드 데이터
 * 활용.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@Transactional
class AdminProgramServiceTest {

  @Autowired AdminProgramService adminProgramService;
  @Autowired UserRepository userRepository;

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

  private void loginAsCenterAdmin() {
    User u = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(u);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CENTER_ADMIN"))));
  }

  // ================= 목록 =================

  @Test
  void list_sysadmin_returnsAllPrograms() {
    loginAsSysadmin();
    Page<Program> page = adminProgramService.list(null, null, 0);
    assertThat(page.getTotalElements()).isGreaterThan(0);
    assertThat(page.getContent()).isNotEmpty();
  }

  @Test
  void list_pageSize_10() {
    loginAsSysadmin();
    Page<Program> page = adminProgramService.list(null, null, 0);
    assertThat(page.getSize()).isEqualTo(AdminProgramService.ADMIN_PAGE_SIZE);
    assertThat(AdminProgramService.ADMIN_PAGE_SIZE).isEqualTo(10);
  }

  @Test
  void list_centerAdmin_filtersByOrganization() {
    loginAsCenterAdmin();
    User centerUser = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    String centerName = centerUser.getCenter().getName();

    Page<Program> page = adminProgramService.list(null, null, 0);
    // 모든 결과가 해당 센터 organization 이어야 함
    for (Program p : page.getContent()) {
      assertThat(p.getOrganization()).isEqualTo(centerName);
    }
  }

  @Test
  void list_statusFilter_OPEN_onlyActivePrograms() {
    loginAsSysadmin();
    Page<Program> page = adminProgramService.list(null, "OPEN", 0);
    // OPEN 은 isActive=true 인 프로그램만
    for (Program p : page.getContent()) {
      assertThat(p.isActive()).isTrue();
    }
  }

  @Test
  void list_statusFilter_SUSPENDED_onlyInactive() {
    loginAsSysadmin();
    Page<Program> page = adminProgramService.list(null, "SUSPENDED", 0);
    for (Program p : page.getContent()) {
      assertThat(p.isActive()).isFalse();
    }
  }

  @Test
  void list_statusFilter_invalidValue_returnsAll() {
    loginAsSysadmin();
    Page<Program> full = adminProgramService.list(null, null, 0);
    Page<Program> withGarbage = adminProgramService.list(null, "GARBAGE_XYZ", 0);
    assertThat(withGarbage.getTotalElements()).isEqualTo(full.getTotalElements());
  }

  @Test
  void list_keyword_returnsMatchingByTitle() {
    loginAsSysadmin();
    // 시드에 있는 프로그램 하나로 검색
    Page<Program> all = adminProgramService.list(null, null, 0);
    assertThat(all.getContent()).isNotEmpty();
    String firstTitle = all.getContent().get(0).getTitle();
    // title 첫 두 글자로 검색 → 최소 그 프로그램 hit
    String needle = firstTitle.substring(0, Math.min(2, firstTitle.length()));

    Page<Program> filtered = adminProgramService.list(needle, null, 0);
    assertThat(filtered.getTotalElements()).isGreaterThan(0);
  }

  @Test
  void list_keyword_noMatch_returnsEmpty() {
    loginAsSysadmin();
    Page<Program> page = adminProgramService.list("XYZ_NEVER_EXIST_QQQ_123", null, 0);
    assertThat(page.getTotalElements()).isEqualTo(0);
  }

  // ================= 상세 =================

  @Test
  void find_sysadmin_returnsAnyProgram() {
    loginAsSysadmin();
    Program p = adminProgramService.find(1L);
    assertThat(p).isNotNull();
    assertThat(p.getId()).isEqualTo(1L);
  }

  @Test
  void find_notFound_throwsIllegalArgument() {
    loginAsSysadmin();
    assertThatThrownBy(() -> adminProgramService.find(999999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("존재하지 않는 프로그램");
  }

  @Test
  void find_centerAdmin_wrongOrganization_throwsIllegalAccess() {
    loginAsCenterAdmin();
    User centerUser = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    String centerName = centerUser.getCenter().getName();

    // sysadmin 컨텍스트로 다른 센터 프로그램을 하나 찾음
    SecurityContextHolder.clearContext();
    loginAsSysadmin();
    Page<Program> allPrograms = adminProgramService.list(null, null, 0);
    Program other =
        allPrograms.getContent().stream()
            .filter(p -> !p.getOrganization().equals(centerName))
            .findFirst()
            .orElse(null);
    // 다른 organization 프로그램이 없다면 검증 스킵 (시드 특성상 여러 organization 존재해야 함)
    if (other == null) return;

    // 다시 CENTER_ADMIN 로 접근
    SecurityContextHolder.clearContext();
    loginAsCenterAdmin();
    Long otherId = other.getId();
    assertThatThrownBy(() -> adminProgramService.find(otherId))
        .isInstanceOf(IllegalAccessError.class);
  }
}

package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.application.WaitlistPromotionService;
import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.time.LocalDate;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * FOLLOW-waitlist-auto-approve (2026-10-08): {@link AdminApplicationService#forceCancel} / {@link
 * AdminApplicationService#reject} 에 끼운 승격 트리거 검증.
 *
 * <p>Q2 A 핵심: APPROVED→CANCELLED/REJECTED 만 공석을 만든다. PENDING→REJECTED/CANCELLED 는 승격 트리거 X.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@Transactional
class AdminApplicationServiceAutoApproveTest {

  @Autowired AdminApplicationService adminApplicationService;
  @Autowired ApplicationRepository applicationRepository;
  @Autowired ProgramRepository programRepository;
  @Autowired UserRepository userRepository;
  @Autowired CenterRepository centerRepository;

  private User user1;
  private User user2;
  private Program program;

  @AfterEach
  void clearAuth() {
    SecurityContextHolder.clearContext();
  }

  private void loginAsSysadmin() {
    User u = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(u);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, "N/A", principal.getAuthorities()));
  }

  @BeforeEach
  void seed() {
    LocalDate today = LocalDate.now();
    user1 =
        userRepository.save(
            User.builder()
                .email("waitlist-admin1@test.com")
                .password("hashed")
                .name("승인자")
                .role(UserRole.USER)
                .build());
    user2 =
        userRepository.save(
            User.builder()
                .email("waitlist-admin2@test.com")
                .password("hashed")
                .name("대기자")
                .role(UserRole.USER)
                .build());
    Center center =
        centerRepository
            .findByName("딴딴회관")
            .orElseGet(
                () ->
                    centerRepository.save(
                        Center.builder().name("딴딴회관").region("양평군").isActive(true).build()));
    User sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    program =
        programRepository.save(
            Program.builder()
                .title("관리자 승격 테스트")
                .center(center)
                .category("취업")
                .region("양평군")
                .content("c")
                .startDate(today.minusDays(5))
                .endDate(today.plusDays(30))
                .capacity(1)
                .createdBy(sysadmin)
                .autoApproveWhenFull(true)
                .build());
  }

  private Application seedApproved(User u) {
    Application a =
        applicationRepository.save(
            Application.builder().user(u).program(program).applyReason("승인됨").build());
    User sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    a.approve(sysadmin);
    return a;
  }

  private Application seedPending(User u) {
    return applicationRepository.save(
        Application.builder().user(u).program(program).applyReason("대기").build());
  }

  @Test
  @DisplayName("AdminApplicationService.reject: APPROVED→REJECTED 시 PENDING 1건 승격")
  void admin_reject_approved_promotes_pending() {
    loginAsSysadmin();
    Application approved = seedApproved(user1);
    Application pending = seedPending(user2);

    adminApplicationService.reject(
        program.getId(), approved.getId(), "sysadmin@youth-moa.test", "자격 미달");

    Application reloaded = applicationRepository.findById(pending.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
    assertThat(reloaded.getAdminNote()).isEqualTo(WaitlistPromotionService.PROMOTION_NOTE);
  }

  @Test
  @DisplayName("AdminApplicationService.forceCancel: APPROVED→CANCELLED 시 PENDING 1건 승격")
  void admin_forceCancel_approved_promotes_pending() {
    loginAsSysadmin();
    Application approved = seedApproved(user1);
    Application pending = seedPending(user2);

    adminApplicationService.forceCancel(
        program.getId(), approved.getId(), "sysadmin@youth-moa.test", "관리자 사정");

    Application reloaded = applicationRepository.findById(pending.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
    assertThat(reloaded.getProcessedBy()).isNotNull();
    assertThat(reloaded.getProcessedBy().getEmail())
        .isEqualTo(WaitlistPromotionService.SYSTEM_ADMIN_EMAIL);
  }

  @Test
  @DisplayName("AdminApplicationService.reject: PENDING→REJECTED 는 승격 트리거 X (공석 생성 안 함)")
  void admin_reject_pending_doesNotPromote() {
    loginAsSysadmin();
    Application approved = seedApproved(user1); // 자리 차지
    Application pending1 = seedPending(user2);
    User user3 =
        userRepository.save(
            User.builder()
                .email("waitlist-admin3@test.com")
                .password("hashed")
                .name("대기2")
                .role(UserRole.USER)
                .build());
    Application pending2 = seedPending(user3);

    adminApplicationService.reject(
        program.getId(), pending1.getId(), "sysadmin@youth-moa.test", "자격 미달");

    // pending1 은 REJECTED, 다른 pending 은 PENDING 유지 (approved 가 그대로이므로 공석 없음)
    Application p2 = applicationRepository.findById(pending2.getId()).orElseThrow();
    assertThat(p2.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    Application a = applicationRepository.findById(approved.getId()).orElseThrow();
    assertThat(a.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
  }

  // 미사용 import 방지
  @SuppressWarnings("unused")
  private static final Object UNUSED = Collections.emptyList();
}

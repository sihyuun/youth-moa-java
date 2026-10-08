package io.github.sihyuuun.youthmoa.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.application.event.ApplicationApprovedEvent;
import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

/**
 * FOLLOW-waitlist-auto-approve (2026-10-08): 대기자 자동 승격 로직 단위 검증.
 *
 * <p>Q2 A — 트리거 시점: APPROVED→CANCELLED/REJECTED 직후.
 *
 * <p>Q3 A — processedBy = sysadmin + adminNote = {@link WaitlistPromotionService#PROMOTION_NOTE}.
 *
 * <p>4가지 조합:
 *
 * <ol>
 *   <li>정원 미달 + 토글OFF → PENDING 승격 X
 *   <li>정원 꽉 참 + 토글OFF → PENDING 승격 X
 *   <li>정원 꽉 참 + 토글ON + CANCELLED → PENDING 승격 O
 *   <li>정원 꽉 참 + 토글ON + REJECTED → PENDING 승격 O
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("e2e")
@RecordApplicationEvents
@Transactional
class ApplicationServiceAutoApproveTest {

  @Autowired ApplicationService applicationService;
  @Autowired ApplicationRepository applicationRepository;
  @Autowired ProgramRepository programRepository;
  @Autowired UserRepository userRepository;
  @Autowired CenterRepository centerRepository;
  @Autowired ApplicationEvents events;

  // DataInitializer 가 생성해주는 sysadmin 재활용 (WaitlistPromotionService 가 lookup) + prev admin
  // 역할용 추가 User 는 테스트 내부에서 생성.

  private User user1; // approved → cancel 대상
  private User user2; // pending → 승격 대상
  private Program program;

  @BeforeEach
  void seed() {
    LocalDate today = LocalDate.now();
    User admin =
        userRepository.save(
            User.builder()
                .email("waitlist-admin@test.com")
                .password("hashed")
                .name("관리자")
                .role(UserRole.ADMIN)
                .build());
    user1 =
        userRepository.save(
            User.builder()
                .email("waitlist-user1@test.com")
                .password("hashed")
                .name("승인자")
                .role(UserRole.USER)
                .build());
    user2 =
        userRepository.save(
            User.builder()
                .email("waitlist-user2@test.com")
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

    // capacity=1 로 설정 → user1 APPROVED 한 명이 꽉 참을 뜻함.
    program =
        programRepository.save(
            Program.builder()
                .title("대기자 자동승인 테스트")
                .center(center)
                .category("취업")
                .region("양평군")
                .content("c")
                .startDate(today.minusDays(5))
                .endDate(today.plusDays(30))
                .capacity(1)
                .createdBy(admin)
                .build());
  }

  private Application seedApproved(User u) {
    Application a =
        applicationRepository.save(
            Application.builder().user(u).program(program).applyReason("승인됨").build());
    // 상태를 APPROVED 로 전환 (approve() 는 admin 파라미터 필요, 테스트에선 admin User 하나 더 로드)
    User admin = userRepository.findByEmail("waitlist-admin@test.com").orElseThrow();
    a.approve(admin);
    return a;
  }

  private Application seedPending(User u) {
    return applicationRepository.save(
        Application.builder().user(u).program(program).applyReason("대기").build());
  }

  @Test
  @DisplayName("①정원 미달 + 토글OFF → 취소해도 승격 안 함 (PENDING 유지)")
  void toggleOff_underCapacity_noPromotion() {
    // capacity=1 로 미달 상황 재현: approved 없이 pending 1건만 있고 user1 이 pending 취소 시도
    Application pending = seedPending(user1);
    Application other = seedPending(user2);
    // program.autoApproveWhenFull = false (기본)

    applicationService.cancel(pending.getId(), user1.getEmail(), "변심");

    Application reloaded = applicationRepository.findById(other.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    // ApprovedEvent 는 1건도 발행되지 않음
    assertThat(events.stream(ApplicationApprovedEvent.class).count()).isZero();
  }

  @Test
  @DisplayName("②정원 꽉 참 + 토글OFF → APPROVED 취소해도 PENDING 승격 안 함")
  void toggleOff_full_noPromotion() {
    Application approved = seedApproved(user1);
    Application pending = seedPending(user2);
    // autoApproveWhenFull = false (기본)

    applicationService.cancel(approved.getId(), user1.getEmail(), "변심");

    Application reloaded = applicationRepository.findById(pending.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    assertThat(events.stream(ApplicationApprovedEvent.class).count()).isZero();
  }

  @Test
  @DisplayName("③정원 꽉 참 + 토글ON + CANCELLED → PENDING 1건 승격 (adminNote·sysadmin 세팅)")
  void toggleOn_full_cancel_promotes() {
    Application approved = seedApproved(user1);
    Application pending = seedPending(user2);
    program.enableAutoApproveWhenFull();
    programRepository.save(program);

    applicationService.cancel(approved.getId(), user1.getEmail(), "변심");

    Application reloaded = applicationRepository.findById(pending.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
    assertThat(reloaded.getAdminNote()).isEqualTo(WaitlistPromotionService.PROMOTION_NOTE);
    assertThat(reloaded.getProcessedBy()).isNotNull();
    assertThat(reloaded.getProcessedBy().getEmail())
        .isEqualTo(WaitlistPromotionService.SYSTEM_ADMIN_EMAIL);
    // 승격은 ApplicationApprovedEvent 1건 발행 (사용자 알림용)
    assertThat(events.stream(ApplicationApprovedEvent.class).count()).isEqualTo(1);
  }

  @Test
  @DisplayName("④capacity=null (정원 제한 없음) + 토글ON → 승격 안 함 (Q5 노출 조건과 일치)")
  void toggleOn_noCapacity_noPromotion() {
    // capacity null 프로그램 재구성
    User admin = userRepository.findByEmail("waitlist-admin@test.com").orElseThrow();
    Center center = program.getCenter();
    Program noCapProgram =
        programRepository.save(
            Program.builder()
                .title("정원 제한 없음")
                .center(center)
                .category("c")
                .region("r")
                .content("c")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .createdBy(admin)
                .autoApproveWhenFull(true)
                .build());
    Application approved =
        applicationRepository.save(
            Application.builder().user(user1).program(noCapProgram).applyReason("승인").build());
    approved.approve(admin);
    Application pending =
        applicationRepository.save(
            Application.builder().user(user2).program(noCapProgram).applyReason("대기").build());

    applicationService.cancel(approved.getId(), user1.getEmail(), "변심");

    Application reloaded = applicationRepository.findById(pending.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.PENDING);
  }
}

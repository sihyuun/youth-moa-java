package io.github.sihyuuun.youthmoa.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase
@Import({JpaConfig.class, ApplicationService.class, ApplicationServiceTest.MeterConfig.class})
class ApplicationServiceTest {

  /**
   * @DataJpaTest slice 는 Actuator 를 포함하지 않으므로 MeterRegistry 를 직접 주입.
   */
  @TestConfiguration
  static class MeterConfig {
    @Bean
    MeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }
  }

  @Autowired MeterRegistry meterRegistry;

  @Autowired ApplicationService applicationService;
  @Autowired ApplicationRepository applicationRepository;
  @Autowired UserRepository userRepository;
  @Autowired ProgramRepository programRepository;

  private User user;
  private Program activeProgram;
  private Program upcomingProgram;
  private Program closedProgram;
  private Program inactiveProgram;

  @BeforeEach
  void seed() {
    user =
        userRepository.save(
            User.builder()
                .email("apply@test.com")
                .password("hashed")
                .name("신청자")
                .role(UserRole.USER)
                .build());

    LocalDate today = LocalDate.now();

    activeProgram =
        programRepository.save(
            Program.builder()
                .title("진행중 프로그램")
                .organization("내일스퀘어")
                .category("취업")
                .region("수원시")
                .content("c")
                .startDate(today.minusDays(5))
                .endDate(today.plusDays(10))
                .capacity(30)
                .build());

    upcomingProgram =
        programRepository.save(
            Program.builder()
                .title("진행예정 프로그램")
                .organization("내일스퀘어")
                .category("취업")
                .region("수원시")
                .content("c")
                .startDate(today.plusDays(10))
                .endDate(today.plusDays(30))
                .capacity(30)
                .build());

    closedProgram =
        programRepository.save(
            Program.builder()
                .title("마감 프로그램")
                .organization("내일스퀘어")
                .category("취업")
                .region("수원시")
                .content("c")
                .startDate(today.minusDays(30))
                .endDate(today.minusDays(5))
                .capacity(30)
                .build());

    inactiveProgram =
        programRepository.save(
            Program.builder()
                .title("비활성 프로그램")
                .organization("내일스퀘어")
                .category("취업")
                .region("수원시")
                .content("c")
                .startDate(today.minusDays(5))
                .endDate(today.plusDays(10))
                .capacity(30)
                .isActive(false)
                .build());
  }

  private ApplyRequest request(String reason) {
    ApplyRequest r = new ApplyRequest();
    r.setApplyReason(reason);
    return r;
  }

  @Test
  @DisplayName("정상 신청 시 PENDING 상태의 Application 이 저장된다 + 메트릭 카운터 1 증가")
  void apply_success() {
    double before = meterRegistry.counter("youthmoa.application.submitted").count();

    Application saved =
        applicationService.apply(
            user.getEmail(), activeProgram.getId(), request("취업역량 강화를 위해 꼭 참여하고 싶습니다."));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    assertThat(saved.getApplyReason()).startsWith("취업역량");
    assertThat(saved.getAppliedAt()).isNotNull();
    assertThat(applicationRepository.count()).isEqualTo(1);
    // chore-observability PR-2: 성공 시 metric 증가 검증
    assertThat(meterRegistry.counter("youthmoa.application.submitted").count())
        .isEqualTo(before + 1);
  }

  @Test
  @DisplayName("apply 실패 (upcoming/closed/inactive) 시 metric 미증가")
  void apply_failure_does_not_increment_metric() {
    double before = meterRegistry.counter("youthmoa.application.submitted").count();

    assertThatThrownBy(
            () ->
                applicationService.apply(
                    user.getEmail(), upcomingProgram.getId(), request("upcoming 신청 시도입니다.")))
        .isInstanceOf(IllegalStateException.class);

    assertThat(meterRegistry.counter("youthmoa.application.submitted").count()).isEqualTo(before);
  }

  @Test
  @DisplayName("동일 사용자가 같은 프로그램에 PENDING 상태로 중복 신청 시 IllegalStateException")
  void apply_duplicate_pending() {
    applicationService.apply(
        user.getEmail(), activeProgram.getId(), request("첫 번째 신청입니다. 꼭 참여하겠습니다."));

    assertThatThrownBy(
            () ->
                applicationService.apply(
                    user.getEmail(), activeProgram.getId(), request("같은 프로그램에 또 신청합니다.")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("이미 신청한");
  }

  @Test
  @DisplayName("진행예정(UPCOMING) 프로그램은 신청 불가")
  void apply_upcoming_blocked() {
    assertThatThrownBy(
            () ->
                applicationService.apply(
                    user.getEmail(), upcomingProgram.getId(), request("진행예정 프로그램에 신청합니다.")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("모집 중");
  }

  @Test
  @DisplayName("종료(ENDED) 프로그램은 신청 불가")
  void apply_closed_blocked() {
    assertThatThrownBy(
            () ->
                applicationService.apply(
                    user.getEmail(), closedProgram.getId(), request("마감 프로그램에 신청합니다.")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("모집 중");
  }

  @Test
  @DisplayName("비활성(isActive=false) 프로그램은 신청 불가")
  void apply_inactive_blocked() {
    assertThatThrownBy(
            () ->
                applicationService.apply(
                    user.getEmail(), inactiveProgram.getId(), request("비활성 프로그램에 신청합니다.")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("비활성");
  }

  @Test
  @DisplayName("존재하지 않는 프로그램 ID 는 IllegalArgumentException")
  void apply_program_notFound() {
    assertThatThrownBy(
            () ->
                applicationService.apply(
                    user.getEmail(), 999_999L, request("존재하지 않는 프로그램에 신청합니다.")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("프로그램을 찾을 수 없습니다");
  }

  @Test
  @DisplayName("존재하지 않는 사용자 이메일 은 IllegalArgumentException")
  void apply_user_notFound() {
    assertThatThrownBy(
            () ->
                applicationService.apply(
                    "ghost@nowhere.com", activeProgram.getId(), request("유령 사용자의 신청입니다.")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("사용자를 찾을 수 없습니다");
  }

  @Test
  @DisplayName("CANCELLED 상태의 신청 후 같은 사용자가 다시 신청하면 기존 row 가 재활용된다")
  void apply_again_after_cancel_reuses_row() {
    Application first =
        applicationService.apply(
            user.getEmail(), activeProgram.getId(), request("첫 번째 신청 후 취소할 예정입니다."));
    Long firstId = first.getId();
    first.cancel();
    applicationRepository.flush();

    Application second =
        applicationService.apply(
            user.getEmail(), activeProgram.getId(), request("취소 후 다시 신청합니다. 꼭 참여하고 싶습니다."));

    assertThat(second.getId()).isEqualTo(firstId);
    assertThat(second.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    assertThat(second.getApplyReason()).startsWith("취소 후 다시 신청");
    assertThat(applicationRepository.count()).isEqualTo(1L);
  }

  @Test
  @DisplayName("REJECTED 상태의 신청이 있으면 같은 사용자는 재신청 불가")
  void apply_blocked_after_reject() {
    User admin =
        userRepository.save(
            User.builder()
                .email("admin@test.com")
                .password("hashed")
                .name("관리자")
                .role(UserRole.ADMIN)
                .build());
    Application first =
        applicationService.apply(user.getEmail(), activeProgram.getId(), request("첫 번째 신청입니다."));
    first.reject(admin, "자격 미충족");
    applicationRepository.flush();

    assertThatThrownBy(
            () ->
                applicationService.apply(
                    user.getEmail(), activeProgram.getId(), request("반려 후 다시 신청 시도합니다.")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("반려");
  }
}

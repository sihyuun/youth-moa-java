package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
 * A4 admin-program-detail (2026-09-15 · Qn 24건 A) — {@link AdminApplicationService} 통합 검증.
 *
 * <p>A3-2 학습 반영: verify FAIL 원인이 "신규 로직 테스트 자산 부재" 였으므로, A4 QA 세션에서 Service Test 를 필수 신설한다. e2e
 * 프로파일 H2 + 시드 데이터 활용. {@link SecurityContextHolder} 로 AdminScope 를 실제 라우팅한다.
 *
 * <p>시드 (DataInitializer): program 1 (organization="내일스퀘어 양평", capacity=30) 에 28건 APPROVED. program
 * 2 에 19건 PENDING. program 3 에 6건 PENDING. center1_admin 은 centers[0] ("28청춘창업소" or "내일꿈제작소" 등 CSV
 * 알파벳 첫번째) 소속.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@Transactional
class AdminApplicationServiceTest {

  @Autowired AdminApplicationService adminApplicationService;
  @Autowired ApplicationRepository applicationRepository;
  @Autowired ProgramRepository programRepository;
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

  private void loginAsCenterAdmin1() {
    User u = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(u);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CENTER_ADMIN"))));
  }

  private Long firstProgramId() {
    return programRepository.findAll().stream()
        .findFirst()
        .map(Program::getId)
        .orElseThrow(() -> new IllegalStateException("no seeded program"));
  }

  // ================= assertProgramInScope =================

  @Test
  void assertProgramInScope_sysadmin_allowsAny() {
    loginAsSysadmin();
    Program p = adminApplicationService.assertProgramInScope(firstProgramId());
    assertThat(p.getId()).isEqualTo(firstProgramId());
  }

  @Test
  void assertProgramInScope_missingId_throwsIllegalArgument() {
    loginAsSysadmin();
    assertThatThrownBy(() -> adminApplicationService.assertProgramInScope(999_999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("존재하지 않는");
  }

  @Test
  void assertProgramInScope_centerAdmin_otherCenterProgram_denied() {
    loginAsCenterAdmin1();
    // center1 은 centers[0] 소속. seed 데이터의 program 1 (organization="내일스퀘어 양평") 는 centers[0] 와 무관.
    // program 이 center1 조직과 매칭되지 않으면 IllegalAccessError.
    User centerAdmin = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    // A9-a: Center FK 기반으로 격리 검증
    Long centerId = centerAdmin.getCenter().getId();
    Program otherProgram =
        programRepository.findAll().stream()
            .filter(p -> p.getCenter() == null || !centerId.equals(p.getCenter().getId()))
            .findFirst()
            .orElseThrow();
    assertThatThrownBy(() -> adminApplicationService.assertProgramInScope(otherProgram.getId()))
        .isInstanceOf(IllegalAccessError.class)
        .hasMessageContaining("자신의 센터");
  }

  // ================= 목록 =================

  @Test
  void list_sysadmin_returnsAppliedApplications() {
    loginAsSysadmin();
    Long pid = firstProgramId();
    Page<Application> page = adminApplicationService.list(pid, null, null, 0);
    // 시드: program 1 은 28 APPROVED
    assertThat(page.getTotalElements()).isGreaterThan(0);
  }

  @Test
  void list_pageSize_matches_constant() {
    loginAsSysadmin();
    Page<Application> page = adminApplicationService.list(firstProgramId(), null, null, 0);
    assertThat(page.getSize()).isEqualTo(AdminApplicationService.PAGE_SIZE);
  }

  @Test
  void list_statusFilter_APPROVED_isolatesRows() {
    loginAsSysadmin();
    Long pid = firstProgramId();
    Page<Application> approved = adminApplicationService.list(pid, "APPROVED", null, 0);
    assertThat(approved.getContent())
        .allSatisfy(a -> assertThat(a.getStatus()).isEqualTo(ApplicationStatus.APPROVED));
  }

  @Test
  void list_statusFilter_invalidValue_ignored() {
    loginAsSysadmin();
    Long pid = firstProgramId();
    // 잘못된 status 값은 필터 미적용 (Service 구현)
    Page<Application> all = adminApplicationService.list(pid, "BOGUS_STATUS", null, 0);
    Page<Application> noFilter = adminApplicationService.list(pid, null, null, 0);
    assertThat(all.getTotalElements()).isEqualTo(noFilter.getTotalElements());
  }

  @Test
  void list_sortedByAppliedAt_desc() {
    loginAsSysadmin();
    Page<Application> page = adminApplicationService.list(firstProgramId(), null, null, 0);
    // 첫번째 페이지 결과는 appliedAt DESC 정렬이어야 함
    if (page.getContent().size() >= 2) {
      Application first = page.getContent().get(0);
      Application second = page.getContent().get(1);
      assertThat(first.getAppliedAt()).isAfterOrEqualTo(second.getAppliedAt());
    }
  }

  // ================= 요약 카운트 =================

  @Test
  void summaryCounts_returnsAllFourStatuses() {
    loginAsSysadmin();
    Map<ApplicationStatus, Long> summary = adminApplicationService.summaryCounts(firstProgramId());
    assertThat(summary)
        .containsKeys(
            ApplicationStatus.PENDING,
            ApplicationStatus.APPROVED,
            ApplicationStatus.REJECTED,
            ApplicationStatus.CANCELLED);
  }

  @Test
  void totalCount_matchesRepositoryDirect() {
    loginAsSysadmin();
    Long pid = firstProgramId();
    long total = adminApplicationService.totalCount(pid);
    long direct = applicationRepository.countByProgramId(pid);
    assertThat(total).isEqualTo(direct);
  }

  // ================= visits N+1 방지 =================

  @Test
  void visitsByUserIds_empty_returnsEmptyMap() {
    loginAsSysadmin();
    Map<Long, Long> visits = adminApplicationService.visitsByUserIds(List.of());
    assertThat(visits).isEmpty();
  }

  @Test
  void visitsByUserIds_bulkQueryOnlyCountsApproved() {
    loginAsSysadmin();
    // seed: user1 has 1 APPROVED (program 1). Should return 1.
    List<Application> approvedApps =
        applicationRepository.findAll().stream()
            .filter(a -> a.getStatus() == ApplicationStatus.APPROVED)
            .toList();
    if (approvedApps.isEmpty()) return;
    Long userId = approvedApps.get(0).getUser().getId();
    Map<Long, Long> visits = adminApplicationService.visitsByUserIds(List.of(userId));
    assertThat(visits.get(userId)).isGreaterThanOrEqualTo(1L);
  }

  // ================= 상태 변경 =================

  @Test
  void approve_pending_transitionsToApproved() {
    loginAsSysadmin();
    Application pending = firstApplicationByStatus(ApplicationStatus.PENDING);
    if (pending == null) return;
    Long pid = pending.getProgram().getId();
    adminApplicationService.approve(pid, pending.getId(), "sysadmin@youth-moa.test");
    Application after = applicationRepository.findById(pending.getId()).orElseThrow();
    assertThat(after.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
    assertThat(after.getProcessedBy()).isNotNull();
    assertThat(after.getProcessedAt()).isNotNull();
  }

  @Test
  void approve_alreadyApproved_idempotentNoop() {
    loginAsSysadmin();
    Application approved = firstApplicationByStatus(ApplicationStatus.APPROVED);
    if (approved == null) return;
    // idempotent: 재호출 시 예외 발생 안 함
    adminApplicationService.approve(
        approved.getProgram().getId(), approved.getId(), "sysadmin@youth-moa.test");
    Application after = applicationRepository.findById(approved.getId()).orElseThrow();
    assertThat(after.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
  }

  @Test
  void reject_withReason_transitionsToRejected() {
    loginAsSysadmin();
    Application pending = firstApplicationByStatus(ApplicationStatus.PENDING);
    if (pending == null) return;
    adminApplicationService.reject(
        pending.getProgram().getId(), pending.getId(), "sysadmin@youth-moa.test", "정원 초과입니다");
    Application after = applicationRepository.findById(pending.getId()).orElseThrow();
    assertThat(after.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    assertThat(after.getRejectReason()).contains("정원 초과");
  }

  @Test
  void reject_blankReason_throwsIllegalArgument() {
    loginAsSysadmin();
    Application pending = firstApplicationByStatus(ApplicationStatus.PENDING);
    if (pending == null) return;
    assertThatThrownBy(
            () ->
                adminApplicationService.reject(
                    pending.getProgram().getId(),
                    pending.getId(),
                    "sysadmin@youth-moa.test",
                    "   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("반려 사유");
  }

  @Test
  void reject_nullReason_throwsIllegalArgument() {
    loginAsSysadmin();
    Application pending = firstApplicationByStatus(ApplicationStatus.PENDING);
    if (pending == null) return;
    assertThatThrownBy(
            () ->
                adminApplicationService.reject(
                    pending.getProgram().getId(), pending.getId(), "sysadmin@youth-moa.test", null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void forceCancel_withReason_prefixesReason_andSetsProcessedBy() {
    loginAsSysadmin();
    Application pending = firstApplicationByStatus(ApplicationStatus.PENDING);
    if (pending == null) return;
    adminApplicationService.forceCancel(
        pending.getProgram().getId(), pending.getId(), "sysadmin@youth-moa.test", "중복 신청");
    Application after = applicationRepository.findById(pending.getId()).orElseThrow();
    assertThat(after.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
    // Qn-C A: "관리자 취소: " 접두어
    assertThat(after.getCancelReason()).startsWith("관리자 취소: ");
    assertThat(after.getCancelReason()).contains("중복 신청");
    assertThat(after.getProcessedBy()).isNotNull();
  }

  @Test
  void forceCancel_blankReason_throwsIllegalArgument() {
    loginAsSysadmin();
    Application pending = firstApplicationByStatus(ApplicationStatus.PENDING);
    if (pending == null) return;
    assertThatThrownBy(
            () ->
                adminApplicationService.forceCancel(
                    pending.getProgram().getId(), pending.getId(), "sysadmin@youth-moa.test", ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("취소 사유");
  }

  // ================= 담당자 의견 =================

  @Test
  void updateNote_savesNote_statusUnchanged() {
    loginAsSysadmin();
    Application app = applicationRepository.findAll().stream().findFirst().orElseThrow();
    ApplicationStatus before = app.getStatus();
    adminApplicationService.updateNote(app.getProgram().getId(), app.getId(), "확인 완료");
    Application after = applicationRepository.findById(app.getId()).orElseThrow();
    assertThat(after.getAdminNote()).isEqualTo("확인 완료");
    assertThat(after.getStatus()).isEqualTo(before);
  }

  @Test
  void updateNote_blank_clearsNote() {
    loginAsSysadmin();
    Application app = applicationRepository.findAll().stream().findFirst().orElseThrow();
    adminApplicationService.updateNote(app.getProgram().getId(), app.getId(), "TEMP");
    adminApplicationService.updateNote(app.getProgram().getId(), app.getId(), "   ");
    Application after = applicationRepository.findById(app.getId()).orElseThrow();
    assertThat(after.getAdminNote()).isNull();
  }

  @Test
  void updateNote_over1000chars_rejected() {
    loginAsSysadmin();
    Application app = applicationRepository.findAll().stream().findFirst().orElseThrow();
    String tooLong = "가".repeat(1001);
    assertThatThrownBy(
            () ->
                adminApplicationService.updateNote(app.getProgram().getId(), app.getId(), tooLong))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("1000자");
  }

  // ================= 라벨 매핑 =================

  @Test
  void labelOf_koreanMapping() {
    assertThat(AdminApplicationService.labelOf(ApplicationStatus.PENDING)).isEqualTo("대기");
    assertThat(AdminApplicationService.labelOf(ApplicationStatus.APPROVED)).isEqualTo("승인");
    assertThat(AdminApplicationService.labelOf(ApplicationStatus.REJECTED)).isEqualTo("반려");
    assertThat(AdminApplicationService.labelOf(ApplicationStatus.CANCELLED)).isEqualTo("취소");
    assertThat(AdminApplicationService.labelOf(null)).isEqualTo("");
  }

  @Test
  void statusOptions_containsAllFour() {
    List<ApplicationStatus> options = adminApplicationService.statusOptions();
    assertThat(options)
        .containsExactly(
            ApplicationStatus.PENDING,
            ApplicationStatus.APPROVED,
            ApplicationStatus.REJECTED,
            ApplicationStatus.CANCELLED);
  }

  // ================= helper =================

  private Application firstApplicationByStatus(ApplicationStatus status) {
    return applicationRepository.findAll().stream()
        .filter(a -> a.getStatus() == status)
        .findFirst()
        .orElse(null);
  }
}

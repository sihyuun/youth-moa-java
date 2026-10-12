package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * A5-2 admin-approval-bulk (2026-10-12): {@link AdminApplicationBulkService#bulkApproveAllPending}
 * 검증.
 *
 * <ul>
 *   <li>PENDING 전체를 APPROVED 로 전이
 *   <li>PENDING 이 없으면 total=0
 *   <li>이미 처리된 신청은 영향 없음
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("e2e")
class AdminApplicationBulkApproveAllPendingTest {

  @Autowired AdminApplicationBulkService bulkService;
  @Autowired ApplicationRepository applicationRepository;

  @Test
  void approveAllPending_covers_all_pending_in_program() {
    // program 3 (시드 규약: 전부 PENDING) 전체 승인 시도
    List<Application> pendingBefore =
        applicationRepository.findAll().stream()
            .filter(a -> a.getProgram().getId() == 3L)
            .filter(a -> a.getStatus() == ApplicationStatus.PENDING)
            .toList();
    if (pendingBefore.isEmpty()) return;

    BulkResult result =
        bulkService.bulkApproveAllPending(3L, "sysadmin@youth-moa.test");
    assertThat(result.getTotal()).isEqualTo(pendingBefore.size());
    assertThat(result.getSuccessCount()).isEqualTo(pendingBefore.size());

    // 전부 APPROVED 로 전환됐는지 재확인
    long stillPending =
        applicationRepository.findAll().stream()
            .filter(a -> a.getProgram().getId() == 3L)
            .filter(a -> a.getStatus() == ApplicationStatus.PENDING)
            .count();
    assertThat(stillPending).isZero();
  }

  @Test
  void approveAllPending_no_pending_returns_zero() {
    // 모든 신청을 승인한 뒤 다시 호출하면 total=0
    bulkService.bulkApproveAllPending(3L, "sysadmin@youth-moa.test");
    BulkResult second = bulkService.bulkApproveAllPending(3L, "sysadmin@youth-moa.test");
    assertThat(second.getTotal()).isZero();
  }
}

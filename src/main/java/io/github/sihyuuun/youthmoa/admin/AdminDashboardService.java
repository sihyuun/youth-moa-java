package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.program.ProgramStatus;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A1 대시보드 데이터 소스. 스탯 카드 4개 + 승인 대기 + 최근 프로그램 + 마감 임박.
 *
 * <p>{@link ProgramStatus} 는 파생값 (isActive · start/endDate + today 로 계산) 이라 Repository 에서 status 로 필터
 * 쿼리가 불가능하다. A1 은 학습 단계라 findAll() 로드 후 Java 스트림으로 필터한다. 프로그램 규모가 커지면 A2/A6 에서 상태 컬럼 도입·인덱스
 * 최적화 필요 (deferred).
 *
 * <p>센터 격리는 {@code Program.organization = Center.name} 문자열 매칭 (Q7 근사). scope==null 이면 전체.
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

  private final ProgramRepository programRepository;
  private final ApplicationRepository applicationRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public DashboardModel load(String scopeCenterName) {
    List<Program> scoped = scopedPrograms(scopeCenterName);

    long active = scoped.stream().filter(p -> p.getStatus() == ProgramStatus.OPEN).count();
    long closed = scoped.stream().filter(p -> p.getStatus() == ProgramStatus.ENDED).count();
    long upcoming = scoped.stream().filter(p -> p.getStatus() == ProgramStatus.UPCOMING).count();

    long totalUsers;
    if (scopeCenterName == null) {
      // SYSTEM_ADMIN: 전체 USER 수
      totalUsers = userRepository.findAll().stream().filter(u -> u.getRole() == UserRole.USER).count();
    } else {
      // CENTER_ADMIN: 자기 센터 소속 USER 만 (Q8 근사 — User.center.name 매칭).
      final String cn = scopeCenterName;
      totalUsers =
          userRepository.findAll().stream()
              .filter(u -> u.getRole() == UserRole.USER)
              .filter(u -> u.getCenter() != null && cn.equals(u.getCenter().getName()))
              .count();
    }

    long pending = countPendingApplications(scoped);

    List<Program> recent =
        scoped.stream()
            .sorted(Comparator.comparing(Program::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(5)
            .toList();

    // 마감 임박: endDate 가 오늘~+7 이내 (A3 에서 applyEndDate 도입 시 그것으로 교체 · deferred)
    LocalDate today = LocalDate.now();
    LocalDate cutoff = today.plusDays(7);
    List<Program> urgent =
        scoped.stream()
            .filter(p -> p.getEndDate() != null)
            .filter(p -> !p.getEndDate().isBefore(today) && !p.getEndDate().isAfter(cutoff))
            .sorted(Comparator.comparing(Program::getEndDate))
            .limit(5)
            .toList();

    return DashboardModel.builder()
        .activeCount(active)
        .closedCount(closed)
        .upcomingCount(upcoming)
        .totalUsers(totalUsers)
        .pendingCount(pending)
        .recentPrograms(recent)
        .urgentPrograms(urgent)
        .build();
  }

  private List<Program> scopedPrograms(String scopeCenterName) {
    Stream<Program> all = programRepository.findAll().stream();
    if (scopeCenterName != null) {
      final String cn = scopeCenterName;
      all = all.filter(p -> cn.equals(p.getOrganization()));
    }
    return all.toList();
  }

  private long countPendingApplications(List<Program> scoped) {
    // Application 은 Program FK 를 가짐. scope 안의 프로그램 id set 을 갖고 count.
    if (scoped.isEmpty()) return 0L;
    // 학습 단계 단순 구현: findAll 후 stream. 규모 커지면 커스텀 쿼리로 승격 (deferred).
    List<Long> ids = scoped.stream().map(Program::getId).toList();
    List<Application> all = applicationRepository.findAll();
    return all.stream()
        .filter(a -> a.getStatus() == ApplicationStatus.PENDING)
        .filter(a -> a.getProgram() != null && ids.contains(a.getProgram().getId()))
        .count();
  }

  @Getter
  @Builder
  public static class DashboardModel {
    private long activeCount;
    private long closedCount;
    private long upcomingCount;
    private long totalUsers;
    private long pendingCount;
    private List<Program> recentPrograms;
    private List<Program> urgentPrograms;
  }
}

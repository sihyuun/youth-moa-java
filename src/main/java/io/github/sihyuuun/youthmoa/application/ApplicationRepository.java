package io.github.sihyuuun.youthmoa.application;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByUserAndProgram(User user, Program program);

    boolean existsByUserAndProgramAndStatusIn(User user, Program program, List<ApplicationStatus> statuses);

    Page<Application> findAllByUser(User user, Pageable pageable);

    long countByProgramAndStatusIn(Program program, List<ApplicationStatus> statuses);

    List<Application> findAllByProgramAndStatusOrderByAppliedAtAsc(Program program, ApplicationStatus status);

    /** 홈 "누적 참여자" — 신청 한 번이라도 한 distinct user 수. status 무관 (학습 단계 단순화). */
    @Query("SELECT COUNT(DISTINCT a.user.id) FROM Application a")
    long countDistinctUsers();

    /** 프로그램 ID 목록으로 상태별 신청자 수 일괄 조회 (N+1 방지). Object[]{programId, count} 리스트 반환. */
    @Query("SELECT a.program.id, COUNT(a) FROM Application a WHERE a.program.id IN :programIds AND a.status IN :statuses GROUP BY a.program.id")
    List<Object[]> countByProgramIdsAndStatuses(@Param("programIds") List<Long> programIds,
                                                @Param("statuses") List<ApplicationStatus> statuses);
}

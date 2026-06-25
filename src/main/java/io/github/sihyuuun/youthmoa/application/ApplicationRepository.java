package io.github.sihyuuun.youthmoa.application;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByUserAndProgram(User user, Program program);

    boolean existsByUserAndProgramAndStatusIn(User user, Program program, List<ApplicationStatus> statuses);

    Page<Application> findAllByUser(User user, Pageable pageable);

    long countByProgramAndStatusIn(Program program, List<ApplicationStatus> statuses);

    List<Application> findAllByProgramAndStatusOrderByAppliedAtAsc(Program program, ApplicationStatus status);
}

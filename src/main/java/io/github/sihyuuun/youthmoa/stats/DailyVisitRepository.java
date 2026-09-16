package io.github.sihyuuun.youthmoa.stats;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyVisitRepository extends JpaRepository<DailyVisit, Long> {

  Optional<DailyVisit> findByVisitDate(LocalDate visitDate);

  List<DailyVisit> findByVisitDateBetweenOrderByVisitDateAsc(LocalDate from, LocalDate to);
}

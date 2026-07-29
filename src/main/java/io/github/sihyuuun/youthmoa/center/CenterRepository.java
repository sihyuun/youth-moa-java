package io.github.sihyuuun.youthmoa.center;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CenterRepository extends JpaRepository<Center, Long> {

  List<Center> findAllByIsActiveTrue();

  List<Center> findByRegion(String region);

  List<Center> findAllByIsFeaturedTrueOrderByNameAsc();

  List<Center> findAllByOrderByNameAsc();

  List<Center> findAllByIsActiveTrueOrderByRegionAscNameAsc();

  List<Center> findByRegionAndIsActiveTrue(String region);

  /** Program.organization → Center 정확 매칭. 프로그램 상세의 문의처 전화 조회용. */
  Optional<Center> findByName(String name);
}

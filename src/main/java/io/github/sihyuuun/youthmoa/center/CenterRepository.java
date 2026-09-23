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

  /** Center 이름 정확 매칭. 시드 매핑 · 테스트 유틸리티 · 이름 기반 조회 지점에서 사용. */
  Optional<Center> findByName(String name);

  /** A9-a (2026-09-21): 활성 센터 가나다순 — admin 프로그램 폼 select 옵션 데이터 소스. */
  List<Center> findByIsActiveTrueOrderByNameAsc();
}

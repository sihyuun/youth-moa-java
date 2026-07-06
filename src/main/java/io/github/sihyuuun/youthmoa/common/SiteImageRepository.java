package io.github.sihyuuun.youthmoa.common;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteImageRepository extends JpaRepository<SiteImage, Long> {

  Optional<SiteImage> findBySlotAndIsActiveTrue(String slot);

  List<SiteImage> findAllByIsActiveTrueOrderBySortOrderAsc();

  /** F0e-2: 동일 slot 다건 조회 (HERO_BANNER 로테이션용). */
  List<SiteImage> findAllBySlotAndIsActiveTrueOrderBySortOrderAsc(String slot);
}

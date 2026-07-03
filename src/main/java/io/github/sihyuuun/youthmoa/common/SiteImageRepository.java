package io.github.sihyuuun.youthmoa.common;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteImageRepository extends JpaRepository<SiteImage, Long> {

  Optional<SiteImage> findBySlotAndIsActiveTrue(String slot);

  List<SiteImage> findAllByIsActiveTrueOrderBySortOrderAsc();
}

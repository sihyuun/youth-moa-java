package io.github.sihyuuun.youthmoa.common;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SiteImageRepository extends JpaRepository<SiteImage, Long> {

    Optional<SiteImage> findBySlotAndIsActiveTrue(String slot);

    List<SiteImage> findAllByIsActiveTrueOrderBySortOrderAsc();
}
